package cl.miproyecto.minegocio.data.repository

// Este archivo es el PUENTE entre las capas, por eso importa cosas de "data" y de "domain".
import cl.miproyecto.minegocio.data.local.dao.ProductoDao
import cl.miproyecto.minegocio.data.mapper.toDomain
import cl.miproyecto.minegocio.data.mapper.toEntity
import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// IMPLEMENTACIÓN DEL REPOSITORIO = el que CUMPLE las promesas de ProductoRepository.
//
// Funciona como intermediario:
//   - Recibe un Producto (del ViewModel), lo traduce con toEntity() y se lo pasa al DAO.
//   - Recibe un ProductoEntity (del DAO), lo traduce con toDomain() y lo devuelve.
//
// "(private val dao: ProductoDao)": para crear esta clase hay que ENTREGARLE un DAO
// (se llama "inyección de dependencias"). La clase no crea el DAO, se lo pasan.
//
// ": ProductoRepository": esta clase cumple la interface.
// Kotlin obliga a escribir sus 4 funciones, cada una con "override" adelante.
class ProductoRepositoryImpl(
    private val dao: ProductoDao,
) : ProductoRepository {

    // Le pregunta al DAO si existe un producto repetido.
    // El DAO devuelve un ProductoEntity? (o null si no encontró nada).
    // "?." -> si NO es null, lo traduce a Producto con toDomain().
    //         si es null, devuelve null directamente.
    override suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto? =
        dao.buscarDuplicado(nombreNormalizado, sku)?.toDomain()

    // Recibe un Producto, pero el DAO solo entiende ProductoEntity,
    // así que primero se traduce con toEntity().
    // El DAO devuelve el id nuevo (Long), que se entrega tal cual.
    override suspend fun insertar(producto: Producto): Long =
        dao.insertar(producto.toEntity())

    // Aquí no hay productos que traducir: entra un id (Long) y sale un número (Int).
    // Por eso se llama al DAO directamente, sin usar el mapper.
    override suspend fun reactivar(idProducto: Long): Int =
        dao.reactivar(idProducto)

    // El DAO entrega un Flow<List<ProductoEntity>> y hay que devolver un Flow<List<Producto>>.
    // Son dos "cajas", una dentro de otra, por eso hay dos "map":
    //   1) .map { lista -> ... }    -> cada vez que el Flow envía una lista nueva, se procesa.
    //   2) lista.map { it.toDomain() } -> recorre la lista y traduce cada producto.
    //      "it" es el nombre automático de cada elemento, uno por uno.
    override fun obtenerActivos(): Flow<List<Producto>> =
        dao.obtenerActivos().map { lista -> lista.map { it.toDomain() } }
}
