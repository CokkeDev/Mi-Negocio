package cl.miproyecto.minegocio

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object ListaProductos : NavKey

@Serializable data object AgregarProducto : NavKey