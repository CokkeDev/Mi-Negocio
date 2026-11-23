package cl.miproyecto.minegocio.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CostoLoteTest {

    @Test
    fun divide_el_costo_total_entre_las_unidades() {
        // El caso del bug: 15 libros por $15.000 -> $1.000 cada uno (no $15.000).
        assertEquals(1_000, costoPorUnidad(15_000, 15))
    }

    @Test
    fun redondea_al_peso_mas_cercano() {
        assertEquals(3_333, costoPorUnidad(10_000, 3))
        assertEquals(3, costoPorUnidad(5, 2))
    }

    @Test
    fun costo_cero_da_cero() {
        assertEquals(0, costoPorUnidad(0, 12))
    }

    @Test
    fun no_acepta_unidades_cero_ni_costo_negativo() {
        assertThrows(IllegalArgumentException::class.java) { costoPorUnidad(1_000, 0) }
        assertThrows(IllegalArgumentException::class.java) { costoPorUnidad(-1, 5) }
    }
}
