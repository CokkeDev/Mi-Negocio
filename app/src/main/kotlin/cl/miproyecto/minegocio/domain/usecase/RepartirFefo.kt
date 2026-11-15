package cl.miproyecto.minegocio.domain.usecase

import cl.miproyecto.minegocio.domain.model.Lote

// FEFO: "First Expired, First Out" = lo que vence primero, sale primero.
//
// Recibe los lotes YA ORDENADOS (como los entrega LoteDao.obtenerPorProducto)
// y la cantidad que se quiere sacar. Devuelve de qué lote sale cada unidad.
//
// Ejemplo: lotes [A: 9 u., B: 24 u.] y cantidad 12
//   -> [(A, 9), (B, 3)]
//
// Si no alcanza el stock lanza un error, así la venta completa se cancela.
// Es una función "pura" (no usa la base de datos), por eso se puede probar con tests.
fun repartirFefo(lotes: List<Lote>, cantidad: Int): List<Pair<Lote, Int>> {
    require(cantidad > 0) { "La cantidad debe ser mayor a 0" }
    val resultado = mutableListOf<Pair<Lote, Int>>()
    var faltan = cantidad
    for (lote in lotes) {
        if (faltan == 0) break
        if (lote.unidadesDisponibles <= 0) continue
        // Se saca lo que falte, pero nunca más de lo que tiene el lote.
        val sacar = minOf(faltan, lote.unidadesDisponibles)
        resultado.add(lote to sacar)
        faltan -= sacar
    }
    if (faltan > 0) throw StockInsuficienteException()
    return resultado
}

// Error propio para cuando no hay unidades suficientes.
class StockInsuficienteException : Exception("Stock insuficiente")
