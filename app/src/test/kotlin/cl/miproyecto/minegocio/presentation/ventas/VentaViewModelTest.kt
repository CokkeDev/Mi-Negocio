package cl.miproyecto.minegocio.presentation.ventas

import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.repository.VentaRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests de la boleta: precio actualizado y boleta bloqueada mientras se confirma.
@OptIn(ExperimentalCoroutinesApi::class)
class VentaViewModelTest {

    private class ProductosFalsos(inicial: List<ProductoResumen>) : ProductoRepository {
        val lista = MutableStateFlow(inicial)
        override fun obtenerConStock(): Flow<List<ProductoResumen>> = lista
        override suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto? = null
        override suspend fun insertar(producto: Producto): Long = 0
        override suspend fun reactivar(idProducto: Long): Int = 0
        override suspend fun eliminar(idProducto: Long): Int = 0
        override fun obtenerActivos(): Flow<List<Producto>> = throw NotImplementedError()
        override fun obtenerEliminados(): Flow<List<Producto>> = throw NotImplementedError()
        override fun obtenerResumen(idProducto: Long): Flow<ProductoResumen?> = throw NotImplementedError()
        override suspend fun buscarPorSku(sku: String): ProductoResumen? = null
    }

    private class VentasFalsas : VentaRepository {
        val guardadas = mutableListOf<List<LineaVenta>>()
        var espera: CompletableDeferred<Unit>? = null
        override suspend fun registrarVenta(lineas: List<LineaVenta>): Long {
            espera?.await()
            guardadas += lineas
            return 1
        }
    }

    private class ConfigFalsa : ConfiguracionRepository {
        override val configuracion: StateFlow<Configuracion> = MutableStateFlow(Configuracion())
        override fun actualizar(cambio: (Configuracion) -> Configuracion) {}
    }

    private fun resumen(id: Long, precio: Int, stock: Int) = ProductoResumen(
        producto = Producto(idProducto = id, sku = null, nombre = "P$id", nombreNormalizado = "p$id",
            urlImagen = null, precioVenta = precio),
        stock = stock, lotesConStock = 1, totalLotes = 1, proximoVencimiento = null,
    )

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun si_cambia_el_precio_la_boleta_cobra_el_precio_nuevo() {
        val productos = ProductosFalsos(listOf(resumen(1, 1000, 10)))
        val vm = VentaViewModel(productos, VentasFalsas(), ConfigFalsa())
        vm.agregarProducto(productos.lista.value[0])
        assertEquals(1000, vm.uiState.value.total)

        productos.lista.value = listOf(resumen(1, 1500, 10))

        assertEquals(1500, vm.uiState.value.total)
    }

    @Test
    fun si_baja_el_stock_la_cantidad_se_ajusta_y_si_es_cero_se_quita_la_linea() {
        val productos = ProductosFalsos(listOf(resumen(1, 1000, 10)))
        val vm = VentaViewModel(productos, VentasFalsas(), ConfigFalsa())
        repeat(3) { vm.agregarProducto(productos.lista.value[0]) }
        assertEquals(3, vm.uiState.value.unidades)

        productos.lista.value = listOf(resumen(1, 1000, 2))
        assertEquals(2, vm.uiState.value.unidades)

        productos.lista.value = listOf(resumen(1, 1000, 0))
        assertTrue(vm.uiState.value.lineas.isEmpty())
    }

    @Test
    fun mientras_se_confirma_la_venta_la_boleta_no_se_puede_modificar() {
        val productos = ProductosFalsos(listOf(resumen(1, 1000, 10)))
        val ventas = VentasFalsas().apply { espera = CompletableDeferred() }
        val vm = VentaViewModel(productos, ventas, ConfigFalsa())
        vm.agregarProducto(productos.lista.value[0])

        vm.confirmarVenta()
        assertTrue(vm.uiState.value.registrando)

        vm.sumar(1)
        vm.restar(1)
        vm.agregarProducto(productos.lista.value[0])
        productos.lista.value = listOf(resumen(1, 2000, 10))
        assertEquals(1, vm.uiState.value.unidades)
        assertEquals(1000, vm.uiState.value.total)

        ventas.espera!!.complete(Unit)
        assertEquals(1, ventas.guardadas.size)
        assertEquals(1000, ventas.guardadas[0].sumOf { it.subtotal })
        assertTrue(vm.uiState.value.lineas.isEmpty())
    }

    @Test
    fun un_segundo_toque_en_confirmar_no_registra_dos_ventas() {
        val productos = ProductosFalsos(listOf(resumen(1, 1000, 10)))
        val ventas = VentasFalsas().apply { espera = CompletableDeferred() }
        val vm = VentaViewModel(productos, ventas, ConfigFalsa())
        vm.agregarProducto(productos.lista.value[0])
        vm.confirmarVenta()
        vm.confirmarVenta()
        ventas.espera!!.complete(Unit)
        assertEquals(1, ventas.guardadas.size)
    }
}
