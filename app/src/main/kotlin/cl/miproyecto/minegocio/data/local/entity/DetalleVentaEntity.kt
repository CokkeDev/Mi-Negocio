package cl.miproyecto.minegocio.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ENTIDAD DETALLE_VENTA = TABLA "detalle_venta".
// Cada fila dice: "en la venta X se sacaron N unidades del lote Y a tal precio".
// Si una línea de la boleta necesitó unidades de 2 lotes (FEFO), se guardan 2 filas.
// Gracias a que se guarda el LOTE, se sabe el precio de compra y se calcula la ganancia.
@Entity(
    tableName = "detalle_venta",
    foreignKeys = [
        // Si alguna vez se borrara la venta, se borran sus detalles (CASCADE).
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["id_venta"],
            childColumns = ["id_venta"],
            onDelete = ForeignKey.CASCADE,
        ),
        // No se puede borrar un lote que ya se vendió (RESTRICT).
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["id_lote"],
            childColumns = ["id_lote"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["id_venta"]), Index(value = ["id_lote"])],
)
data class DetalleVentaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_detalle")
    val idDetalle: Long = 0,

    @ColumnInfo(name = "id_venta")
    val idVenta: Long,

    @ColumnInfo(name = "id_lote")
    val idLote: Long,

    @ColumnInfo(name = "cantidad")
    val cantidad: Int,

    // Precio de venta de cada unidad EN ESE MOMENTO.
    // Se guarda aquí porque el precio del producto puede cambiar después.
    @ColumnInfo(name = "precio_unitario")
    val precioUnitario: Int,
)
