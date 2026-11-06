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
fun normalizarNombre(nombre: String): String =
    // 1) trim(): quita los espacios del inicio y del final  -> "Café-Nescafé"
    // 2) lowercase(): pasa todo a minúsculas                -> "café-nescafé"
    // 3) Normalizer NFD: separa cada tilde de su letra (é -> e + ´)
    Normalizer.normalize(nombre.trim().lowercase(), Normalizer.Form.NFD)
        // 4) Borra las tildes que quedaron sueltas.
        //    "\\p{M}" significa "marcas" (tildes, diéresis...) -> "cafe-nescafe"
        //    La ñ también pierde su "tilde": "piña" -> "pina".
        .replace(Regex("\\p{M}"), "")
        // 5) Todo lo que NO sea letra, número o espacio se cambia por un espacio.
        //    El "^" dentro de [ ] significa "que NO sea" -> "cafe nescafe"
        .replace(Regex("[^a-z0-9 ]"), " ")
        // 6) Si quedaron varios espacios seguidos, los deja en uno solo.
        //    "\\s+" significa "uno o más espacios"
        .replace(Regex("\\s+"), " ")
        // 7) Vuelve a quitar los espacios de los bordes (pudieron aparecer en el paso 5).
        .trim()
