package cl.miproyecto.minegocio.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TextoUtilsTest {

    @Test
    fun quitaGuionesYPasaAMinusculas() {
        assertEquals("coca cola 3l", normalizarNombre("Coca-Cola 3L"))
    }

    @Test
    fun quitaEspaciosRepetidosYDeLosBordes() {
        assertEquals("coca cola 3l", normalizarNombre("  COCA   cola  3l "))
    }

    @Test
    fun quitaTildes() {
        assertEquals("cafe nescafe", normalizarNombre("Café Nescafé"))
    }

    @Test
    fun laEnieEsUnaLetraDistintaDeLaN() {
        assertEquals("piña", normalizarNombre("Piña"))
        assertNotEquals(normalizarNombre("Piña"), normalizarNombre("Pina"))
        assertNotEquals(normalizarNombre("Año"), normalizarNombre("Ano"))
    }

    @Test
    fun nombresSoloConSimbolosOEmojisNoQuedanVacios() {
        assertEquals("!!!", normalizarNombre("!!!"))
        assertEquals("🍫", normalizarNombre(" 🍫 "))
    }

    @Test
    fun conservaLetrasDeOtrosAlfabetos() {
        assertEquals("日本茶", normalizarNombre("日本茶"))
    }

    @Test
    fun nombreEnBlancoSigueVacio() {
        assertEquals("", normalizarNombre("   "))
    }

    @Test
    fun nombresEscritosDistintoSonIguales() {
        assertEquals(normalizarNombre("Pan"), normalizarNombre("  PAN "))
    }
}