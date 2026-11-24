package cl.miproyecto.minegocio.domain.usecase

// TOPES DE MONTOS
// Los montos son Int (máx. 2.147.483.647). Sin topes, precio x cantidad podía desbordarse
// y dar una boleta con total NEGATIVO. Estos límites dejan mucho margen para un kiosco.
const val PRECIO_VENTA_MAXIMO = 1_000_000
const val UNIDADES_LOTE_MAXIMO = 100_000
const val COSTO_LOTE_MAXIMO = 1_000_000_000
const val GASTO_MAXIMO = 10_000_000
const val BOLETA_MAXIMA = 1_000_000_000

// Total de una boleta calculado en Long (no se desborda) para poder compararlo con el tope.
fun totalSeguro(lineas: List<cl.miproyecto.minegocio.domain.model.LineaVenta>): Long =
    lineas.sumOf { it.precioUnitario.toLong() * it.cantidad }
