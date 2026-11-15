package cl.miproyecto.minegocio.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ENTIDAD PERDIDA = TABLA "perdida" (la merma).
// Cada fila es una vez que se perdieron unidades de un lote:
// se vencieron, se dañaron, se las robaron...
// Se une al LOTE (no al producto) porque así se sabe cuánto costó lo perdido.
@Entity(
    tableName = "perdida",
    foreignKeys = [
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["id_lote"],
            childColumns = ["id_lote"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["id_lote"]), Index(value = ["fecha"])],
)
data class PerdidaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_perdida")
    val idPerdida: Long = 0,

    // De qué lote salieron las unidades perdidas.
    @ColumnInfo(name = "id_lote")
    val idLote: Long,

    // Cuántas unidades se perdieron.
    @ColumnInfo(name = "cantidad")
    val cantidad: Int,

    // Por qué se perdieron: "Vencido", "Dañado", etc.
    @ColumnInfo(name = "motivo")
    val motivo: String,

    // Comentario opcional del usuario.
    @ColumnInfo(name = "nota")
    val nota: String?,

    // Cuándo se registró (milisegundos, igual que en Lote).
    @ColumnInfo(name = "fecha")
    val fecha: Long,
)
