package cl.miproyecto.minegocio.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.miproyecto.minegocio.MiNegocioApplication
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.repository.VentaRepository
import cl.miproyecto.minegocio.domain.usecase.BOLETA_MAXIMA
import cl.miproyecto.minegocio.domain.usecase.totalSeguro
import cl.miproyecto.minegocio.core.utils.diaActual
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Avisos que aparecen sobre la cámara o como diálogo.
sealed interface AvisoVenta {
    // "Galletas agregadas +1" (el número cambia para que se note cada escaneo).
    data class Agregado(val nombre: String, val contador: Int) : AvisoVenta
    data class SinStock(val nombre: String) : AvisoVenta
    data class StockMaximo(val nombre: String, val stock: Int) : AvisoVenta
    data class CodigoNoRegistrado(val codigo: String) : AvisoVenta
    // La boleta llegaría al tope de montos (evita que el total se desborde).
    data object BoletaMaxima : AvisoVenta
}

data class VentaUiState(
    // La boleta que se está armando (en memoria, todavía no se guarda).
    val lineas: List<LineaVenta> = emptyList(),
    val aviso: AvisoVenta? = null,
    val registrando: Boolean = false,
) {
    val total: Int get() = lineas.sumOf { it.subtotal }
    val unidades: Int get() = lineas.sumOf { it.cantidad }
}

// Resultado de confirmar la venta (lo escucha la pantalla una sola vez).
sealed interface ResultadoVenta {
    data class Exito(val total: Int) : ResultadoVenta
    data object Fallo : ResultadoVenta
}

