package cl.miproyecto.minegocio.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListaProductosViewModel(
    private val repository: ProductoRepository,
) : ViewModel() {

    // ── BLOQUE A: la lista ──
    val productos: StateFlow<List<Producto>> =
        repository.obtenerActivos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // ── BLOQUE B: eliminar ──
    fun eliminarProducto(idProducto: Long) {
        viewModelScope.launch {
            repository.eliminar(idProducto)
        }
    }

    // ── BLOQUE C: el Factory ──
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Se pide el repositorio a la Application guardada en "instancia".
                // (Navigation 3 no entrega APPLICATION_KEY, por eso no se usa this[...]).
                ListaProductosViewModel(MiNegocioApplication.instancia.productoRepository)
            }
        }
    }
}