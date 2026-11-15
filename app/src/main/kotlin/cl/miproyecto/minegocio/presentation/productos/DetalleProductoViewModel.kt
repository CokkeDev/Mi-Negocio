package cl.miproyecto.minegocio.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.utils.inicioDeMes
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleUiState(
    val cargando: Boolean = true,
    // null = el producto no existe.
    val resumen: ProductoResumen? = null,
    // Lotes con stock, en orden FEFO (el primero es el próximo en venderse).
    val lotes: List<Lote> = emptyList(),
    val vendidasMes: Int = 0,
    val configuracion: Configuracion = Configuracion(),
) {
    // Ganancia por unidad usando el lote que se venderá primero.
    val gananciaPorUnidad: Int?
        get() {
            val precio = resumen?.producto?.precioVenta ?: return null
            val lote = lotes.firstOrNull() ?: return null
            return precio - lote.precioCompra
        }
}

class DetalleProductoViewModel(
    private val idProducto: Long,
    private val productoRepository: ProductoRepository,
    loteRepository: LoteRepository,
    reporteRepository: ReporteRepository,
    configuracionRepository: ConfiguracionRepository,
) : ViewModel() {

    val uiState: StateFlow<DetalleUiState> = combine(
        productoRepository.obtenerResumen(idProducto),
        loteRepository.obtenerPorProducto(idProducto),
        reporteRepository.unidadesVendidas(idProducto, inicioDeMes()),
        configuracionRepository.configuracion,
    ) { resumen, lotes, vendidas, config ->
        DetalleUiState(false, resumen, lotes, vendidas, config)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    // Eliminación lógica (activo = 0): las ventas se conservan en los reportes.
    fun eliminar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            try {
                productoRepository.eliminar(idProducto)
                Mensajero.mostrar("Producto eliminado")
                alTerminar()
            } catch (e: Exception) {
                Mensajero.mostrar("No se pudo eliminar el producto")
            }
        }
    }

    companion object {
        // Este Factory es una FUNCIÓN porque necesita saber qué producto mostrar.
        fun factory(idProducto: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                DetalleProductoViewModel(
                    idProducto,
                    app.productoRepository,
                    app.loteRepository,
                    app.reporteRepository,
                    app.configuracionRepository,
                )
            }
        }
    }
}
