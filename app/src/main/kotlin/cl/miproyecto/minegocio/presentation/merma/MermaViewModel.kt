package cl.miproyecto.minegocio.presentation.merma

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import cl.miproyecto.minegocio.domain.repository.MermaRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.usecase.StockInsuficienteException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MermaUiState {
    data object Inicial : MermaUiState
    data object Guardando : MermaUiState
    data object Registrada : MermaUiState
    data class Error(val mensaje: String, val enCantidad: Boolean = false) : MermaUiState
}

// Sigue el diagrama "Merma" del README.
class MermaViewModel(
    idProducto: Long,
    productoRepository: ProductoRepository,
    loteRepository: LoteRepository,
    private val mermaRepository: MermaRepository,
) : ViewModel() {

    val producto: StateFlow<ProductoResumen?> = productoRepository.obtenerResumen(idProducto)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Solo los lotes que todavía tienen unidades (FEFO).
    val lotes: StateFlow<List<Lote>> = loteRepository.obtenerPorProducto(idProducto)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<MermaUiState>(MermaUiState.Inicial)
    val uiState: StateFlow<MermaUiState> = _uiState.asStateFlow()

    fun registrarMerma(lote: Lote?, cantidad: Int, motivo: String, nota: String) {
        // Validar: hay lote, cantidad > 0 y no más de las disponibles.
        if (lote == null) {
            _uiState.value = MermaUiState.Error("Elige de qué lote se perdieron las unidades")
            return
        }
        if (cantidad <= 0) {
            _uiState.value = MermaUiState.Error("La cantidad debe ser mayor a 0", enCantidad = true)
            return
        }
        if (cantidad > lote.unidadesDisponibles) {
            _uiState.value = MermaUiState.Error("Solo hay ${lote.unidadesDisponibles} unidades en este lote", enCantidad = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = MermaUiState.Guardando
            _uiState.value = try {
                mermaRepository.registrarMerma(lote.idLote, cantidad, motivo, nota.trim().ifBlank { null })
                MermaUiState.Registrada
            } catch (e: StockInsuficienteException) {
                MermaUiState.Error("Ya no quedan tantas unidades en este lote", enCantidad = true)
            } catch (e: Exception) {
                MermaUiState.Error("No se pudo registrar la merma")
            }
        }
    }

    fun limpiarError() {
        if (_uiState.value is MermaUiState.Error) _uiState.value = MermaUiState.Inicial
    }

    companion object {
        fun factory(idProducto: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                MermaViewModel(idProducto, app.productoRepository, app.loteRepository, app.mermaRepository)
            }
        }
    }
}
