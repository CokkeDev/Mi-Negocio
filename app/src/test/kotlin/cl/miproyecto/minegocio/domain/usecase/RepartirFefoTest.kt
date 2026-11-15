package cl.miproyecto.minegocio.domain.usecase

import cl.miproyecto.minegocio.domain.model.Lote
import org.junit.Assert.assertEquals
import org.junit.Test

class RepartirFefoTest {

    private fun lote(id: Long, disponibles: Int) = Lote(
        idLote = id,
        idProducto = 1,
        precioCompra = 1000,
        fechaIngreso = 0,
        fechaVencimiento = null,
        unidadesIniciales = disponibles,
        unidadesDisponibles = disponibles,
    )

    @Test
    fun alcanzaConElPrimerLote() {
        val resultado = repartirFefo(listOf(lote(1, 9), lote(2, 24)), 5)
        assertEquals(listOf(1L to 5), resultado.map { it.first.idLote to it.second })
    }

    @Test
    fun usaVariosLotesEnOrden() {
        val resultado = repartirFefo(listOf(lote(1, 9), lote(2, 24)), 12)
        assertEquals(listOf(1L to 9, 2L to 3), resultado.map { it.first.idLote to it.second })
    }

    @Test
    fun saltaLotesVacios() {
        val resultado = repartirFefo(listOf(lote(1, 0), lote(2, 4)), 4)
        assertEquals(listOf(2L to 4), resultado.map { it.first.idLote to it.second })
    }

    @Test(expected = StockInsuficienteException::class)
    fun sinStockSuficienteLanzaError() {
        repartirFefo(listOf(lote(1, 2), lote(2, 3)), 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cantidadCeroNoSePermite() {
        repartirFefo(listOf(lote(1, 2)), 0)
    }
}
