package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

// DAO = "Data Access Object" (objeto de acceso a datos).
// Aquí se escriben todas las consultas a la tabla "producto":
// buscar, guardar, modificar...
//
// Es una "interface": solo dice QUÉ consultas existen.
// Room escribe solo el código que las ejecuta (ProductoDao_Impl)
// cuando compilas el proyecto.
//
// "suspend" significa que la función tarda un poco (va a la base de datos)
// y se ejecuta sin congelar la pantalla.
@Dao
interface ProductoDao {
    //Consulta para buscar duplicados
    // Busca un producto que tenga el mismo nombre normalizado O el mismo código de barras.
    // Los ":nombre" del SQL se reemplazan por los parámetros de la función con el mismo nombre.
    // "LIMIT 1": basta con encontrar uno.
    // Devuelve el producto encontrado, o null si no hay ninguno (por eso el "?").
    @Query("SELECT * FROM producto WHERE nombre_normalizado = :nombreNormalizado OR sku = :sku LIMIT 1")
    suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): ProductoEntity?

    //Insertar productos a la base de datos
    // @Insert no necesita SQL: Room lo arma solo a partir de la entidad.
    // Devuelve el id (número) que SQLite le dio al producto nuevo.
    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    //Reactivar producto anteriormente insertado, devuelve la cantidad de filas modificadas (1 = ok, 0 = no existe)
    // Vuelve a poner "activo = 1" a un producto que estaba eliminado.
    // Se busca por su id, que es lo único que nunca se repite.
    @Query("UPDATE producto SET activo = 1 WHERE id_producto = :idProducto")
    suspend fun reactivar(idProducto: Long): Int

    // Elimina un producto de forma "lógica": no lo borra, solo lo marca como inactivo (activo = 0).
    // Así se conserva su historial y después se puede reactivar.
    // Devuelve la cantidad de filas modificadas (1 = ok, 0 = no existe).
    @Query("UPDATE producto SET activo = 0 WHERE id_producto = :idProducto")
    suspend fun desactivar(idProducto: Long): Int

    //Obtener productos activos
    // Trae todos los productos que no están eliminados, ordenados por nombre (A → Z).
    // Devuelve un Flow: es como una "suscripción". Cada vez que la tabla cambia
    // (se agrega o reactiva un producto), envía la lista nueva automáticamente,
    // y la pantalla se actualiza sola. Por eso no lleva "suspend".
    @Query ("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre")
    fun obtenerActivos(): Flow<List<ProductoEntity>>

}
