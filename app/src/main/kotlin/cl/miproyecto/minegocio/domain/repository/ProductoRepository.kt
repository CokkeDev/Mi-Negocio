package cl.miproyecto.minegocio.domain.repository

// Solo se importa el modelo "limpio" y Flow (que es Kotlin puro, no Android).
// Aquí NUNCA se importa nada de androidx ni de la capa data (ProductoEntity, DAO...).
import cl.miproyecto.minegocio.domain.model.Producto
import kotlinx.coroutines.flow.Flow

// REPOSITORIO = el "mesero" de los productos.
// El resto de la app (ViewModel) le pide los datos a él,
// sin saber si vienen de Room, de un archivo o de internet.
//
// Es una "interface": una lista de PROMESAS. Dice QUÉ se puede hacer,
// pero no CÓMO. Por eso las funciones no tienen { } con código.
// El "cómo" lo escribe ProductoRepositoryImpl (en la capa data),
// que usa el DAO y el mapper.
//
// Diferencia con el DAO: aquí se trabaja con Producto, nunca con ProductoEntity.
interface ProductoRepository {

    // Busca un producto con el mismo nombre normalizado o el mismo código de barras.
    // Devuelve el producto encontrado, o null si no hay ninguno.
    suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto?

    // Guarda un producto nuevo y devuelve el id que le asignó la base de datos.
    suspend fun insertar(producto: Producto): Long

    // Vuelve a activar un producto eliminado.
    // Devuelve cuántos productos se modificaron (1 = ok, 0 = no existe).
    suspend fun reactivar(idProducto: Long): Int

    // Entrega la lista de productos activos y avisa sola cada vez que cambia.
    // No lleva "suspend" porque devuelve un Flow.
    fun obtenerActivos(): Flow<List<Producto>>
}
