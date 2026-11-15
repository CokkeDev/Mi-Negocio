package cl.miproyecto.minegocio.presentation.productos

import cl.miproyecto.minegocio.domain.model.Producto

// Los campos del formulario, para saber DEBAJO de cuál mostrar un error.
enum class CampoProducto { NOMBRE, PRECIO, GENERAL }

// Los estados posibles de la pantalla "Agregar producto".
// "sealed interface": una lista cerrada de opciones; el "when" obliga a revisarlas todas.
sealed interface AgregarProductoUiState {
    data object Inicial : AgregarProductoUiState

    data object Guardando : AgregarProductoUiState

    // Se guardó: se lleva el id para abrir el detalle del producto nuevo.
    data class ProductoAgregado(val idProducto: Long) : AgregarProductoUiState

    data object YaExiste : AgregarProductoUiState

    data class OfrecerReactivar(val producto: Producto) : AgregarProductoUiState

    data class ProductoReactivado(val idProducto: Long) : AgregarProductoUiState

    data class Error(val mensaje: String, val campo: CampoProducto = CampoProducto.GENERAL) : AgregarProductoUiState
}