// Sigue el diagrama "Venta con escaneo de SKU" del README.
class VentaViewModel(
    private val productoRepository: ProductoRepository,
    private val ventaRepository: VentaRepository,
    configuracionRepository: ConfiguracionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentaUiState())
    val uiState: StateFlow<VentaUiState> = _uiState.asStateFlow()

    private val _resultado = MutableStateFlow<ResultadoVenta?>(null)
    val resultado: StateFlow<ResultadoVenta?> = _resultado.asStateFlow()

    val configuracion: StateFlow<Configuracion> = configuracionRepository.configuracion

    // Todos los productos (para elegir los que no tienen código de barras).
    @OptIn(ExperimentalCoroutinesApi::class)
    val productos: StateFlow<List<ProductoResumen>> = diaActual().flatMapLatest { productoRepository.obtenerConStock() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var contadorAvisos = 0

    init {
        // La boleta se mantiene al día con los productos: si cambia el precio o el stock
        // de un producto mientras se arma la boleta, la línea se actualiza (no se cobra el
        // precio viejo). Si el producto ya no existe o se quedó sin stock, la línea se quita.
        viewModelScope.launch {
            diaActual().flatMapLatest { productoRepository.obtenerConStock() }.collect { lista ->
                val porId = lista.associateBy { it.producto.idProducto }
                _uiState.update { estado ->
                    // Mientras se confirma la venta la boleta no se toca.
                    if (estado.registrando || estado.lineas.isEmpty()) return@update estado
                    val nuevas = estado.lineas.mapNotNull { linea ->
                        val actual = porId[linea.idProducto] ?: return@mapNotNull null
                        val cantidad = minOf(linea.cantidad, actual.stock)
                        if (cantidad <= 0) null
                        else linea.copy(
                            precioUnitario = actual.producto.precioVenta,
                            cantidad = cantidad,
                            stock = actual.stock,
                        )
                    }
                    if (nuevas == estado.lineas) estado else estado.copy(lineas = nuevas)
                }
            }
        }
    }

    // El escáner leyó un código. Devuelve true si se agregó (para sonar / vibrar).
    fun escanearSku(codigo: String, alAgregar: () -> Unit) {
        viewModelScope.launch {
            val resumen = productoRepository.buscarPorSku(codigo)
            if (resumen == null) {
                _uiState.update { it.copy(aviso = AvisoVenta.CodigoNoRegistrado(codigo)) }
            } else if (agregar(resumen)) {
                alAgregar()
            }
        }
    }

    // Producto sin código, elegido de la lista.
    fun agregarProducto(resumen: ProductoResumen) {
        agregar(resumen)
    }

    private fun agregar(resumen: ProductoResumen): Boolean {
        // Mientras se confirma la venta la boleta no se puede modificar.
        if (_uiState.value.registrando) return false
        val producto = resumen.producto
        if (resumen.stock <= 0) {
            _uiState.update { it.copy(aviso = AvisoVenta.SinStock(producto.nombre)) }
            return false
        }
        val lineas = _uiState.value.lineas
        val existente = lineas.firstOrNull { it.idProducto == producto.idProducto }
        val nuevas = when {
            // Nuevo en la boleta: línea con cantidad 1.
            existente == null -> lineas + LineaVenta(
                idProducto = producto.idProducto,
                nombre = producto.nombre,
                precioUnitario = producto.precioVenta,
                cantidad = 1,
                stock = resumen.stock,
            )
            // Ya está: cantidad + 1, pero nunca más que el stock.
            existente.cantidad >= resumen.stock -> {
                _uiState.update { it.copy(aviso = AvisoVenta.StockMaximo(producto.nombre, resumen.stock)) }
                return false
            }
            else -> lineas.map {
                if (it.idProducto == producto.idProducto) it.copy(cantidad = it.cantidad + 1, stock = resumen.stock, precioUnitario = producto.precioVenta) else it
            }
        }
        if (totalSeguro(nuevas) > BOLETA_MAXIMA) {
            _uiState.update { it.copy(aviso = AvisoVenta.BoletaMaxima) }
            return false
        }
        contadorAvisos++
        _uiState.update { it.copy(lineas = nuevas, aviso = AvisoVenta.Agregado(producto.nombre, contadorAvisos)) }
        return true
    }

    fun sumar(idProducto: Long) = cambiarCantidad(idProducto, +1)

    // Si llega a 0, la línea se quita de la boleta.
    fun restar(idProducto: Long) = cambiarCantidad(idProducto, -1)

    private fun cambiarCantidad(idProducto: Long, delta: Int) {
        _uiState.update { estado ->
            if (estado.registrando) return@update estado
            val nuevas = estado.lineas.mapNotNull { linea ->
                if (linea.idProducto != idProducto) return@mapNotNull linea
                val cantidad = (linea.cantidad + delta).coerceAtMost(linea.stock)
                if (cantidad <= 0) null else linea.copy(cantidad = cantidad)
            }
            // Si con el cambio la boleta pasa el tope de montos, no se aplica.
            if (totalSeguro(nuevas) > BOLETA_MAXIMA) estado.copy(aviso = AvisoVenta.BoletaMaxima)
            else estado.copy(lineas = nuevas)
        }
    }

    fun cerrarAviso() {
        _uiState.update { it.copy(aviso = null) }
    }

    fun confirmarVenta() {
        val estado = _uiState.value
        if (estado.lineas.isEmpty() || estado.registrando) return
        // Se marca "registrando" ANTES de lanzar la corrutina: así un segundo toque o un
        // cambio en la boleta justo después de confirmar ya no entran.
        _uiState.update { it.copy(registrando = true) }
        viewModelScope.launch {
            try {
                ventaRepository.registrarVenta(estado.lineas)
                // Éxito: se limpia la boleta.
                _uiState.value = VentaUiState()
                _resultado.value = ResultadoVenta.Exito(estado.total)
            } catch (e: Exception) {
                // La transacción hizo ROLLBACK: no se descontó stock.
                _uiState.update { it.copy(registrando = false) }
                _resultado.value = ResultadoVenta.Fallo
            }
        }
    }

    fun resultadoMostrado() {
        _resultado.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = MiNegocioApplication.instancia
                VentaViewModel(app.productoRepository, app.ventaRepository, app.configuracionRepository)
            }
        }
    }
}
