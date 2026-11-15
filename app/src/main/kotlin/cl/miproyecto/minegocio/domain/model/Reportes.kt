package cl.miproyecto.minegocio.domain.model

// Los números de un periodo (hoy, semana, mes o año).
data class ResumenPeriodo(
    // Total cobrado en ventas.
    val ventas: Int = 0,
    // Cantidad de boletas.
    val boletas: Int = 0,
    // Ganancia de las ventas: (precio de venta - precio de compra del lote) x unidades.
    val gananciaVentas: Int = 0,
    // Lo pagado en gastos.
    val gastos: Int = 0,
    // Lo que costaron las unidades perdidas (precio de compra x unidades).
    val merma: Int = 0,
) {
    // Lo que de verdad le queda al negocio.
    val gananciaNeta: Int get() = gananciaVentas - gastos - merma
}

// Un producto del ranking "Más vendidos".
data class MasVendido(
    val nombre: String,
    val unidades: Int,
    val ganancia: Int,
)

// La ganancia neta de un mes, para el gráfico.
data class GananciaMes(
    // Primer día del mes, en milisegundos.
    val inicioMes: Long,
    val gananciaNeta: Int,
)
