package cl.miproyecto.minegocio.data.repository

import cl.miproyecto.minegocio.data.local.dao.LoteDao
import cl.miproyecto.minegocio.data.mapper.toDomain
import cl.miproyecto.minegocio.data.mapper.toEntity
import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// La "cocina": cumple lo que promete LoteRepository usando el DAO.
class LoteRepositoryImpl(
    private val dao: LoteDao,
) : LoteRepository {

    // Lote -> LoteEntity, y se guarda.
    override suspend fun insertar(lote: Lote): Long =
        dao.insertar(lote.toEntity())

    // Cada lista que llega de Room se convierte de LoteEntity a Lote.
    override fun obtenerPorProducto(idProducto: Long): Flow<List<Lote>> =
        dao.obtenerPorProducto(idProducto).map { lista ->
            lista.map { it.toDomain() }
        }

    // El stock ya es un número, no hay nada que convertir.
    override fun obtenerStock(idProducto: Long): Flow<Int> =
        dao.obtenerStock(idProducto)
}
