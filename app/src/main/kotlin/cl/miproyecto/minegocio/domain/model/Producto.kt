package cl.miproyecto.minegocio.domain.model

data class Producto(
    val idProducto: Long = 0,
    val sku: String?,
    val nombre: String,
    val nombreNormalizado: String,
    val urlImagen: String?,
    val precioVenta: Int,
    val activo: Boolean = true,
)