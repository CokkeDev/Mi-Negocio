package cl.miproyecto.minegocio

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// LAS PANTALLAS DE LA APP (las "llaves" de Navigation 3).
// - "data object": pantallas que no necesitan datos para abrirse.
// - "data class": pantallas que necesitan saber algo, por ejemplo QUÉ producto mostrar.
// @Serializable permite que Android las guarde si la app se cierra en segundo plano.

// Inicio: la lista de productos.
@Serializable data object ListaProductos : NavKey

// "sku" viene lleno cuando se escaneó un código que no estaba registrado.
@Serializable data class AgregarProducto(val sku: String? = null) : NavKey

@Serializable data class DetalleProducto(val idProducto: Long) : NavKey

@Serializable data class AgregarLote(val idProducto: Long) : NavKey

@Serializable data class RegistrarMerma(val idProducto: Long) : NavKey

@Serializable data object Venta : NavKey

@Serializable data object Reportes : NavKey

@Serializable data object Gastos : NavKey

@Serializable data object Configuracion : NavKey

@Serializable data object ProductosEliminados : NavKey
