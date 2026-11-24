package cl.miproyecto.minegocio.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.core.utils.normalizarNombre
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.usecase.PRECIO_VENTA_MAXIMO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel de "Agregar producto". Sigue el diagrama "Registro de producto" del README.
class ProductoViewModel(
    private val repository: ProductoRepository,
    configuracionRepository: ConfiguracionRepository,
) : ViewModel() {

    // Ajustes de sonido y vibración al escanear (igual que en VentaViewModel).
    val configuracion: StateFlow<Configuracion> = configuracionRepository.configuracion

    private val _uiState =
        MutableStateFlow<AgregarProductoUiState>(AgregarProductoUiState.Inicial)
    val uiState: StateFlow<AgregarProductoUiState> = _uiState.asStateFlow()

    fun agregarProducto(nombre: String, sku: String, urlImagen: String?, precioTexto: String) {
        // Con toques rápidos la segunda llamada chocaba con el índice único y pisaba el éxito
        // con "No se pudo guardar el producto" aunque ya estuviera guardado.
        val actual = _uiState.value
        if (actual is AgregarProductoUiState.Guardando || actual is AgregarProductoUiState.ProductoAgregado) return
        // 1) Validar datos
        val nombreNormalizado = normalizarNombre(nombre)
        if (nombreNormalizado.isBlank()) {
            _uiState.value = AgregarProductoUiState.Error("Ingresa el nombre del producto", CampoProducto.NOMBRE)
            return
        }
        val precio = precioTexto.trim().toIntOrNull()
        if (precio == null || precio <= 0) {
            _uiState.value = AgregarProductoUiState.Error("Ingresa un precio válido mayor a 0", CampoProducto.PRECIO)
            return
        }
        if (precio > PRECIO_VENTA_MAXIMO) {
            _uiState.value = AgregarProductoUiState.Error(
                "El precio no puede superar los \$1.000.000", CampoProducto.PRECIO,
            )
            return
        }
        val skuLimpio = sku.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.value = AgregarProductoUiState.Guardando
            try {
                // 2) Buscar duplicado
                val existente = repository.buscarDuplicado(nombreNormalizado, skuLimpio)

                _uiState.value = when {
                    // 3a) No existe: se guarda
                    existente == null -> {
                        val id = repository.insertar(
                            Producto(
                                sku = skuLimpio,
                                nombre = nombre.trim(),
                                nombreNormalizado = nombreNormalizado,
                                urlImagen = urlImagen,
                                precioVenta = precio,
                            ),
                        )
                        AgregarProductoUiState.ProductoAgregado(id)
                    }
                    // 3b) Existe y está activo: error
                    existente.activo -> AgregarProductoUiState.YaExiste(
                        porNombre = existente.nombreNormalizado == nombreNormalizado,
                        porCodigo = skuLimpio != null && existente.sku == skuLimpio,
                        nombreExistente = existente.nombre,
                    )
                    // 3c) Existe pero está eliminado: ofrecer reactivar
                    else -> AgregarProductoUiState.OfrecerReactivar(existente)
                }
            } catch (e: Exception) {
                _uiState.value = AgregarProductoUiState.Error("No se pudo guardar el producto")
            }
        }
    }

    fun reactivarProducto(idProducto: Long) {
        viewModelScope.launch {
            _uiState.value = AgregarProductoUiState.Guardando
            try {
                val filas = repository.reactivar(idProducto)
                _uiState.value = if (filas > 0) {
                    AgregarProductoUiState.ProductoReactivado(idProducto)
                } else {
                    AgregarProductoUiState.Error("No se encontró el producto")
                }
            } catch (e: Exception) {
                _uiState.value = AgregarProductoUiState.Error("No se pudo reactivar el producto")
            }
        }
    }

    fun cancelarReactivacion() {
        _uiState.value = AgregarProductoUiState.Inicial
    }

    // Al escribir de nuevo en el formulario se borra el error anterior.
    fun limpiarError() {
        val actual = _uiState.value
        if (actual is AgregarProductoUiState.Error || actual is AgregarProductoUiState.YaExiste) {
            _uiState.value = AgregarProductoUiState.Inicial
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                ProductoViewModel(app.productoRepository, app.configuracionRepository)
            }
        }
    }
}
