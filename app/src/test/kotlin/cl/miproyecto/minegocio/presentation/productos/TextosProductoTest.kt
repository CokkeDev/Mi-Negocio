package cl.miproyecto.minegocio.presentation.productos

import cl.miproyecto.minegocio.core.utils.inicioDelDia
import cl.miproyecto.minegocio.domain.model.Lote
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

    private fun lote(id: Long, ingreso: Long) = Lote(
        idLote = id, idProducto = 1, precioCompra = 1, fechaIngreso = ingreso, fechaVencimiento = null,
        unidadesIniciales = 1, unidadesDisponibles = 1,
    )

    @Test
    fun el_lote_se_nombra_por_su_fecha_y_no_por_el_numero_interno() {
        val dia = inicioDelDia() + 3_600_000
        val etiquetas = etiquetasDeLotes(listOf(lote(id = 4, ingreso = dia)))
        assertEquals("Lote del ${cl.miproyecto.minegocio.core.utils.formatoDiaMes(dia)}", etiquetas[4L])
        assertFalse(etiquetas[4L]!!.contains("#"))
    }

    @Test
    fun lotes_del_mismo_dia_se_numeran_y_de_dias_distintos_no() {
        val hoy = inicioDelDia() + 3_600_000
        val ayer = hoy - 24L * 3_600_000
        val etiquetas = etiquetasDeLotes(listOf(lote(7, hoy), lote(5, hoy), lote(9, ayer)))
        val fHoy = cl.miproyecto.minegocio.core.utils.formatoDiaMes(hoy)
        val fAyer = cl.miproyecto.minegocio.core.utils.formatoDiaMes(ayer)
        // El más antiguo (id menor) es el (1).
        assertEquals("Lote del $fHoy (1)", etiquetas[5L])
        assertEquals("Lote del $fHoy (2)", etiquetas[7L])
        assertEquals("Lote del $fAyer", etiquetas[9L])
    }
}
