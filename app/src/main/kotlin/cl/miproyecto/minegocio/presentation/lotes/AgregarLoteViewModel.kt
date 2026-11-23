package cl.miproyecto.minegocio.presentation.lotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.usecase.costoPorUnidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CampoLote { PRECIO, UNIDADES, VENCIMIENTO, GENERAL }

sealed interface AgregarLoteUiState {
    data object Inicial : AgregarLoteUiState
    data object Guardando : AgregarLoteUiState
    data object Guardado : AgregarLoteUiState
    data class Error(val mensaje: String, val campo: CampoLote = CampoLote.GENERAL) : AgregarLoteUiState
}

// Sigue el diagrama "Ingreso de lotes" del README.
class AgregarLoteViewModel(
    private val idProducto: Long,
    productoRepository: ProductoRepository,
    private val loteRepository: LoteRepository,
) : ViewModel() {

    // El producto al que se le agrega el lote (para mostrar su nombre, precio y stock).
    val producto: StateFlow<ProductoResumen?> = productoRepository.obtenerResumen(idProducto)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow<AgregarLoteUiState>(AgregarLoteUiState.Inicial)
    val uiState: StateFlow<AgregarLoteUiState> = _uiState.asStateFlow()

    // costoTotalTexto: lo que se pagó por el lote COMPLETO (no por unidad).
    fun guardarLote(costoTotalTexto: String, unidades: Int, fechaVencimiento: Long?, noVence: Boolean) {
        // 1) Validar: costo >= 0, unidades > 0, vencimiento opcional (pero hay que decidir).
        val costoTotal = costoTotalTexto.trim().toIntOrNull()
        if (costoTotal == null || costoTotal < 0) {
            _uiState.value = AgregarLoteUiState.Error("Ingresa el costo total del lote (puede ser 0)", CampoLote.PRECIO)
            return
        }
        if (unidades <= 0) {
            _uiState.value = AgregarLoteUiState.Error("Las unidades deben ser mayores a 0", CampoLote.UNIDADES)
            return
        }
        if (!noVence && fechaVencimiento == null) {
            _uiState.value = AgregarLoteUiState.Error(
                "Elige la fecha de vencimiento o marca que no vence",
                CampoLote.VENCIMIENTO,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AgregarLoteUiState.Guardando
            try {
                loteRepository.insertar(
                    Lote(
                        idProducto = idProducto,
                        // La base guarda el costo de CADA unidad: 15.000 / 15 = 1.000.
                        precioCompra = costoPorUnidad(costoTotal, unidades),
                        // 2) fechaIngreso = hoy
                        fechaIngreso = System.currentTimeMillis(),
                        fechaVencimiento = if (noVence) null else fechaVencimiento,
                        unidadesIniciales = unidades,
                        // 3) Al comprar, quedan disponibles todas las unidades.
                        unidadesDisponibles = unidades,
                    ),
                )
                _uiState.value = AgregarLoteUiState.Guardado
            } catch (e: Exception) {
                _uiState.value = AgregarLoteUiState.Error("No se pudo guardar el lote")
            }
        }
    }

    fun limpiarError() {
        if (_uiState.value is AgregarLoteUiState.Error) _uiState.value = AgregarLoteUiState.Inicial
    }

    companion object {
        fun factory(idProducto: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                AgregarLoteViewModel(idProducto, app.productoRepository, app.loteRepository)
            }
        }
    }
}
