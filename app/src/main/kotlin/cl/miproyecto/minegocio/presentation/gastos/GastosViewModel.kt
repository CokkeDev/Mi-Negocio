package cl.miproyecto.minegocio.presentation.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.utils.diaActual
import cl.miproyecto.minegocio.core.utils.inicioDeMes
import cl.miproyecto.minegocio.core.utils.inicioDeSemana
import cl.miproyecto.minegocio.core.utils.inicioDelDiaSiguiente
import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.repository.GastoRepository
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import cl.miproyecto.minegocio.domain.usecase.GASTO_MAXIMO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GastosUiState(
    val totalMes: Int = 0,
    val ventasMes: Int = 0,
    val porCategoria: List<Pair<CategoriaGasto, Int>> = emptyList(),
    val estaSemana: List<Gasto> = emptyList(),
    val antes: List<Gasto> = emptyList(),
) {
    // Qué parte de las ventas del mes se fue en gastos.
    // (Se calcula en Long: totalMes * 100 se desbordaba con totales grandes.)
    val porcentajeDeVentas: Int? get() =
        if (ventasMes > 0) (totalMes.toLong() * 100 / ventasMes).toInt() else null
}

class GastosViewModel(
    private val gastoRepository: GastoRepository,
    reporteRepository: ReporteRepository,
) : ViewModel() {

    // diaActual().flatMapLatest: si la app queda abierta y cambia el día (o el mes),
    // los totales se recalculan con el rango nuevo.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GastosUiState> = diaActual().flatMapLatest { dia ->
        val inicioMes = inicioDeMes(dia)
        val hasta = inicioDeMes(dia, meses = 1)
        combine(
            gastoRepository.obtenerTodos(),
            reporteRepository.gastosPorCategoria(inicioMes, hasta),
            reporteRepository.resumen(inicioMes, inicioDelDiaSiguiente(dia)),
        ) { gastos, categorias, resumenMes ->
            val lunes = inicioDeSemana(dia)
            GastosUiState(
                totalMes = categorias.sumOf { it.second },
                ventasMes = resumenMes.ventas,
                porCategoria = categorias,
                estaSemana = gastos.filter { it.fecha >= lunes },
                antes = gastos.filter { it.fecha < lunes },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GastosUiState())

    // true mientras se guarda un gasto (solo se usa desde el hilo principal).
    private var guardando = false

    // Devuelve un mensaje de error, o null si se guardó bien.
    fun agregarGasto(descripcion: String, montoTexto: String, categoria: CategoriaGasto, fecha: Long): String? {
        if (descripcion.isBlank()) return "Escribe en qué se gastó"
        val monto = montoTexto.toIntOrNull()
        if (monto == null || monto <= 0) return "Ingresa un monto mayor a 0"
        if (monto > GASTO_MAXIMO) return "El monto no puede superar los \$10.000.000"
        // Un solo guardado a la vez: con toques rápidos se registraba el gasto varias veces.
        if (guardando) return null
        guardando = true
        viewModelScope.launch {
            try {
                gastoRepository.insertar(Gasto(descripcion = descripcion.trim(), monto = monto, categoria = categoria, fecha = fecha))
                Mensajero.mostrar("Gasto agregado")
            } catch (e: Exception) {
                Mensajero.mostrar("No se pudo guardar el gasto")
            } finally {
                guardando = false
            }
        }
        return null
    }

    fun eliminarGasto(gasto: Gasto) {
        viewModelScope.launch {
            gastoRepository.eliminar(gasto.idGasto)
            Mensajero.mostrar("Gasto eliminado")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                GastosViewModel(app.gastoRepository, app.reporteRepository)
            }
        }
    }
}
