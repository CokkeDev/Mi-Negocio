package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.entity.DetalleVentaEntity
import cl.miproyecto.minegocio.data.local.entity.GastoEntity
import cl.miproyecto.minegocio.data.local.entity.PerdidaEntity
import cl.miproyecto.minegocio.data.local.entity.VentaEntity
import kotlinx.coroutines.flow.Flow

// DAOs pequeños: solo guardan o borran. Las sumas para los reportes están en ReporteDao.

// Guardar ventas (la boleta y cada una de sus líneas).
@Dao
interface VentaDao {
    @Insert
    suspend fun insertarVenta(venta: VentaEntity): Long

    @Insert
    suspend fun insertarDetalle(detalle: DetalleVentaEntity): Long
}

// Guardar mermas.
@Dao
interface PerdidaDao {
    @Insert
    suspend fun insertar(perdida: PerdidaEntity): Long
}

// Guardar, borrar y listar gastos.
@Dao
interface GastoDao {
    @Insert
    suspend fun insertar(gasto: GastoEntity): Long

    // Los gastos sí se borran de verdad: no tienen historial que cuidar.
    @Query("DELETE FROM gasto WHERE id_gasto = :idGasto")
    suspend fun eliminar(idGasto: Long): Int

    // Todos los gastos, del más nuevo al más antiguo.
    @Query("SELECT * FROM gasto ORDER BY fecha DESC, id_gasto DESC")
    fun obtenerTodos(): Flow<List<GastoEntity>>
}
