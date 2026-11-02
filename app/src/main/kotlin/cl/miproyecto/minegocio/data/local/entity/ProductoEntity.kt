package cl.miproyecto.minegocio.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "producto",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["nombre_normalizado"], unique = true),
    ]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_producto")
    val idProducto: Long = 0,

    @ColumnInfo(name = "sku")
    val sku: String?,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "nombre_normalizado")
    val nombreNormalizado: String,

    @ColumnInfo(name = "URL_imagen")
    val urlImagen: String?,

    @ColumnInfo(name = "precio_venta")
    val precioVenta: Int,

    @ColumnInfo(name = "activo")
    val activo: Boolean = true,

    )
