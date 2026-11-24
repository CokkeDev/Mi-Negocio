package cl.miproyecto.minegocio.presentation.lotes

import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class AgregarLoteViewModelTest {

    private class LotesFalsos : LoteRepository {
        val guardados = mutableListOf<Lote>()
        // Mientras no se complete, el guardado queda "en curso" (como una escritura lenta a disco).
        val espera = CompletableDeferred<Unit>()
        override suspend fun insertar(lote: Lote): Long { espera.await(); guardados += lote; return 1 }
        override fun obtenerPorProducto(idProducto: Long): Flow<List<Lote>> = flowOf(emptyList())
        override fun obtenerStock(idProducto: Long): Flow<Int> = flowOf(0)
    }

    private class ProductosFalsos : ProductoRepository {
        override suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto? = null
        override suspend fun insertar(producto: Producto): Long = 0
        override suspend fun reactivar(idProducto: Long): Int = 0
        override suspend fun eliminar(idProducto: Long): Int = 0
        override fun obtenerActivos(): Flow<List<Producto>> = flowOf(emptyList())
        override fun obtenerEliminados(): Flow<List<Producto>> = flowOf(emptyList())
        override fun obtenerConStock(): Flow<List<ProductoResumen>> = flowOf(emptyList())
        override fun obtenerResumen(idProducto: Long): Flow<ProductoResumen?> = flowOf(null)
        override suspend fun buscarPorSku(sku: String): ProductoResumen? = null
    }

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun toques_rapidos_en_guardar_solo_guardan_un_lote() {
        val lotes = LotesFalsos()
        val vm = AgregarLoteViewModel(1, ProductosFalsos(), lotes)

        repeat(3) { vm.guardarLote("10000", 4, null, noVence = true) }
        lotes.espera.complete(Unit)

        assertEquals(1, lotes.guardados.size)
        assertTrue(vm.uiState.value is AgregarLoteUiState.Guardado)
    }

    @Test
    fun un_toque_despues_de_guardar_tampoco_repite() {
        val lotes = LotesFalsos().apply { espera.complete(Unit) }
        val vm = AgregarLoteViewModel(1, ProductosFalsos(), lotes)
        vm.guardarLote("10000", 4, null, noVence = true)
        vm.guardarLote("10000", 4, null, noVence = true)
        assertEquals(1, lotes.guardados.size)
    }
}
