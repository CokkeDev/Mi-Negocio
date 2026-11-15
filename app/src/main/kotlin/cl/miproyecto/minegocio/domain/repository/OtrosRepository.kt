package cl.miproyecto.minegocio.domain.repository

import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.GananciaMes
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.model.MasVendido
import cl.miproyecto.minegocio.domain.model.ResumenPeriodo
import kotlinx.coroutines.flow.Flow

// Los "menús" (interfaces) del resto de la app.

interface VentaRepository {
    // Guarda la boleta completa descontando el stock en orden FEFO.
    // Si algo falla, NO se guarda nada (transacción). Devuelve el id de la venta.
    suspend fun registrarVenta(lineas: List<LineaVenta>): Long
}

interface MermaRepository {
    // Descuenta las unidades del lote y guarda la pérdida, todo junto (transacción).
    suspend fun registrarMerma(idLote: Long, cantidad: Int, motivo: String, nota: String?)
}

interface GastoRepository {
    suspend fun insertar(gasto: Gasto): Long
    suspend fun eliminar(idGasto: Long)
    fun obtenerTodos(): Flow<List<Gasto>>
}

interface ReporteRepository {
    // Ventas, ganancia, gastos y merma entre dos fechas.
    fun resumen(desde: Long, hasta: Long): Flow<ResumenPeriodo>

    // Los productos más vendidos entre dos fechas.
    fun masVendidos(desde: Long, hasta: Long, limite: Int): Flow<List<MasVendido>>

    // La ganancia neta de los últimos "meses" meses (incluido el actual).
    fun gananciaPorMes(meses: Int): Flow<List<GananciaMes>>

    // Los gastos sumados por categoría entre dos fechas.
    fun gastosPorCategoria(desde: Long, hasta: Long): Flow<List<Pair<CategoriaGasto, Int>>>

    // Unidades vendidas de un producto desde una fecha.
    fun unidadesVendidas(idProducto: Long, desde: Long): Flow<Int>
}
