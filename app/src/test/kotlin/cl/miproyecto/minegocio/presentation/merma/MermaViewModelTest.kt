package cl.miproyecto.minegocio.presentation.merma

import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import cl.miproyecto.minegocio.domain.repository.MermaRepository
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
class MermaViewModelTest {

    private class MermasFalsas : MermaRepository {
        val registradas = mutableListOf<Int>()
        val espera = CompletableDeferred<Unit>()
        override suspend fun registrarMerma(idLote: Long, cantidad: Int, motivo: String, nota: String?) {
            espera.await()
            registradas += cantidad
        }
    }

    private class LotesFalsos : LoteRepository {
        override suspend fun insertar(lote: Lote): Long = 1
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

    private val lote = Lote(idLote = 1, idProducto = 1, precioCompra = 500, fechaIngreso = 1L,
        fechaVencimiento = null, unidadesIniciales = 10, unidadesDisponibles = 10)

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun toques_rapidos_en_registrar_solo_registran_una_merma() {
        val mermas = MermasFalsas()
        val vm = MermaViewModel(1, ProductosFalsos(), LotesFalsos(), mermas)

        repeat(4) { vm.registrarMerma(lote, 1, "Vencido", "") }
        mermas.espera.complete(Unit)

        assertEquals(listOf(1), mermas.registradas)
        assertTrue(vm.uiState.value is MermaUiState.Registrada)
    }
}
