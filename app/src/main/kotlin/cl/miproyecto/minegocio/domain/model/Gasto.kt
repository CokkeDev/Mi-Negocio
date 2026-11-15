package cl.miproyecto.minegocio.domain.model

// Un gasto del negocio (luz, arriendo, transporte...).
data class Gasto(
    val idGasto: Long = 0,
    val descripcion: String,
    val monto: Int,
    val categoria: CategoriaGasto,
    val fecha: Long,
)

// "enum class": una lista cerrada de opciones. Un gasto solo puede tener una de estas.
enum class CategoriaGasto(val etiqueta: String) {
    SERVICIOS("Servicios"),
    ARRIENDO("Arriendo"),
    TRANSPORTE("Transporte"),
    INSUMOS("Insumos"),
    OTROS("Otros");

    companion object {
        // Convierte el texto guardado en la base de datos de vuelta a la categoría.
        // Si no lo reconoce, usa OTROS para no caerse.
        fun desdeNombre(nombre: String): CategoriaGasto =
            entries.firstOrNull { it.name == nombre } ?: OTROS
    }
}
