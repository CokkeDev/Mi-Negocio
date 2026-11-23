package cl.miproyecto.minegocio.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.core.utils.claveMes
import cl.miproyecto.minegocio.data.DIA
import cl.miproyecto.minegocio.data.crearBaseEnMemoria
import cl.miproyecto.minegocio.data.crearLote
import cl.miproyecto.minegocio.data.crearProducto
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.usecase.costoPorUnidad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Tests de reportes (ganancia neta, más vendidos, meses) y de la lista de productos con stock.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReporteYProductoRepositoryTest {

    private lateinit var db: MiNegocioDatabase
    private lateinit var ventas: VentaRepositoryImpl
    private lateinit var merma: MermaRepositoryImpl
    private lateinit var gastos: GastoRepositoryImpl
    private lateinit var reportes: ReporteRepositoryImpl
    private lateinit var productos: ProductoRepositoryImpl
    private val ahora = System.currentTimeMillis()

    @Before
    fun crear() {
        db = crearBaseEnMemoria()
        ventas = VentaRepositoryImpl(db)
        merma = MermaRepositoryImpl(db)
        gastos = GastoRepositoryImpl(db.gastoDao())
        reportes = ReporteRepositoryImpl(db.reporteDao())
        productos = ProductoRepositoryImpl(db.productoDao())
    }

    @After
    fun cerrar() = db.close()

    private suspend fun vender(idProducto: Long, precio: Int, cantidad: Int) =
        ventas.registrarVenta(listOf(LineaVenta(idProducto, "x", precio, cantidad, stock = 0)))

    // ── REPORTES ──

    @Test
    fun ganancia_neta_es_ganancia_de_ventas_menos_gastos_menos_merma() = runTest {
        // Mismo caso que se probó a mano en el emulador.
        val cola = db.crearProducto("Bebida cola 3L", 2_500)
        val lote = db.crearLote(cola, 1_500, unidades = 12)
        vender(cola, 2_500, 2)                                      // ganancia 2.000
        gastos.insertar(Gasto(descripcion = "Luz", monto = 1_000, categoria = CategoriaGasto.SERVICIOS, fecha = ahora))
        merma.registrarMerma(lote, 1, "Dañado", null)               // pérdida 1.500

        val r = reportes.resumen(ahora - DIA, ahora + DIA).first()

        assertEquals(5_000, r.ventas)
        assertEquals(2_000, r.gananciaVentas)
        assertEquals(1_000, r.gastos)
        assertEquals(1_500, r.merma)
        assertEquals(2_000 - 1_000 - 1_500, r.gananciaNeta)
    }

    @Test
    fun ganancia_de_hoy_es_ventas_menos_costo_de_los_lotes_ingresados() = runTest {
        // Caso del bug: lote de 15 libros que costó $15.000 en total ($1.000 c/u), venta a $2.000.
        val libro = db.crearProducto("Libro", 2_000)
        db.crearLote(libro, costoPorUnidad(15_000, 15), unidades = 15)
        suspend fun hoy() = reportes.resumen(ahora - DIA, ahora + DIA).first()

        assertEquals(15_000, hoy().compras)
        assertEquals(-15_000, hoy().ventasMenosCompras)

        vender(libro, 2_000, 1)
        assertEquals(-13_000, hoy().ventasMenosCompras)

        vender(libro, 2_000, 1)
        assertEquals(-11_000, hoy().ventasMenosCompras)
        // El margen de lo vendido sigue siendo (2.000 - 1.000) x 2.
        assertEquals(2_000, hoy().gananciaVentas)
    }

    @Test
    fun compras_no_cuentan_lotes_ingresados_otro_dia() = runTest {
        val libro = db.crearProducto("Libro", 2_000)
        db.crearLote(libro, 1_000, unidades = 15, ingreso = ahora - 5 * DIA)

        val r = reportes.resumen(ahora - DIA, ahora + DIA).first()

        assertEquals(0, r.compras)
    }

    @Test
    fun resumen_no_cuenta_lo_que_esta_fuera_del_rango_de_fechas() = runTest {
        gastos.insertar(Gasto(descripcion = "Viejo", monto = 9_999, categoria = CategoriaGasto.OTROS, fecha = ahora - 40 * DIA))
        gastos.insertar(Gasto(descripcion = "Hoy", monto = 100, categoria = CategoriaGasto.OTROS, fecha = ahora))

        assertEquals(100, reportes.resumen(ahora - DIA, ahora + DIA).first().gastos)
    }

    @Test
    fun mas_vendidos_ordena_por_unidades() = runTest {
        val a = db.crearProducto("Poco", 1_000)
        val b = db.crearProducto("Mucho", 500)
        db.crearLote(a, 800, 50)
        db.crearLote(b, 300, 50)
        vender(a, 1_000, 1)
        vender(b, 500, 7)

        val lista = reportes.masVendidos(ahora - DIA, ahora + DIA, limite = 5).first()

        assertEquals(listOf("Mucho", "Poco"), lista.map { it.nombre })
        assertEquals(7, lista.first().unidades)
        assertEquals(7 * 200, lista.first().ganancia)
    }

    @Test
    fun ganancia_por_mes_entrega_seis_meses_y_el_actual_al_final() = runTest {
        val p = db.crearProducto("P", 1_000)
        db.crearLote(p, 400, 10)
        vender(p, 1_000, 2)                                          // +1.200 este mes
        gastos.insertar(Gasto(descripcion = "G", monto = 200, categoria = CategoriaGasto.OTROS, fecha = ahora))

        val meses = reportes.gananciaPorMes(6).first()

        assertEquals(6, meses.size)
        assertEquals(claveMes(ahora), claveMes(meses.last().inicioMes))
        assertEquals(1_000, meses.last().gananciaNeta)
        assertTrue(meses.dropLast(1).all { it.gananciaNeta == 0 })
    }

    // ── PRODUCTOS CON STOCK ──

    @Test
    fun producto_con_stock_suma_lotes_y_muestra_el_vencimiento_mas_cercano() = runTest {
        val p = db.crearProducto("Leche", 1_200)
        db.crearLote(p, 800, 4, vence = ahora + 10 * DIA)
        db.crearLote(p, 800, 6, vence = ahora + 3 * DIA)
        db.crearLote(p, 800, 2, vence = null)

        val resumen = productos.obtenerResumen(p).first()

        assertNotNull(resumen)
        assertEquals(12, resumen!!.stock)
        assertEquals(3, resumen.lotesConStock)
        assertEquals(ahora + 3 * DIA, resumen.proximoVencimiento)
    }

    @Test
    fun producto_sin_lotes_tiene_stock_cero() = runTest {
        val p = db.crearProducto("Nuevo", 990)

        val resumen = productos.obtenerConStock().first().single()

        assertEquals(p, resumen.producto.idProducto)
        assertEquals(0, resumen.stock)
        assertEquals(0, resumen.totalLotes)
        assertNull(resumen.proximoVencimiento)
    }

    @Test
    fun eliminar_producto_lo_esconde_y_reactivar_lo_devuelve() = runTest {
        val p = db.crearProducto("Galletas", 700, sku = "123")

        productos.eliminar(p)
        assertTrue(productos.obtenerConStock().first().isEmpty())
        assertEquals(listOf("Galletas"), productos.obtenerEliminados().first().map { it.nombre })
        assertNull(productos.buscarPorSku("123"))       // eliminado: el escáner no lo encuentra

        productos.reactivar(p)
        assertEquals(1, productos.obtenerConStock().first().size)
        assertNotNull(productos.buscarPorSku("123"))
    }

    @Test
    fun busca_duplicados_por_nombre_o_por_codigo() = runTest {
        db.crearProducto("arroz", 1_500, sku = "780111")

        assertNotNull(productos.buscarDuplicado("arroz", null))
        assertNotNull(productos.buscarDuplicado("otro nombre", "780111"))
        assertNull(productos.buscarDuplicado("fideos", "999"))
    }

    @Test
    fun la_base_de_datos_no_permite_dos_productos_con_el_mismo_codigo() {
        runBlocking { db.crearProducto("Uno", 100, sku = "555") }
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { db.crearProducto("Dos", 100, sku = "555") }
        }
    }
}
