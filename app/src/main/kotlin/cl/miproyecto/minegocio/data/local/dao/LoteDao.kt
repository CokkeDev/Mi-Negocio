package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow

// Las 00:00 de hoy en la hora del teléfono, en milisegundos (como se guardan las fechas).
private const val HOY = "(strftime('%s', 'now', 'localtime', 'start of day', 'utc') * 1000)"

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

    // 3) El stock de un producto (sin contar lo vencido)
    @Query(
        """
        SELECT COALESCE(SUM(unidades_disponibles), 0)
        FROM lote
        WHERE id_producto = :idProducto
          AND (fecha_vencimiento IS NULL OR fecha_vencimiento >= $HOY)
        """
    )
    fun obtenerStock(idProducto: Long): Flow<Int>

    // 4) Igual que la 2), pero una sola vez (sin Flow).
    // Se usa DENTRO de una transacción de venta para repartir las unidades.
    // Un lote que venció (antes de hoy) NO se vende: sigue en la base para poder
    // registrarlo como merma, pero no cuenta como stock ni sale en la boleta.
    @Query(
        """
        SELECT * FROM lote
        WHERE id_producto = :idProducto AND unidades_disponibles > 0
          AND (fecha_vencimiento IS NULL OR fecha_vencimiento >= $HOY)
        ORDER BY fecha_vencimiento IS NULL, fecha_vencimiento, fecha_ingreso
        """
    )
    suspend fun obtenerPorProductoUnaVez(idProducto: Long): List<LoteEntity>

    // 5) Resta unidades a un lote.
    // El "AND unidades_disponibles >= :cantidad" es una protección:
    // si no alcanzan, no cambia nada y devuelve 0 filas.
    @Query(
        """
        UPDATE lote SET unidades_disponibles = unidades_disponibles - :cantidad
        WHERE id_lote = :idLote AND unidades_disponibles >= :cantidad
        """
    )
    suspend fun descontar(idLote: Long, cantidad: Int): Int
}
