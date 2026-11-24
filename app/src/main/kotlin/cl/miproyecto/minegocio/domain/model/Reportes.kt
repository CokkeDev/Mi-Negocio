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
    // Lo invertido en lotes ingresados en el periodo (costo total de cada lote).
    val compras: Int = 0,
) {
    // Lo que de verdad le queda al negocio: ventas - lotes comprados - gastos.
    // La merma no se resta: ya está pagada dentro del costo del lote.
    val gananciaNeta: Int get() = ventas - compras - gastos

    // Flujo de caja: lo que entró por ventas menos lo que se pagó por lotes.
    // Ej: lote de 15 u. a $15.000 y 2 ventas de $2.000 -> 4.000 - 15.000 = −11.000.
    val ventasMenosCompras: Int get() = ventas - compras
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
