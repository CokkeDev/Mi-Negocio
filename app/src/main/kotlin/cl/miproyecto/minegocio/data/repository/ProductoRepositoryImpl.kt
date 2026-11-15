package cl.miproyecto.minegocio.data.repository

import cl.miproyecto.minegocio.data.local.dao.ProductoDao
import cl.miproyecto.minegocio.data.mapper.toDomain
import cl.miproyecto.minegocio.data.mapper.toEntity
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// La "cocina": cumple lo que promete ProductoRepository usando el DAO.
// Su trabajo es pedirle los datos al DAO y convertirlos (Entity <-> modelo).
class ProductoRepositoryImpl(
    private val dao: ProductoDao,
) : ProductoRepository {

    // El DAO devuelve ProductoEntity?; "?.toDomain()" lo convierte solo si no es null.
    override suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto? =
        dao.buscarDuplicado(nombreNormalizado, sku)?.toDomain()

    override suspend fun insertar(producto: Producto): Long =
        dao.insertar(producto.toEntity())

    override suspend fun reactivar(idProducto: Long): Int =
        dao.reactivar(idProducto)

    // "Eliminar" para la app es "desactivar" en la base de datos (activo = 0).
    override suspend fun eliminar(idProducto: Long): Int =
        dao.desactivar(idProducto)

    // Primer map: cada lista nueva del Flow. Segundo map: cada producto de la lista.
    override fun obtenerActivos(): Flow<List<Producto>> =
        dao.obtenerActivos().map { lista -> lista.map { it.toDomain() } }

    override fun obtenerEliminados(): Flow<List<Producto>> =
        dao.obtenerEliminados().map { lista -> lista.map { it.toDomain() } }

    override fun obtenerConStock(): Flow<List<ProductoResumen>> =
        dao.obtenerConStock().map { lista -> lista.map { it.toDomain() } }

    override fun obtenerResumen(idProducto: Long): Flow<ProductoResumen?> =
        dao.obtenerConStockPorId(idProducto).map { it?.toDomain() }

    override suspend fun buscarPorSku(sku: String): ProductoResumen? =
        dao.buscarPorSku(sku)?.toDomain()
}
