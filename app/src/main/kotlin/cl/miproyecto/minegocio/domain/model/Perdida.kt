package cl.miproyecto.minegocio.domain.model

// Una merma: unidades de un lote que se perdieron.
data class Perdida(
    val idPerdida: Long = 0,
    val idLote: Long,
    val cantidad: Int,
    val motivo: String,
    val nota: String?,
    val fecha: Long,
)

// Los motivos que se ofrecen en la pantalla de merma.
val MOTIVOS_MERMA = listOf("Vencido", "Dañado", "Robo o extravío", "Consumo propio", "Otro")
