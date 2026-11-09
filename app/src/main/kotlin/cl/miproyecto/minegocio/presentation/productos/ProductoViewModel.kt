package cl.miproyecto.minegocio.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.miproyecto.minegocio.core.utils.normalizarNombre
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication

class ProductoViewModel(
    private val repository: ProductoRepository,
) : ViewModel() {

    // ── BLOQUE A: el estado ──
    private val _uiState =
        MutableStateFlow<AgregarProductoUiState>(AgregarProductoUiState.Inicial)
    val uiState: StateFlow<AgregarProductoUiState> = _uiState.asStateFlow()

    // ── BLOQUE B: agregar producto ──
    fun agregarProducto(nombre: String, sku: String, urlImagen: String?, precioTexto: String) {
        // B1. Validar datos
        val nombreNormalizado = normalizarNombre(nombre)
        if (nombreNormalizado.isBlank()) {
            _uiState.value = AgregarProductoUiState.Error("Ingresa el nombre del producto")
            return
        }
        val precio = precioTexto.trim().toIntOrNull()
        if (precio == null || precio <= 0) {
            _uiState.value = AgregarProductoUiState.Error("Ingresa un precio válido mayor a 0")
            return
        }
        val skuLimpio = sku.trim().ifBlank { null }

        // B2. Ir a la base de datos
        viewModelScope.launch {
            _uiState.value = AgregarProductoUiState.Guardando
            try {
                val existente = repository.buscarDuplicado(nombreNormalizado, skuLimpio)

                // B3. Decidir según tu diagrama
                _uiState.value = when {
                    existente == null -> {
                        repository.insertar(
                            Producto(
                                sku = skuLimpio,
                                nombre = nombre.trim(),
                                nombreNormalizado = nombreNormalizado,
                                urlImagen = urlImagen,
                                precioVenta = precio,
                            )
                        )
                        AgregarProductoUiState.ProductoAgregado
                    }

                    existente.activo -> AgregarProductoUiState.YaExiste
                    else -> AgregarProductoUiState.OfrecerReactivar(existente)
                }
            } catch (e: Exception) {
                _uiState.value = AgregarProductoUiState.Error("No se pudo guardar el producto")
            }
        }
    }

    // ── BLOQUE C: reactivar ──
    fun reactivarProducto(idProducto: Long) {
        viewModelScope.launch {
            _uiState.value = AgregarProductoUiState.Guardando
            try {
                val filas = repository.reactivar(idProducto)
                _uiState.value = if (filas > 0) {
                    AgregarProductoUiState.ProductoReactivado
                } else {
                    AgregarProductoUiState.Error("No se encontró el producto")
                }
            } catch (e: Exception) {
                _uiState.value = AgregarProductoUiState.Error("No se pudo reactivar el producto")
            }
        }
    }

    // ── BLOQUE D: cancelar ──
    fun cancelarReactivacion() {
        _uiState.value = AgregarProductoUiState.Inicial
    }

    // --- Bloque E: el Factory ---
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as MiNegocioApplication
                ProductoViewModel(app.productoRepository)
            }
        }
    }
}