package cl.miproyecto.minegocio.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lote",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id_producto"],
            childColumns = ["id_producto"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["id_producto"])],
)
data class LoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_lote")
    val idLote: Long = 0,

    @ColumnInfo(name = "id_producto")
    val idProducto: Long,

    @ColumnInfo(name = "precio_compra")
    val precioCompra: Int,

    @ColumnInfo(name = "fecha_ingreso")
    val fechaIngreso: Long,

    @ColumnInfo(name = "fecha_vencimiento")
    val fechaVencimiento: Long?,

    @ColumnInfo(name = "unidades_iniciales")
    val unidadesIniciales: Int,

    @ColumnInfo(name = "unidades_disponibles")
    val unidadesDisponibles: Int,
)