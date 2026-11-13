package cl.miproyecto.minegocio.domain.model

// Un lote es cada compra de un producto (modelo limpio, sin Room).
data class Lote(
    val idLote: Long = 0,
    val idProducto: Long,
    val precioCompra: Int,
    val fechaIngreso: Long,
    val fechaVencimiento: Long?,
    val unidadesIniciales: Int,
    val unidadesDisponibles: Int,
)