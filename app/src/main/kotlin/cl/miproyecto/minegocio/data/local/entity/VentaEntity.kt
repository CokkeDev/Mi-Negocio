package cl.miproyecto.minegocio.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ENTIDAD VENTA = TABLA "venta" (una boleta).
// Solo guarda la fecha y el total. Lo que se vendió va en "detalle_venta".
@Entity(
    tableName = "venta",
    // Índice en fecha: los reportes buscan siempre "ventas entre tal y tal día".
    indices = [Index(value = ["fecha"])],
)
data class VentaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_venta")
    val idVenta: Long = 0,

    // Cuándo se hizo la venta (milisegundos).
    @ColumnInfo(name = "fecha")
    val fecha: Long,

    // Total cobrado, en pesos.
    @ColumnInfo(name = "total")
    val total: Int,
)
