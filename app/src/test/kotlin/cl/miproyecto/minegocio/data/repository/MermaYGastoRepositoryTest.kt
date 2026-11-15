package cl.miproyecto.minegocio.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.data.DIA
import cl.miproyecto.minegocio.data.crearBaseEnMemoria
import cl.miproyecto.minegocio.data.crearLote
import cl.miproyecto.minegocio.data.crearProducto
import cl.miproyecto.minegocio.data.disponibles
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.usecase.StockInsuficienteException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Tests de merma (pérdidas) y de gastos.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MermaYGastoRepositoryTest {

    private lateinit var db: MiNegocioDatabase
    private lateinit var merma: MermaRepositoryImpl
    private lateinit var gastos: GastoRepositoryImpl
    private lateinit var reportes: ReporteRepositoryImpl
    private val ahora = System.currentTimeMillis()

    @Before
    fun crear() {
        db = crearBaseEnMemoria()
        merma = MermaRepositoryImpl(db)
        gastos = GastoRepositoryImpl(db.gastoDao())
        reportes = ReporteRepositoryImpl(db.reporteDao())
    }

    @After
    fun cerrar() = db.close()

    // ── MERMA ──

    @Test
    fun merma_descuenta_el_lote_y_cuenta_el_costo_de_compra() = runTest {
        val yogur = db.crearProducto("Yogur", 600)
        val lote = db.crearLote(yogur, 350, unidades = 8)

        merma.registrarMerma(lote, cantidad = 3, motivo = "Vencido", nota = null)

        assertEquals(5, db.disponibles(yogur, lote))
        // La pérdida se valora al precio de COMPRA: 3 x 350.
        val r = reportes.resumen(ahora - DIA, ahora + DIA).first()
        assertEquals(1_050, r.merma)
    }

    @Test
    fun merma_mayor_que_lo_disponible_no_cambia_nada() = runTest {
        val queso = db.crearProducto("Queso", 3_000)
        val lote = db.crearLote(queso, 2_000, unidades = 2)

        assertThrows(StockInsuficienteException::class.java) {
            runBlocking { merma.registrarMerma(lote, cantidad = 5, motivo = "Dañado", nota = "cayó") }
        }

        assertEquals(2, db.disponibles(queso, lote))
        assertEquals(0, reportes.resumen(ahora - DIA, ahora + DIA).first().merma)
    }

    // ── GASTOS ──

    private fun gasto(descripcion: String, monto: Int, categoria: CategoriaGasto, fecha: Long = ahora) =
        Gasto(descripcion = descripcion, monto = monto, categoria = categoria, fecha = fecha)

    @Test
    fun gastos_se_listan_del_mas_nuevo_al_mas_antiguo() = runTest {
        gastos.insertar(gasto("Arriendo", 200_000, CategoriaGasto.ARRIENDO, fecha = ahora - 10 * DIA))
        gastos.insertar(gasto("Luz", 25_000, CategoriaGasto.SERVICIOS, fecha = ahora))
        gastos.insertar(gasto("Micro", 800, CategoriaGasto.TRANSPORTE, fecha = ahora - DIA))

        val lista = gastos.obtenerTodos().first()

        assertEquals(listOf("Luz", "Micro", "Arriendo"), lista.map { it.descripcion })
        assertEquals(CategoriaGasto.SERVICIOS, lista.first().categoria)
    }

    @Test
    fun eliminar_gasto_lo_quita_de_la_lista_y_del_total() = runTest {
        val id = gastos.insertar(gasto("Bolsas", 5_000, CategoriaGasto.INSUMOS))
        gastos.insertar(gasto("Gas", 3_000, CategoriaGasto.SERVICIOS))

        gastos.eliminar(id)

        assertEquals(listOf("Gas"), gastos.obtenerTodos().first().map { it.descripcion })
        assertEquals(3_000, reportes.resumen(ahora - DIA, ahora + DIA).first().gastos)
    }

    @Test
    fun gastos_por_categoria_suman_y_ordenan_de_mayor_a_menor() = runTest {
        gastos.insertar(gasto("Luz", 20_000, CategoriaGasto.SERVICIOS))
        gastos.insertar(gasto("Agua", 10_000, CategoriaGasto.SERVICIOS))
        gastos.insertar(gasto("Micro", 5_000, CategoriaGasto.TRANSPORTE))
        gastos.insertar(gasto("Arriendo", 100_000, CategoriaGasto.ARRIENDO))

        val porCategoria = reportes.gastosPorCategoria(ahora - DIA, ahora + DIA).first()

        assertEquals(
            listOf(
                CategoriaGasto.ARRIENDO to 100_000,
                CategoriaGasto.SERVICIOS to 30_000,
                CategoriaGasto.TRANSPORTE to 5_000,
            ),
            porCategoria,
        )
    }

    @Test
    fun categoria_desconocida_se_lee_como_otros() {
        assertEquals(CategoriaGasto.OTROS, CategoriaGasto.desdeNombre("CATEGORIA_QUE_NO_EXISTE"))
        assertTrue(CategoriaGasto.entries.all { CategoriaGasto.desdeNombre(it.name) == it })
    }
}
