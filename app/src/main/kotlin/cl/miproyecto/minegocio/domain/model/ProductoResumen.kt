package cl.miproyecto.minegocio.domain.model

// Un producto junto con los datos de sus lotes que se muestran en las listas.
data class ProductoResumen(
    val producto: Producto,
    // Suma de unidades disponibles de todos sus lotes.
    val stock: Int,
    // Cuántos lotes todavía tienen unidades.
    val lotesConStock: Int,
    // Cuántos lotes ha tenido en total (aunque estén agotados).
    val totalLotes: Int,
    // La fecha de vencimiento más cercana de los lotes con stock (null = ninguno vence).
    val proximoVencimiento: Long?,
)
