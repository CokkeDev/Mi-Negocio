package cl.miproyecto.minegocio.domain.repository

import cl.miproyecto.minegocio.domain.model.Producto
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import kotlinx.coroutines.flow.Flow

// Lo que la app puede hacer con los productos (el "menú").
// Solo usa modelos de domain, nunca entidades de Room.
interface ProductoRepository {

    // Busca un producto con el mismo nombre normalizado o el mismo código de barras.
    suspend fun buscarDuplicado(nombreNormalizado: String, sku: String?): Producto?

    // Guarda un producto nuevo y devuelve su id.
    suspend fun insertar(producto: Producto): Long

    // Vuelve a activar un producto eliminado. Devuelve las filas cambiadas.
    suspend fun reactivar(idProducto: Long): Int

    // Elimina (lógicamente) un producto. Devuelve las filas cambiadas.
    suspend fun eliminar(idProducto: Long): Int

    // Los productos activos.
    fun obtenerActivos(): Flow<List<Producto>>

    // Los productos eliminados (para reactivarlos).
    fun obtenerEliminados(): Flow<List<Producto>>

    // Los productos activos con su stock, lotes y vencimiento más cercano.
    fun obtenerConStock(): Flow<List<ProductoResumen>>

    // Un producto con su stock.
    fun obtenerResumen(idProducto: Long): Flow<ProductoResumen?>

    // Busca un producto activo por código de barras.
    suspend fun buscarPorSku(sku: String): ProductoResumen?
}
