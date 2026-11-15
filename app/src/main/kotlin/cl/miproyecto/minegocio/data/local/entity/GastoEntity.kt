package cl.miproyecto.minegocio.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ENTIDAD GASTO = TABLA "gasto".
// Lo que el negocio paga y que no es mercadería: luz, arriendo, transporte, bolsas...
// (La mercadería ya se registra como precio de compra en cada Lote).
@Entity(
    tableName = "gasto",
    indices = [Index(value = ["fecha"])],
)
data class GastoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_gasto")
    val idGasto: Long = 0,

    // Ej: "Cuenta de luz".
    @ColumnInfo(name = "descripcion")
    val descripcion: String,

    // Cuánto se pagó, en pesos.
    @ColumnInfo(name = "monto")
    val monto: Int,

    // Nombre de la categoría (ver CategoriaGasto en domain).
    @ColumnInfo(name = "categoria")
    val categoria: String,

    @ColumnInfo(name = "fecha")
    val fecha: Long,
)
