package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    //Consulta para buscar duplicados
    @Query("SELECT * FROM producto WHERE nombre_normalizado = :nombreNormalizado OR sku = :sku LIMIT 1")
    suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): ProductoEntity?

    //Insertar productos a la base de datos
    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    //Reactivar producto anteriormente insertado, devuelve la cantidad de filas modificadas (1 = ok, 0 = no existe)
    @Query("UPDATE producto SET activo = 1 WHERE id_producto = :idProducto")
    suspend fun reactivar(idProducto: Long): Int

    //Obtener productos activos
    @Query ("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre")
    fun obtenerActivos(): Flow<List<ProductoEntity>>
}