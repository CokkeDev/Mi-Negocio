package cl.miproyecto.minegocio.core.utils

import kotlin.math.abs

// Ayudas para mostrar números como pesos chilenos.

// 18400 -> "18.400". Separa los miles con punto, como se escribe en Chile.
fun separarMiles(valor: Int): String {
    val texto = abs(valor.toLong()).toString()
    // reversed + chunked(3): "00481" -> ["004", "81"] -> se unen con "." y se dan vuelta.
    val conPuntos = texto.reversed().chunked(3).joinToString(".").reversed()
    return if (valor < 0) "-$conPuntos" else conPuntos
}

// 18400 -> "$18.400"   |   -3600 -> "−$3.600"
fun formatoPesos(valor: Int): String =
    if (valor < 0) "−$" + separarMiles(-valor) else "$" + separarMiles(valor)

// Igual que formatoPesos, pero siempre con signo: "+$8.400" o "−$3.600".
fun formatoPesosConSigno(valor: Int): String =
    if (valor < 0) formatoPesos(valor) else "+" + formatoPesos(valor)

// Las iniciales para el cuadrito del producto: "Bebida cola 3L" -> "BC".
fun iniciales(nombre: String): String =
    nombre.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
