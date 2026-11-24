package cl.miproyecto.minegocio.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.core.utils.diasHasta
import cl.miproyecto.minegocio.core.utils.inicioDelDia
import cl.miproyecto.minegocio.core.utils.inicioDelDiaSiguiente
import cl.miproyecto.minegocio.core.utils.normalizarNombre
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.model.ResumenPeriodo
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import cl.miproyecto.minegocio.core.utils.diaActual
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Los 3 botones de filtro de la pantalla de inicio.
enum class FiltroProductos { TODOS, POR_VENCER, SIN_STOCK }

// Todo lo que necesita la pantalla de inicio para dibujarse.
data class InicioUiState(
    val cargando: Boolean = true,
    val configuracion: Configuracion = Configuracion(),
    val hoy: ResumenPeriodo = ResumenPeriodo(),
    // La lista ya filtrada y buscada.
    val productos: List<ProductoResumen> = emptyList(),
    val totalProductos: Int = 0,
    val porVencer: Int = 0,
    val sinStock: Int = 0,
    val busqueda: String = "",
    val filtro: FiltroProductos = FiltroProductos.TODOS,
)

class ListaProductosViewModel(
    productoRepository: ProductoRepository,
    reporteRepository: ReporteRepository,
    configuracionRepository: ConfiguracionRepository,
) : ViewModel() {

    // Lo que el usuario escribe en el buscador y el filtro elegido.
    private val busqueda = MutableStateFlow("")
    private val filtro = MutableStateFlow(FiltroProductos.TODOS)

    // "combine" junta 5 Flows: cada vez que cambia uno (llega un producto nuevo,
    // se escribe en el buscador, se vende algo...), se recalcula la pantalla.
    // diaActual().flatMapLatest: al cambiar el día (pasada la medianoche) se vuelven a pedir
    // los datos, así "Ventas de hoy" parte de cero y los lotes que vencieron dejan de contar.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InicioUiState> = combine(
        diaActual().flatMapLatest { productoRepository.obtenerConStock() },
        diaActual().flatMapLatest { dia -> reporteRepository.resumen(dia, inicioDelDiaSiguiente(dia)) },
        configuracionRepository.configuracion,
        busqueda,
        filtro,
    ) { productos, hoy, config, texto, filtroActual ->
        val porVencer = productos.filter { estaPorVencer(it, config) }
        val sinStock = productos.filter { it.stock == 0 }

        // 1) Primero el filtro.
        val filtrados = when (filtroActual) {
            FiltroProductos.TODOS -> productos
            FiltroProductos.POR_VENCER -> porVencer
            FiltroProductos.SIN_STOCK -> sinStock
        }
        // 2) Después la búsqueda, por nombre normalizado o por código de barras.
        val buscado = normalizarNombre(texto)
        val resultado = if (buscado.isEmpty()) filtrados else filtrados.filter {
            it.producto.nombreNormalizado.contains(buscado) || (it.producto.sku?.contains(texto.trim()) == true)
        }

        InicioUiState(
            cargando = false,
            configuracion = config,
            hoy = hoy,
            productos = resultado,
            totalProductos = productos.size,
            porVencer = porVencer.size,
            sinStock = sinStock.size,
            busqueda = texto,
            filtro = filtroActual,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InicioUiState(),
    )

    fun buscar(texto: String) {
        busqueda.value = texto
    }

    fun elegirFiltro(nuevo: FiltroProductos) {
        filtro.value = nuevo
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Se pide el repositorio a la Application guardada en "instancia".
                // (Navigation 3 no entrega APPLICATION_KEY, por eso no se usa this[...]).
                val app = MiNegocioApplication.instancia
                ListaProductosViewModel(app.productoRepository, app.reporteRepository, app.configuracionRepository)
            }
        }
    }
}

// Un producto está "por vencer" si su lote más próximo vence dentro de los días de aviso
// (o ya venció).
fun estaPorVencer(producto: ProductoResumen, config: Configuracion): Boolean {
    val vence = producto.proximoVencimiento ?: return false
    return diasHasta(vence) <= config.diasAvisoVencimiento
}
