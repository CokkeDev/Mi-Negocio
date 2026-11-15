package cl.miproyecto.minegocio.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.data.DIA
import cl.miproyecto.minegocio.data.crearBaseEnMemoria
import cl.miproyecto.minegocio.data.crearLote
import cl.miproyecto.minegocio.data.crearProducto
import cl.miproyecto.minegocio.data.disponibles
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.usecase.StockInsuficienteException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Tests de la venta: el corazón de la app.
// Revisan que se descuente el stock en orden FEFO y que, si algo falla, no quede nada a medias.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class VentaRepositoryImplTest {

    private lateinit var db: MiNegocioDatabase
    private lateinit var ventas: VentaRepositoryImpl
    private lateinit var reportes: ReporteRepositoryImpl
    private val ahora = System.currentTimeMillis()

    @Before
    fun crear() {
        db = crearBaseEnMemoria()
        ventas = VentaRepositoryImpl(db)
        reportes = ReporteRepositoryImpl(db.reporteDao())
    }

    @After
    fun cerrar() = db.close()

    private fun linea(idProducto: Long, precio: Int, cantidad: Int) =
        LineaVenta(idProducto, "x", precio, cantidad, stock = 0)

    // Todo el día de hoy (y un poco más) para leer los reportes.
    private suspend fun resumenDeHoy() = reportes.resumen(ahora - DIA, ahora + DIA).first()

    @Test
    fun venta_descuenta_primero_el_lote_que_vence_antes() = runTest {
        val leche = db.crearProducto("Leche", 1_200)
        val noVence = db.crearLote(leche, 800, unidades = 10, vence = null)
        val venceLuego = db.crearLote(leche, 800, unidades = 5, vence = ahora + 30 * DIA)
        val venceProntito = db.crearLote(leche, 800, unidades = 3, vence = ahora + 2 * DIA)

        ventas.registrarVenta(listOf(linea(leche, 1_200, cantidad = 5)))

        // Primero se acaba el que vence en 2 días (3 u.), luego 2 del que vence en 30.
        assertEquals(0, db.disponibles(leche, venceProntito))
        assertEquals(3, db.disponibles(leche, venceLuego))
        assertEquals(10, db.disponibles(leche, noVence))
    }

    @Test
    fun venta_guarda_total_boleta_y_ganancia_con_el_costo_de_cada_lote() = runTest {
        val pan = db.crearProducto("Pan", 1_000)
        db.crearLote(pan, 600, unidades = 2, vence = ahora + DIA)   // más barato, sale primero
        db.crearLote(pan, 700, unidades = 5, vence = ahora + 5 * DIA)

        ventas.registrarVenta(listOf(linea(pan, 1_000, cantidad = 3)))

        val r = resumenDeHoy()
        assertEquals(3_000, r.ventas)
        assertEquals(1, r.boletas)
        // 2 x (1000 - 600) + 1 x (1000 - 700) = 800 + 300
        assertEquals(1_100, r.gananciaVentas)
        assertEquals(3, reportes.unidadesVendidas(pan, ahora - DIA).first())
    }

    @Test
    fun venta_con_varios_productos_suma_todo_en_una_sola_boleta() = runTest {
        val a = db.crearProducto("A", 500)
        val b = db.crearProducto("B", 2_000)
        db.crearLote(a, 300, unidades = 10)
        db.crearLote(b, 1_500, unidades = 10)

        ventas.registrarVenta(listOf(linea(a, 500, 4), linea(b, 2_000, 1)))

        val r = resumenDeHoy()
        assertEquals(4_000, r.ventas)
        assertEquals(1, r.boletas)
        assertEquals(4 * 200 + 500, r.gananciaVentas)
    }

    @Test
    fun sin_stock_suficiente_lanza_error_y_no_guarda_nada() = runTest {
        val jugo = db.crearProducto("Jugo", 900)
        val lote = db.crearLote(jugo, 500, unidades = 2)

        assertThrows(StockInsuficienteException::class.java) {
            kotlinx.coroutines.runBlocking { ventas.registrarVenta(listOf(linea(jugo, 900, 3))) }
        }

        assertEquals(2, db.disponibles(jugo, lote))
        assertEquals(0, resumenDeHoy().boletas)
    }

    @Test
    fun si_falla_el_segundo_producto_se_deshace_el_descuento_del_primero() = runTest {
        val bueno = db.crearProducto("Bueno", 1_000)
        val escaso = db.crearProducto("Escaso", 1_000)
        val loteBueno = db.crearLote(bueno, 500, unidades = 10)
        db.crearLote(escaso, 500, unidades = 1)

        assertThrows(StockInsuficienteException::class.java) {
            kotlinx.coroutines.runBlocking {
                ventas.registrarVenta(listOf(linea(bueno, 1_000, 4), linea(escaso, 1_000, 2)))
            }
        }

        // Transacción: el primer producto vuelve a tener sus 10 unidades.
        assertEquals(10, db.disponibles(bueno, loteBueno))
        assertEquals(0, resumenDeHoy().ventas)
    }

    @Test
    fun boleta_vacia_no_se_puede_registrar() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { ventas.registrarVenta(emptyList()) }
        }
    }
}
