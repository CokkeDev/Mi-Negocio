package cl.miproyecto.minegocio.domain.model

// Las preferencias del usuario (pantalla Configuración).
data class Configuracion(
    val nombreNegocio: String = "Mi Negocio",
    // Cuántos días antes de vencer se marca un lote como "por vencer".
    val diasAvisoVencimiento: Int = 3,
    // Bajo cuántas unidades se marca un producto con "stock bajo".
    val stockBajo: Int = 5,
    val sonidoAlEscanear: Boolean = true,
    val vibrarAlAgregar: Boolean = true,
    val tema: Tema = Tema.SISTEMA,
    // Cuándo se hizo el último respaldo (null = nunca).
    val ultimoRespaldo: Long? = null,
)

enum class Tema { SISTEMA, CLARO, OSCURO }
