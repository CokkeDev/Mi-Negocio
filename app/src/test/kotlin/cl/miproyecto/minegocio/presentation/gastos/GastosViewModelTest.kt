package cl.miproyecto.minegocio.presentation.gastos

import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.GananciaMes
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.model.MasVendido
import cl.miproyecto.minegocio.domain.model.ResumenPeriodo
import cl.miproyecto.minegocio.domain.repository.GastoRepository
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {

    private class GastosFalsos : GastoRepository {
        val guardados = mutableListOf<Gasto>()
        override suspend fun insertar(gasto: Gasto): Long { guardados += gasto; return 1 }
        override suspend fun eliminar(idGasto: Long) {}
        override fun obtenerTodos(): Flow<List<Gasto>> = flowOf(emptyList())
    }

    private class ReportesFalsos : ReporteRepository {
        override fun resumen(desde: Long, hasta: Long): Flow<ResumenPeriodo> = flowOf(ResumenPeriodo())
        override fun masVendidos(desde: Long, hasta: Long, limite: Int): Flow<List<MasVendido>> = flowOf(emptyList())
        override fun gananciaPorMes(meses: Int): Flow<List<GananciaMes>> = flowOf(emptyList())
        override fun gastosPorCategoria(desde: Long, hasta: Long): Flow<List<Pair<CategoriaGasto, Int>>> =
            flowOf(emptyList())
        override fun unidadesVendidas(idProducto: Long, desde: Long): Flow<Int> = flowOf(0)
    }

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun rechaza_un_gasto_sobre_el_tope() {
        val gastos = GastosFalsos()
        val vm = GastosViewModel(gastos, ReportesFalsos())
        val error = vm.agregarGasto("Arriendo", "999999999", CategoriaGasto.ARRIENDO, 1L)
        assertNotNull(error)
        assertEquals(0, gastos.guardados.size)
    }

    @Test
    fun acepta_un_gasto_en_el_tope() {
        val gastos = GastosFalsos()
        val vm = GastosViewModel(gastos, ReportesFalsos())
        assertNull(vm.agregarGasto("Arriendo", "10000000", CategoriaGasto.ARRIENDO, 1L))
        assertEquals(1, gastos.guardados.size)
    }

    @Test
    fun el_porcentaje_de_ventas_no_se_desborda_con_totales_grandes() {
        // 2.000.000.000 x 100 desbordaba el Int y daba un porcentaje negativo.
        val estado = GastosUiState(totalMes = 2_000_000_000, ventasMes = 1_000_000_000)
        assertEquals(200, estado.porcentajeDeVentas)
    }
}
