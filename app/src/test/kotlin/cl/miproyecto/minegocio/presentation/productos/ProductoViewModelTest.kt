package cl.miproyecto.minegocio.presentation.productos

import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    private class ProductosFalsos(var duplicado: Producto?) : ProductoRepository {
        override suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto? = duplicado
        var insertados = 0
        var fallarSegundo = false
        override suspend fun insertar(producto: Producto): Long {
            insertados++
            // Simula el índice único de la base: el segundo insert del mismo producto falla.
            if (fallarSegundo && insertados > 1) error("UNIQUE constraint failed")
            return 1
        }
        override suspend fun reactivar(idProducto: Long): Int = 1
        override suspend fun eliminar(idProducto: Long): Int = 1
        override fun obtenerActivos(): Flow<List<Producto>> = flowOf(emptyList())
        override fun obtenerEliminados(): Flow<List<Producto>> = flowOf(emptyList())
        override fun obtenerConStock(): Flow<List<ProductoResumen>> = flowOf(emptyList())
        override fun obtenerResumen(idProducto: Long): Flow<ProductoResumen?> = flowOf(null)
        override suspend fun buscarPorSku(sku: String): ProductoResumen? = null
    }

    private class ConfigFalsa : ConfiguracionRepository {
        override val configuracion: StateFlow<Configuracion> = MutableStateFlow(Configuracion())
        override fun actualizar(cambio: (Configuracion) -> Configuracion) {}
    }

    private val leche = Producto(idProducto = 1, sku = "7801", nombre = "Leche entera 1L",
        nombreNormalizado = "leche entera 1l", urlImagen = null, precioVenta = 1200)

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun mismo_codigo_con_otro_nombre_marca_solo_el_codigo() {
        val vm = ProductoViewModel(ProductosFalsos(leche), ConfigFalsa())
        vm.agregarProducto("Otro nombre", "7801", null, "900")
        val estado = vm.uiState.value as AgregarProductoUiState.YaExiste
        assertTrue(estado.porCodigo)
        assertEquals(false, estado.porNombre)
        assertEquals("Leche entera 1L", estado.nombreExistente)
    }

    @Test
    fun mismo_nombre_con_otro_codigo_marca_solo_el_nombre() {
        val vm = ProductoViewModel(ProductosFalsos(leche), ConfigFalsa())
        vm.agregarProducto("LECHE entera 1l", "999", null, "900")
        val estado = vm.uiState.value as AgregarProductoUiState.YaExiste
        assertTrue(estado.porNombre)
        assertEquals(false, estado.porCodigo)
    }

    @Test
    fun mismo_nombre_y_mismo_codigo_marca_ambos() {
        val vm = ProductoViewModel(ProductosFalsos(leche), ConfigFalsa())
        vm.agregarProducto("Leche entera 1L", "7801", null, "900")
        val estado = vm.uiState.value as AgregarProductoUiState.YaExiste
        assertTrue(estado.porNombre && estado.porCodigo)
    }

    @Test
    fun sin_codigo_nunca_marca_el_codigo() {
        val sinSku = leche.copy(sku = null)
        val vm = ProductoViewModel(ProductosFalsos(sinSku), ConfigFalsa())
        vm.agregarProducto("Leche entera 1L", "", null, "900")
        val estado = vm.uiState.value as AgregarProductoUiState.YaExiste
        assertEquals(false, estado.porCodigo)
    }

    @Test
    fun toques_rapidos_en_agregar_no_pisan_el_exito_con_un_error() {
        val repo = ProductosFalsos(null).apply { fallarSegundo = true }
        val vm = ProductoViewModel(repo, ConfigFalsa())
        repeat(3) { vm.agregarProducto("Producto doble", "", null, "500") }
        assertEquals(1, repo.insertados)
        assertTrue(vm.uiState.value is AgregarProductoUiState.ProductoAgregado)
    }
}
