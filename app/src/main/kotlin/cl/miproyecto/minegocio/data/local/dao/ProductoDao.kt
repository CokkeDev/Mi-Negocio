package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import cl.miproyecto.minegocio.data.local.relacion.ProductoConStock
import kotlinx.coroutines.flow.Flow

// Parte común de las consultas que traen un producto con los datos de sus lotes.
// El stock NO cuenta los lotes vencidos (no se pueden vender).
// Cada "(SELECT ...)" es una subconsulta que calcula un dato por cada producto "p".
// "const val": un texto fijo que se puede usar dentro de @Query.
private const val SELECT_CON_STOCK = """
    SELECT p.*,
        COALESCE((SELECT SUM(l.unidades_disponibles) FROM lote l
                  WHERE l.id_producto = p.id_producto
                    AND (l.fecha_vencimiento IS NULL
                         OR l.fecha_vencimiento >=
                            (strftime('%s', 'now', 'localtime', 'start of day', 'utc') * 1000))), 0) AS stock,
        (SELECT COUNT(*) FROM lote l
         WHERE l.id_producto = p.id_producto AND l.unidades_disponibles > 0) AS lotes_con_stock,
        (SELECT COUNT(*) FROM lote l WHERE l.id_producto = p.id_producto) AS total_lotes,
        (SELECT MIN(l.fecha_vencimiento) FROM lote l
         WHERE l.id_producto = p.id_producto AND l.unidades_disponibles > 0) AS proximo_vencimiento
    FROM producto p
"""

// DAO = "Data Access Object" (objeto de acceso a datos).
// Aquí se escriben todas las consultas a la tabla "producto":
// buscar, guardar, modificar...
//
// Es una "interface": solo dice QUÉ consultas existen.
// Room escribe solo el código que las ejecuta (ProductoDao_Impl)
// cuando compilas el proyecto.
//
// "suspend" significa que la función tarda un poco (va a la base de datos)
// y se ejecuta sin congelar la pantalla.
@Dao
interface ProductoDao {
    //Consulta para buscar duplicados
    // Busca un producto que tenga el mismo nombre normalizado O el mismo código de barras.
    // Los ":nombre" del SQL se reemplazan por los parámetros de la función con el mismo nombre.
    // "LIMIT 1": basta con encontrar uno.
    // Devuelve el producto encontrado, o null si no hay ninguno (por eso el "?").
    @Query("SELECT * FROM producto WHERE nombre_normalizado = :nombreNormalizado OR sku = :sku LIMIT 1")
    suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): ProductoEntity?

    // Las rutas de las fotos que usan los productos (activos o eliminados).
    // Sirve para saber qué fotos de la carpeta de la app ya no las usa nadie.
    @Query("SELECT URL_imagen FROM producto WHERE URL_imagen IS NOT NULL")
    suspend fun rutasImagenes(): List<String>

    //Insertar productos a la base de datos
    // @Insert no necesita SQL: Room lo arma solo a partir de la entidad.
    // Devuelve el id (número) que SQLite le dio al producto nuevo.
    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    //Reactivar producto anteriormente insertado, devuelve la cantidad de filas modificadas (1 = ok, 0 = no existe)
    // Vuelve a poner "activo = 1" a un producto que estaba eliminado.
    // Se busca por su id, que es lo único que nunca se repite.
    @Query("UPDATE producto SET activo = 1 WHERE id_producto = :idProducto")
    suspend fun reactivar(idProducto: Long): Int

    // Elimina un producto de forma "lógica": no lo borra, solo lo marca como inactivo (activo = 0).
    // Así se conserva su historial y después se puede reactivar.
    // Devuelve la cantidad de filas modificadas (1 = ok, 0 = no existe).
    @Query("UPDATE producto SET activo = 0 WHERE id_producto = :idProducto")
    suspend fun desactivar(idProducto: Long): Int

    //Obtener productos activos
    // Trae todos los productos que no están eliminados, ordenados por nombre (A → Z).
    // Devuelve un Flow: es como una "suscripción". Cada vez que la tabla cambia
    // (se agrega o reactiva un producto), envía la lista nueva automáticamente,
    // y la pantalla se actualiza sola. Por eso no lleva "suspend".
    @Query ("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre")
    fun obtenerActivos(): Flow<List<ProductoEntity>>

    // Los productos eliminados (activo = 0), para poder reactivarlos desde Configuración.
    @Query("SELECT * FROM producto WHERE activo = 0 ORDER BY nombre COLLATE NOCASE")
    fun obtenerEliminados(): Flow<List<ProductoEntity>>

    // Productos activos con su stock. "COLLATE NOCASE": ordena sin importar mayúsculas.
    @Query("$SELECT_CON_STOCK WHERE p.activo = 1 ORDER BY p.nombre COLLATE NOCASE")
    fun obtenerConStock(): Flow<List<ProductoConStock>>

    // Un producto ACTIVO con su stock (para la pantalla de detalle).
    // Si fue eliminado devuelve null: no se puede abrir su detalle, ingresar lotes ni registrar merma.
    @Query("$SELECT_CON_STOCK WHERE p.id_producto = :idProducto AND p.activo = 1")
    fun obtenerConStockPorId(idProducto: Long): Flow<ProductoConStock?>

    // Busca un producto activo por su código de barras (para vender escaneando).
    @Query("$SELECT_CON_STOCK WHERE p.activo = 1 AND p.sku = :sku LIMIT 1")
    suspend fun buscarPorSku(sku: String): ProductoConStock?
}
