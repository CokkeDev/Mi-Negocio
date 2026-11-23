package cl.miproyecto.minegocio.domain.usecase

// COSTO POR UNIDAD DE UN LOTE
// En el formulario se ingresa lo que se pagó por el lote COMPLETO (ej: 15 libros = $15.000),
// pero la base de datos guarda el precio de compra de CADA unidad ($1.000), porque así
// se calcula la ganancia de cada venta: precio de venta - precio de compra.
//
// Se redondea al peso más cercano (los pesos chilenos no tienen decimales):
//   15.000 / 15 = 1.000   |   10.000 / 3 = 3.333,33 -> 3.333   |   5 / 2 = 2,5 -> 3
fun costoPorUnidad(costoTotal: Int, unidades: Int): Int {
    require(costoTotal >= 0) { "El costo total no puede ser negativo" }
    require(unidades > 0) { "Las unidades deben ser mayores a 0" }
    // Se suma la mitad antes de dividir para redondear (la división de Int corta los decimales).
    // toLong() evita que la suma se desborde con montos muy grandes.
    return ((costoTotal.toLong() + unidades / 2) / unidades).toInt()
}
