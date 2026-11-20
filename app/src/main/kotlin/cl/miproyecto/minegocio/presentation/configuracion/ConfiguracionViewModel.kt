package cl.miproyecto.minegocio.presentation.configuracion

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.repository.RespaldoRepository
import cl.miproyecto.minegocio.domain.repository.RestauracionInterrumpidaException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfiguracionViewModel(
    private val configuracionRepository: ConfiguracionRepository,
    private val respaldoRepository: RespaldoRepository,
    private val productoRepository: ProductoRepository,
) : ViewModel() {

    val configuracion: StateFlow<Configuracion> = configuracionRepository.configuracion

    val eliminados: StateFlow<List<Producto>> = productoRepository.obtenerEliminados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // true mientras se copia el respaldo.
    private val _trabajando = MutableStateFlow(false)
    val trabajando: StateFlow<Boolean> = _trabajando.asStateFlow()

    // Se pone en true cuando se restauró un respaldo: la pantalla reinicia la app.
    private val _reiniciar = MutableStateFlow(false)
    val reiniciar: StateFlow<Boolean> = _reiniciar.asStateFlow()

    // Cambia una opción. Ej: cambiar { it.copy(stockBajo = 10) }
    fun cambiar(cambio: (Configuracion) -> Configuracion) = configuracionRepository.actualizar(cambio)

    fun cambiarNombre(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isNotEmpty()) cambiar { it.copy(nombreNegocio = limpio.take(40)) }
    }

    fun crearRespaldo(destino: Uri) {
        viewModelScope.launch {
            _trabajando.value = true
            try {
                respaldoRepository.crearRespaldo(destino)
                cambiar { it.copy(ultimoRespaldo = System.currentTimeMillis()) }
                Mensajero.mostrar("Respaldo creado")
            } catch (e: Exception) {
                Mensajero.mostrar("No se pudo crear el respaldo")
            }
            _trabajando.value = false
        }
    }

    fun restaurarRespaldo(origen: Uri) {
        viewModelScope.launch {
            _trabajando.value = true
            try {
                respaldoRepository.restaurarRespaldo(origen)
                _reiniciar.value = true
            } catch (e: RestauracionInterrumpidaException) {
                // La base de datos ya está cerrada: la app no puede seguir sin reiniciarse.
                _reiniciar.value = true
            } catch (e: Exception) {
                Mensajero.mostrar(e.message ?: "No se pudo restaurar el respaldo")
                _trabajando.value = false
            }
        }
    }

    fun reactivar(producto: Producto) {
        viewModelScope.launch {
            productoRepository.reactivar(producto.idProducto)
            Mensajero.mostrar("${producto.nombre} reactivado")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                ConfiguracionViewModel(app.configuracionRepository, app.respaldoRepository, app.productoRepository)
            }
        }
    }
}
