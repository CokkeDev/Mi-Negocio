package cl.miproyecto.minegocio.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoTest {

    @Test
    fun separaMilesConPunto() {
        assertEquals("0", separarMiles(0))
        assertEquals("900", separarMiles(900))
        assertEquals("1.190", separarMiles(1190))
        assertEquals("412.300", separarMiles(412300))
        assertEquals("1.000.000", separarMiles(1_000_000))
    }

    @Test
    fun formatoPesosConSignoNegativo() {
        assertEquals("$18.400", formatoPesos(18400))
        assertEquals("−$3.600", formatoPesos(-3600))
        assertEquals("+$8.400", formatoPesosConSigno(8400))
    }

    @Test
    fun inicialesDelNombre() {
        assertEquals("BC", iniciales("Bebida cola 3L"))
        assertEquals("E", iniciales("Encendedor"))
        assertEquals("?", iniciales("   "))
    }

    @Test
    fun diasHastaMananaEsUno() {
        val hoy = inicioDelDia()
        assertEquals(0, diasHasta(hoy + 5 * 3_600_000L, hoy))
        assertEquals(1, diasHasta(inicioDelDiaSiguiente(hoy), hoy))
        assertEquals(-1, diasHasta(hoy - 3_600_000L, hoy))
    }
}
