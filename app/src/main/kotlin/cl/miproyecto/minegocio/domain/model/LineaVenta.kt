package cl.miproyecto.minegocio.domain.model

// Una línea de la boleta que se está armando (todavía no está guardada).
data class LineaVenta(
    val idProducto: Long,
    val nombre: String,
    val precioUnitario: Int,
    val cantidad: Int,
    // Stock disponible: la cantidad nunca puede pasar de aquí.
    val stock: Int,
) {
    val subtotal: Int get() = precioUnitario * cantidad
}
