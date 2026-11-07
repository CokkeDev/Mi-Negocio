package cl.miproyecto.minegocio.presentation.productos

import cl.miproyecto.minegocio.domain.model.Producto

sealed interface AgregarProductoUiState{
    data object Inicial : AgregarProductoUiState

    data object  Guardando : AgregarProductoUiState

    data object ProductoAgregado : AgregarProductoUiState

    data object YaExiste : AgregarProductoUiState

    data class OfrecerReactivar(val producto: Producto): AgregarProductoUiState

    data object ProductoReactivado : AgregarProductoUiState

    data class Error(val mensaje: String) : AgregarProductoUiState
}