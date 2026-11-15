package cl.miproyecto.minegocio.data.local.relacion

import androidx.room.ColumnInfo
import androidx.room.Embedded
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity

// Estas clases NO son tablas. Son la "forma" de lo que devuelven
// algunas consultas que juntan datos de varias tablas (JOIN, SUM, COUNT...).
// Room llena cada propiedad buscando la columna con el mismo nombre.

// Un producto + los datos de sus lotes.
data class ProductoConStock(
    // @Embedded: "mete aquí todas las columnas de ProductoEntity".
    @Embedded val producto: ProductoEntity,
    @ColumnInfo(name = "stock") val stock: Int,
    @ColumnInfo(name = "lotes_con_stock") val lotesConStock: Int,
    @ColumnInfo(name = "total_lotes") val totalLotes: Int,
    @ColumnInfo(name = "proximo_vencimiento") val proximoVencimiento: Long?,
)

// Total vendido y cantidad de boletas.
data class TotalesVentas(
    @ColumnInfo(name = "ventas") val ventas: Int,
    @ColumnInfo(name = "boletas") val boletas: Int,
)

// Una fila del ranking de más vendidos.
data class FilaMasVendido(
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "unidades") val unidades: Int,
    @ColumnInfo(name = "ganancia") val ganancia: Int,
)

// Un monto agrupado por mes. "mes" viene como texto "2026-11".
data class MontoPorMes(
    @ColumnInfo(name = "mes") val mes: String,
    @ColumnInfo(name = "monto") val monto: Int,
)

// Un monto agrupado por categoría de gasto.
data class MontoPorCategoria(
    @ColumnInfo(name = "categoria") val categoria: String,
    @ColumnInfo(name = "monto") val monto: Int,
)
