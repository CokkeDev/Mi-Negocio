package cl.miproyecto.minegocio.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.core.utils.inicioDeAnio
import cl.miproyecto.minegocio.core.utils.inicioDeMes
import cl.miproyecto.minegocio.core.utils.inicioDeSemana
import cl.miproyecto.minegocio.core.utils.inicioDelDiaSiguiente
import cl.miproyecto.minegocio.domain.model.GananciaMes
import cl.miproyecto.minegocio.domain.model.MasVendido
import cl.miproyecto.minegocio.domain.model.ResumenPeriodo
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class Periodo { SEMANA, MES, ANIO }

data class ReportesUiState(
    val periodo: Periodo = Periodo.MES,
    val resumen: ResumenPeriodo = ResumenPeriodo(),
    val masVendidos: List<MasVendido> = emptyList(),
    val meses: List<GananciaMes> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportesViewModel(
    repository: ReporteRepository,
) : ViewModel() {

    private val periodo = MutableStateFlow(Periodo.MES)

    // flatMapLatest: cada vez que cambia el periodo, se "cambia de canal"
    // y se escuchan las consultas del nuevo rango de fechas.
    val uiState: StateFlow<ReportesUiState> = periodo.flatMapLatest { p ->
        val desde = when (p) {
            Periodo.SEMANA -> inicioDeSemana()
            Periodo.MES -> inicioDeMes()
            Periodo.ANIO -> inicioDeAnio()
        }
        val hasta = inicioDelDiaSiguiente()
        combine(
            repository.resumen(desde, hasta),
            repository.masVendidos(desde, hasta, limite = 5),
            repository.gananciaPorMes(meses = 6),
        ) { resumen, top, meses -> ReportesUiState(p, resumen, top, meses) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportesUiState())

    fun elegirPeriodo(nuevo: Periodo) {
        periodo.value = nuevo
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ReportesViewModel(MiNegocioApplication.instancia.reporteRepository) }
        }
    }
}
