package cl.miproyecto.minegocio.core.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DiaActualTest {

    // Un "reloj" falso que avanza con el tiempo virtual del test.
    private val base = inicioDelDia() + TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(30)

    @Test
    fun emite_el_dia_de_hoy_y_vuelve_a_emitir_al_cambiar_el_dia() = runTest {
        val emitidos = mutableListOf<Long>()
        val job = launch { diaActual(ahora = { base + testScheduler.currentTime }).toList(emitidos) }

        advanceTimeBy(TimeUnit.MINUTES.toMillis(10))
        assertEquals("a las 23:40 sigue siendo el mismo día", 1, emitidos.size)

        advanceTimeBy(TimeUnit.MINUTES.toMillis(30))
        assertEquals("pasada la medianoche cambia el día", 2, emitidos.size)
        assertEquals(inicioDelDiaSiguiente(base), emitidos[1])
        job.cancel()
    }

    @Test
    fun no_repite_el_mismo_dia() = runTest {
        val emitidos = mutableListOf<Long>()
        val job = launch { diaActual(ahora = { base }).toList(emitidos) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(5))
        assertEquals(listOf(inicioDelDia(base)), emitidos)
        job.cancel()
    }
}
