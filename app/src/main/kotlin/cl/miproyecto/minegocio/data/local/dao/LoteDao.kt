package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoteDao {

    // 1) Guardar un lote nuevo
    @Insert
    suspend fun insertar(lote: LoteEntity): Long

    // 2) Los lotes de un producto, en orden FEFO
    @Query(
        """
        SELECT * FROM lote
        WHERE id_producto = :idProducto AND unidades_disponibles > 0
        ORDER BY fecha_vencimiento IS NULL, fecha_vencimiento, fecha_ingreso
        """
    )
    fun obtenerPorProducto(idProducto: Long): Flow<List<LoteEntity>>

    // 3) El stock de un producto
    @Query(
        """
        SELECT COALESCE(SUM(unidades_disponibles), 0)
        FROM lote
        WHERE id_producto = :idProducto
        """
    )
    fun obtenerStock(idProducto: Long): Flow<Int>
}