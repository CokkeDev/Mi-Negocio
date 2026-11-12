package cl.miproyecto.minegocio.core.utils

import org.junit.Assert.assertEquals
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
    fun convierteEnieEnN() {
        assertEquals("pina", normalizarNombre("Piña"))
    }

    @Test
    fun soloSimbolosQuedaVacio() {
        assertEquals("", normalizarNombre("!!!"))
    }

    @Test
    fun nombresEscritosDistintoSonIguales() {
        assertEquals(normalizarNombre("Pan"), normalizarNombre("  PAN "))
    }
}