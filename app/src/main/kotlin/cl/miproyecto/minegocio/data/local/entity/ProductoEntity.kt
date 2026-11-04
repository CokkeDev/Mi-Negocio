package cl.miproyecto.minegocio.data.local.entity

// Los "import" traen herramientas de otras librerías para poder usarlas aquí.
// Todas estas vienen de Room, la librería que maneja la base de datos.
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ENTIDAD = TABLA
// Esta clase representa la tabla "producto" de la base de datos.
// Cada propiedad (val) de la clase es una columna de la tabla,
// y cada producto que guardes será una fila.
@Entity(
    // Nombre que tendrá la tabla dentro de SQLite.
    tableName = "producto",

    // Índices: hacen que las búsquedas sean más rápidas.
    // "unique = true" además impide que se repita el valor:
    // la base de datos no deja guardar dos productos iguales.
    indices = [
        // No pueden existir dos productos con el mismo código de barras.
        Index(value = ["sku"], unique = true),
        // No pueden existir dos productos con el mismo nombre
        // (ej: "Coca-Cola" y "coca cola" se consideran iguales).
        Index(value = ["nombre_normalizado"], unique = true),
    ]
)
// "data class" es una clase pensada para guardar datos.
// Kotlin le agrega funciones útiles solo, por ejemplo copy(),
// que sirve para crear una copia cambiando algún dato:
// producto.copy(activo = true)
data class ProductoEntity(
    // Clave primaria: el número que identifica a cada producto (1, 2, 3...).
    // "autoGenerate = true" hace que SQLite ponga el número solo.
    @PrimaryKey(autoGenerate = true)
    // @ColumnInfo dice cómo se llama la columna dentro de la base de datos.
    @ColumnInfo(name = "id_producto")
    // Empieza en 0 para que, al guardar un producto nuevo,
    // SQLite sepa que tiene que generarle un número.
    val idProducto: Long = 0,

    // Código de barras del producto.
    // El "?" significa que puede quedar vacío (null),
    // porque no todos los productos tienen código de barras.
    @ColumnInfo(name = "sku")
    val sku: String?,

    // Nombre del producto tal como lo escribe el usuario (ej: "Coca-Cola 3L").
    // Sin "?": es obligatorio, nunca puede quedar vacío.
    @ColumnInfo(name = "nombre")
    val nombre: String,

    // El mismo nombre pero "limpio": en minúsculas, sin tildes ni símbolos
    // (ej: "coca cola 3l"). Sirve para detectar productos repetidos.
    @ColumnInfo(name = "nombre_normalizado")
    val nombreNormalizado: String,

    // Dirección de la foto del producto dentro del celular.
    // Lleva "?" porque el producto puede no tener foto.
    @ColumnInfo(name = "URL_imagen")
    val urlImagen: String?,

    // Precio al que se vende el producto, en pesos.
    // Es Int (número entero) porque los pesos chilenos no usan decimales.
    @ColumnInfo(name = "precio_venta")
    val precioVenta: Int,

    // Indica si el producto está activo (true) o eliminado (false).
    // Los productos no se borran de verdad, solo se marcan como inactivos,
    // así no se pierde el historial de ventas.
    // En la base de datos se guarda como 1 (true) o 0 (false).
    // "= true": un producto nuevo empieza activo.
    @ColumnInfo(name = "activo")
    val activo: Boolean = true,

    )
