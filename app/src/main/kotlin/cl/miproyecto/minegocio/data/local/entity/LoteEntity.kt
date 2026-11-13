package cl.miproyecto.minegocio.data.local.entity

// Herramientas de Room para describir la tabla.
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ENTIDAD LOTE = TABLA "lote"
// Un lote es CADA COMPRA de un producto. Ejemplo:
//   Coca-Cola 3L -> Lote 1: 12 unidades a $1.800, vencen el 01/03
//                -> Lote 2: 24 unidades a $1.750, vencen el 15/04
// El stock de un producto es la suma de las unidades disponibles de todos sus lotes.
@Entity(
    // Nombre de la tabla dentro de SQLite.
    tableName = "lote",

    // CLAVE FORÁNEA (Foreign Key): une cada lote con su producto.
    // Es el "Producto tiene muchos Lotes" del diagrama.
    // SQLite NO deja guardar un lote cuyo producto no exista (evita "lotes huérfanos").
    foreignKeys = [
        ForeignKey(
            // La tabla "padre": producto.
            entity = ProductoEntity::class,
            // La columna del padre a la que se apunta.
            parentColumns = ["id_producto"],
            // La columna de ESTA tabla que guarda a qué producto pertenece el lote.
            childColumns = ["id_producto"],
            // RESTRICT: no deja borrar un producto que tenga lotes.
            // (Nosotros nunca borramos, usamos activo = 0, pero así la base de datos
            // protege los datos si alguien se equivoca).
            onDelete = ForeignKey.RESTRICT,
        ),
    ],

    // Índice en id_producto: hace rápido buscar "los lotes de este producto",
    // que se usará siempre para calcular el stock.
    // No es "unique" porque un producto puede tener muchos lotes.
    indices = [Index(value = ["id_producto"])],
)
data class LoteEntity(
    // Número que identifica a cada lote (1, 2, 3...). SQLite lo pone solo.
    // Empieza en 0 para que SQLite sepa que es un lote nuevo.
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_lote")
    val idLote: Long = 0,

    // A qué producto pertenece este lote (es la clave foránea).
    @ColumnInfo(name = "id_producto")
    val idProducto: Long,

    // Lo que costó cada unidad al comprarla, en pesos.
    // Sirve para calcular la ganancia: precio de venta - precio de compra.
    @ColumnInfo(name = "precio_compra")
    val precioCompra: Int,

    // Día en que se ingresó el lote.
    // SQLite no tiene tipo "fecha", así que se guarda como un número Long:
    // los milisegundos desde el 1 de enero de 1970 (System.currentTimeMillis()).
    // Así ordenar por fecha es tan simple como ordenar números.
    @ColumnInfo(name = "fecha_ingreso")
    val fechaIngreso: Long,

    // Día en que vence el lote, guardado igual que fechaIngreso.
    // Lleva "?" porque es opcional: hay productos que no vencen (ej: un encendedor).
    // Se usará para vender primero lo que vence primero (FEFO).
    @ColumnInfo(name = "fecha_vencimiento")
    val fechaVencimiento: Long?,

    // Cuántas unidades se compraron. Este número NUNCA cambia.
    @ColumnInfo(name = "unidades_iniciales")
    val unidadesIniciales: Int,

    // Cuántas unidades quedan. Al comprar es igual a unidadesIniciales,
    // y baja con cada venta o merma.  Ej: compré 12, vendí 3 -> quedan 9.
    @ColumnInfo(name = "unidades_disponibles")
    val unidadesDisponibles: Int,
)
