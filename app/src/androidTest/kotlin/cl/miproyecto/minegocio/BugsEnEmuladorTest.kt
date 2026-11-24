package cl.miproyecto.minegocio

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.core.utils.normalizarNombre
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.local.entity.LoteEntity
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import cl.miproyecto.minegocio.data.repository.ReporteRepositoryImpl
import cl.miproyecto.minegocio.data.repository.VentaRepositoryImpl
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.usecase.StockInsuficienteException
import cl.miproyecto.minegocio.domain.usecase.costoPorUnidad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Pruebas que corren DENTRO del emulador con una base de datos real (SQLite de Android) en memoria.
// Los tests con prefijo "BUG_" documentan comportamientos incorrectos: pasan cuando el bug existe.
@RunWith(AndroidJUnit4::class)
class BugsEnEmuladorTest {

    private lateinit var db: MiNegocioDatabase
    private val dia = 24L * 60 * 60 * 1000
    private val ahora = System.currentTimeMillis()

    @Before
    fun crear() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MiNegocioDatabase::class.java).build()
    }

    @After
    fun cerrar() = db.close()

    private suspend fun producto(nombre: String, precio: Int, sku: String? = null) =
        db.productoDao().insertar(
            ProductoEntity(sku = sku, nombre = nombre, nombreNormalizado = normalizarNombre(nombre),
                urlImagen = null, precioVenta = precio),
        )

    private suspend fun lote(idProducto: Long, compra: Int, unidades: Int, vence: Long?) =
        db.loteDao().insertar(
            LoteEntity(idProducto = idProducto, precioCompra = compra, fechaIngreso = ahora,
                fechaVencimiento = vence, unidadesIniciales = unidades, unidadesDisponibles = unidades),
        )

    // ---------- Comportamiento correcto ----------

    @Test
    fun venta_FEFO_descuenta_primero_el_que_vence_antes() = runBlocking {
        val p = producto("Leche", 1200)
        val lejos = lote(p, 800, 5, ahora + 30 * dia)
        val cerca = lote(p, 800, 3, ahora + 2 * dia)
        VentaRepositoryImpl(db).registrarVenta(listOf(LineaVenta(p, "Leche", 1200, 4, 8)))
        val lotes = db.loteDao().obtenerPorProductoUnaVez(p).associateBy { it.idLote }
        assertEquals(null, lotes[cerca])          // agotado
        assertEquals(4, lotes[lejos]!!.unidadesDisponibles)
    }

    @Test
    fun venta_sin_stock_hace_rollback_completo() = runBlocking {
        val a = producto("A", 100); val b = producto("B", 100)
        val la = lote(a, 50, 5, null); lote(b, 50, 1, null)
        try {
            VentaRepositoryImpl(db).registrarVenta(
                listOf(LineaVenta(a, "A", 100, 2, 5), LineaVenta(b, "B", 100, 3, 1)),
            )
            fail("debía fallar")
        } catch (e: StockInsuficienteException) { }
        assertEquals(5, db.loteDao().obtenerPorProductoUnaVez(a).first { it.idLote == la }.unidadesDisponibles)
        assertEquals(0, ReporteRepositoryImpl(db.reporteDao()).resumen(0, Long.MAX_VALUE).first().boletas)
    }

    @Test
    fun sku_y_nombre_duplicados_son_rechazados_por_la_base() = runBlocking {
        producto("Coca-Cola", 1000, sku = "123")
        try { producto("Otra", 1000, sku = "123"); fail("sku repetido debía fallar") } catch (e: Exception) { }
        try { producto("coca cola", 1000); fail("nombre repetido debía fallar") } catch (e: Exception) { }
    }

    // ---------- REGRESIÓN de los bugs corregidos ----------

    // Bug 1: un lote vencido NO cuenta como stock ni se vende.
    @Test
    fun lote_vencido_no_cuenta_como_stock_ni_se_vende() = runBlocking {
        val p = producto("Yogurt", 700)
        val vencido = lote(p, 400, 10, ahora - 5 * dia)
        val bueno = lote(p, 400, 10, ahora + 20 * dia)
        assertEquals(10, db.loteDao().obtenerStock(p).first())
        assertEquals(10, db.productoDao().obtenerConStock().first().single().stock)
        VentaRepositoryImpl(db).registrarVenta(listOf(LineaVenta(p, "Yogurt", 700, 1, 10)))
        val lotes = db.loteDao().obtenerPorProducto(p).first().associateBy { it.idLote }
        assertEquals(10, lotes[vencido]!!.unidadesDisponibles)   // intacto
        assertEquals(9, lotes[bueno]!!.unidadesDisponibles)
    }

    @Test
    fun no_se_puede_vender_mas_que_lo_no_vencido() = runBlocking {
        val p = producto("Queso", 900)
        lote(p, 500, 10, ahora - dia)
        lote(p, 500, 2, ahora + 3 * dia)
        try {
            VentaRepositoryImpl(db).registrarVenta(listOf(LineaVenta(p, "Queso", 900, 3, 3)))
            fail("debía fallar: solo hay 2 unidades vendibles")
        } catch (e: StockInsuficienteException) { }
    }

    @Test
    fun lote_que_vence_hoy_todavia_se_puede_vender() = runBlocking {
        val p = producto("Pan", 100)
        lote(p, 50, 4, cl.miproyecto.minegocio.core.utils.inicioDelDia())
        assertEquals(4, db.loteDao().obtenerStock(p).first())
    }

    @Test
    fun lote_vencido_sigue_disponible_para_registrar_merma() = runBlocking {
        val p = producto("Flan", 300)
        val vencido = lote(p, 100, 5, ahora - 2 * dia)
        assertEquals(listOf(vencido), db.loteDao().obtenerPorProducto(p).first().map { it.idLote })
    }

    // Bug 2: la boleta no puede desbordarse.
    @Test
    fun total_de_boleta_se_calcula_sin_desbordarse() {
        val l = LineaVenta(1, "Caro", 1_000_000, 5_000, stock = 5_000)
        assertTrue(cl.miproyecto.minegocio.domain.usecase.totalSeguro(listOf(l)) >
            cl.miproyecto.minegocio.domain.usecase.BOLETA_MAXIMA)
    }

    // Bug 3: el reporte de compras coincide con lo realmente pagado.
    @Test
    fun compras_coinciden_con_el_costo_real_del_lote() = runBlocking {
        val p = producto("Dulces", 500)
        db.loteDao().insertar(
            LoteEntity(idProducto = p, precioCompra = costoPorUnidad(10_000, 3), fechaIngreso = ahora,
                fechaVencimiento = null, unidadesIniciales = 3, unidadesDisponibles = 3, costoTotal = 10_000),
        )
        val compras = ReporteRepositoryImpl(db.reporteDao()).resumen(ahora - dia, ahora + dia).first().compras
        assertEquals(10_000, compras)
    }

    @Test
    fun lote_antiguo_sin_costo_total_usa_precio_por_unidades() = runBlocking {
        val p = producto("Viejo", 500)
        lote(p, 800, 10, null)   // costoTotal = -1
        val compras = ReporteRepositoryImpl(db.reporteDao()).resumen(ahora - dia, ahora + dia).first().compras
        assertEquals(8_000, compras)
    }

    // Bugs 4 y 5.
    @Test
    fun nombres_con_emoji_o_simbolos_ya_no_quedan_vacios() {
        assertEquals("🍫", normalizarNombre("🍫"))
        assertEquals("???", normalizarNombre("???"))
        assertEquals("日本茶", normalizarNombre("日本茶"))
    }

    @Test
    fun ni_y_n_son_productos_distintos() = runBlocking {
        producto("Piña", 100)
        producto("Pina", 100)   // antes chocaba como duplicado
        assertEquals(2, db.productoDao().obtenerActivos().first().size)
    }
}
