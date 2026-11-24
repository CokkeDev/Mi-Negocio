package cl.miproyecto.minegocio.core.utils

// Normalizer es una herramienta de Java que sirve para separar las tildes de las letras.
import java.text.Normalizer

// NORMALIZAR NOMBRE
// Convierte el nombre de un producto a una forma "limpia" para poder compararlo.
// Así se detecta que dos nombres escritos distinto son el mismo producto:
//   "Coca-Cola 3L"        -> "coca cola 3l"
//   "  COCA cola   3l "   -> "coca cola 3l"
//   "Café Nescafé"        -> "cafe nescafe"
//
// Es una función "pura": no usa Android ni la base de datos.
// Entra un texto y sale otro texto, por eso vive en core/utils.
//
// Funciona como una cadena: cada línea toma el resultado de la anterior
// y le hace un cambio más. Ejemplo con "  Café-Nescafé  ":
fun normalizarNombre(nombre: String): String {
    val limpio =
        // 1) trim() + lowercase(): sin espacios en los bordes y en minúsculas.
        // 2) Normalizer NFD: separa cada tilde de su letra (é -> e + ´)
        Normalizer.normalize(nombre.trim().lowercase(), Normalizer.Form.NFD)
            // 3) La "ñ" es una letra distinta de la "n" (piña != pina): se vuelve a unir
            //    antes de borrar las tildes, para que no se pierda.
            .replace("n\u0303", "ñ")
            // 4) Borra las tildes sueltas que quedaron (á -> a). "\\p{M}" = "marcas".
            .replace(Regex("\\p{M}"), "")
            // 5) Todo lo que NO sea letra (de cualquier alfabeto), número o espacio -> espacio.
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            // 6) Varios espacios seguidos -> uno solo, y sin espacios en los bordes.
            .replace(Regex("\\s+"), " ")
            .trim()
    // Si el nombre solo tenía emojis o símbolos (ej: "🍫"), no se puede dejar vacío:
    // se usa el texto original en minúsculas para poder compararlo igual.
    return limpio.ifEmpty { nombre.trim().lowercase().replace(Regex("\\s+"), " ") }
}
