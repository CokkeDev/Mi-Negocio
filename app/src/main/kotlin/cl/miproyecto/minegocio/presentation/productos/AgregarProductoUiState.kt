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

    // Ya hay un producto activo igual. Se indica POR QUÉ (mismo nombre y/o mismo código de
    // barras) para marcar el campo correcto y decir cuál producto es.
    data class YaExiste(
        val porNombre: Boolean,
        val porCodigo: Boolean,
        val nombreExistente: String,
    ) : AgregarProductoUiState

    data class OfrecerReactivar(val producto: Producto) : AgregarProductoUiState

    data class ProductoReactivado(val idProducto: Long) : AgregarProductoUiState

    data class Error(val mensaje: String, val campo: CampoProducto = CampoProducto.GENERAL) : AgregarProductoUiState
}
