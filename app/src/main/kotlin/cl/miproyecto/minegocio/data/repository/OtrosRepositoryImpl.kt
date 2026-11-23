package cl.miproyecto.minegocio.data.repository

import androidx.room.withTransaction
import cl.miproyecto.minegocio.core.utils.claveMes
import cl.miproyecto.minegocio.core.utils.inicioDeMes
import cl.miproyecto.minegocio.data.local.dao.GastoDao
import cl.miproyecto.minegocio.data.local.dao.ReporteDao
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.local.entity.DetalleVentaEntity
import cl.miproyecto.minegocio.data.local.entity.PerdidaEntity
import cl.miproyecto.minegocio.data.local.entity.VentaEntity
import cl.miproyecto.minegocio.data.mapper.toDomain
import cl.miproyecto.minegocio.data.mapper.toEntity
import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.GananciaMes
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.model.MasVendido
import cl.miproyecto.minegocio.domain.model.ResumenPeriodo
import cl.miproyecto.minegocio.domain.repository.GastoRepository
import cl.miproyecto.minegocio.domain.repository.MermaRepository
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import cl.miproyecto.minegocio.domain.repository.VentaRepository
import cl.miproyecto.minegocio.domain.usecase.StockInsuficienteException
import cl.miproyecto.minegocio.domain.usecase.repartirFefo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// ── VENTAS ──
// Recibe la base de datos completa (no solo un DAO) porque necesita una TRANSACCIÓN:
// varias operaciones que se hacen "todas o ninguna".
class VentaRepositoryImpl(
    private val database: MiNegocioDatabase,
) : VentaRepository {

    override suspend fun registrarVenta(lineas: List<LineaVenta>): Long {
        require(lineas.isNotEmpty()) { "La boleta está vacía" }
        val loteDao = database.loteDao()
        val ventaDao = database.ventaDao()

        // withTransaction = BEGIN ... COMMIT.
        // Si adentro se lanza un error, Room hace ROLLBACK: nada queda guardado.
        return database.withTransaction {
            val idVenta = ventaDao.insertarVenta(
                VentaEntity(
                    fecha = System.currentTimeMillis(),
                    total = lineas.sumOf { it.subtotal },
                ),
            )
            for (linea in lineas) {
                // Lotes del producto con stock, en orden FEFO.
                val lotes = loteDao.obtenerPorProductoUnaVez(linea.idProducto).map { it.toDomain() }
                // De qué lote sale cada unidad (lanza error si no alcanza).
                for ((lote, cantidad) in repartirFefo(lotes, linea.cantidad)) {
                    val filas = loteDao.descontar(lote.idLote, cantidad)
                    if (filas == 0) throw StockInsuficienteException()
                    ventaDao.insertarDetalle(
                        DetalleVentaEntity(
                            idVenta = idVenta,
                            idLote = lote.idLote,
                            cantidad = cantidad,
                            precioUnitario = linea.precioUnitario,
                        ),
                    )
                }
            }
            idVenta
        }
    }
}

// ── MERMA ──
class MermaRepositoryImpl(
    private val database: MiNegocioDatabase,
) : MermaRepository {

    override suspend fun registrarMerma(idLote: Long, cantidad: Int, motivo: String, nota: String?) {
        database.withTransaction {
            // Primero se descuenta. Si no hay unidades suficientes, devuelve 0 y se cancela todo.
            val filas = database.loteDao().descontar(idLote, cantidad)
            if (filas == 0) throw StockInsuficienteException()
            database.perdidaDao().insertar(
                PerdidaEntity(
                    idLote = idLote,
                    cantidad = cantidad,
                    motivo = motivo,
                    nota = nota,
                    fecha = System.currentTimeMillis(),
                ),
            )
        }
    }
}

// ── GASTOS ──
class GastoRepositoryImpl(
    private val dao: GastoDao,
) : GastoRepository {

    override suspend fun insertar(gasto: Gasto): Long = dao.insertar(gasto.toEntity())

    override suspend fun eliminar(idGasto: Long) {
        dao.eliminar(idGasto)
    }

    override fun obtenerTodos(): Flow<List<Gasto>> =
        dao.obtenerTodos().map { lista -> lista.map { it.toDomain() } }
}

// ── REPORTES ──
class ReporteRepositoryImpl(
    private val dao: ReporteDao,
) : ReporteRepository {

    // "combine" junta 5 Flows en uno: cada vez que CUALQUIERA cambia,
    // se vuelve a armar el ResumenPeriodo con los últimos valores de los 5.
    override fun resumen(desde: Long, hasta: Long): Flow<ResumenPeriodo> =
        combine(
            dao.totalesVentas(desde, hasta),
            dao.gananciaVentas(desde, hasta),
            dao.totalGastos(desde, hasta),
            dao.costoMerma(desde, hasta),
            dao.totalCompras(desde, hasta),
        ) { totales, ganancia, gastos, merma, compras ->
            ResumenPeriodo(
                ventas = totales.ventas,
                boletas = totales.boletas,
                gananciaVentas = ganancia,
                gastos = gastos,
                merma = merma,
                compras = compras,
            )
        }

    override fun masVendidos(desde: Long, hasta: Long, limite: Int): Flow<List<MasVendido>> =
        dao.masVendidos(desde, hasta, limite).map { filas ->
            filas.map { MasVendido(it.nombre, it.unidades, it.ganancia) }
        }

    override fun gananciaPorMes(meses: Int): Flow<List<GananciaMes>> {
        // Los inicios de cada mes, del más antiguo al actual. Ej (meses = 6): jun, jul, ..., nov.
        val inicios = (meses - 1 downTo 0).map { inicioDeMes(meses = -it) }
        val desde = inicios.first()
        return combine(
            dao.gananciaPorMes(desde),
            dao.gastosPorMes(desde),
            dao.mermaPorMes(desde),
        ) { ganancias, gastos, mermas ->
            // associate: convierte la lista en un "diccionario" mes -> monto.
            val g = ganancias.associate { it.mes to it.monto }
            val gs = gastos.associate { it.mes to it.monto }
            val m = mermas.associate { it.mes to it.monto }
            inicios.map { inicio ->
                val clave = claveMes(inicio)
                // "?: 0": si ese mes no tuvo datos, cuenta 0.
                GananciaMes(inicio, (g[clave] ?: 0) - (gs[clave] ?: 0) - (m[clave] ?: 0))
            }
        }
    }

    override fun gastosPorCategoria(desde: Long, hasta: Long): Flow<List<Pair<CategoriaGasto, Int>>> =
        dao.gastosPorCategoria(desde, hasta).map { filas ->
            filas.map { CategoriaGasto.desdeNombre(it.categoria) to it.monto }
        }

    override fun unidadesVendidas(idProducto: Long, desde: Long): Flow<Int> =
        dao.unidadesVendidas(idProducto, desde)
}
