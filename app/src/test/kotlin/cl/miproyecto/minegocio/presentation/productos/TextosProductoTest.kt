package cl.miproyecto.minegocio.presentation.productos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextosProductoTest {

    @Test
    fun sin_unidades_no_avisa_del_stock() {
        val texto = textoConfirmarEliminar("Café", 0)
        assertTrue(texto.contains("“Café”"))
        assertFalse(texto.contains("Todavía tiene"))
    }

    @Test
    fun con_unidades_avisa_cuantas_y_sugiere_merma_y_reactivar() {
        val texto = textoConfirmarEliminar("Café", 4)
        assertTrue(texto.contains("4 unidades"))
        assertTrue(texto.contains("merma"))
        assertTrue(texto.contains("Productos eliminados"))
    }

    @Test
    fun una_sola_unidad_va_en_singular() {
        assertTrue(textoConfirmarEliminar("Café", 1).contains("1 unidad en lotes"))
        assertEquals(false, textoConfirmarEliminar("Café", 1).contains("1 unidades"))
    }
}
