package cl.miproyecto.minegocio.domain.repository

import cl.miproyecto.minegocio.domain.model.Lote
import kotlinx.coroutines.flow.Flow

// Lo que la app puede hacer con los lotes.
// Solo usa el modelo Lote (domain), nunca LoteEntity (Room).
interface LoteRepository {

    // Guarda un lote nuevo y devuelve su id.
    suspend fun insertar(lote: Lote): Long

    // Los lotes con unidades de un producto, en orden FEFO.
    fun obtenerPorProducto(idProducto: Long): Flow<List<Lote>>

    // Cuántas unidades hay en total de un producto.
    fun obtenerStock(idProducto: Long): Flow<Int>
}