package cl.miproyecto.minegocio.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import cl.miproyecto.minegocio.data.local.relacion.FilaMasVendido
import cl.miproyecto.minegocio.data.local.relacion.MontoPorCategoria
import cl.miproyecto.minegocio.data.local.relacion.MontoPorMes
import cl.miproyecto.minegocio.data.local.relacion.TotalesVentas
import kotlinx.coroutines.flow.Flow

// Consultas que SUMAN datos para los reportes.
// Reciben un rango de fechas: desde (incluido) y hasta (no incluido), en milisegundos.
// Todas son Flow: si se registra una venta, los reportes se actualizan solos.
@Dao
interface ReporteDao {

    // Total vendido y número de boletas.
    @Query(
        """
        SELECT COALESCE(SUM(total), 0) AS ventas, COUNT(*) AS boletas
        FROM venta WHERE fecha >= :desde AND fecha < :hasta
        """
    )
    fun totalesVentas(desde: Long, hasta: Long): Flow<TotalesVentas>

    // Ganancia: por cada detalle, (precio de venta - precio de compra del lote) x cantidad.
    // JOIN une tablas: el detalle con su venta (para la fecha) y con su lote (para el costo).
    @Query(
        """
        SELECT COALESCE(SUM(d.cantidad * (d.precio_unitario - l.precio_compra)), 0)
        FROM detalle_venta d
        JOIN venta v ON v.id_venta = d.id_venta
        JOIN lote l ON l.id_lote = d.id_lote
        WHERE v.fecha >= :desde AND v.fecha < :hasta
        """
    )
    fun gananciaVentas(desde: Long, hasta: Long): Flow<Int>

    // Cuánto costó la merma: unidades perdidas x precio de compra del lote.
    @Query(
        """
        SELECT COALESCE(SUM(p.cantidad * l.precio_compra), 0)
        FROM perdida p JOIN lote l ON l.id_lote = p.id_lote
        WHERE p.fecha >= :desde AND p.fecha < :hasta
        """
    )
    fun costoMerma(desde: Long, hasta: Long): Flow<Int>

    // Lo invertido en compras: costo de todos los lotes INGRESADOS en el rango.
    // precio_compra es por unidad, así que se multiplica por las unidades compradas.
    @Query(
        """
        SELECT COALESCE(SUM(precio_compra * unidades_iniciales), 0)
        FROM lote WHERE fecha_ingreso >= :desde AND fecha_ingreso < :hasta
        """
    )
    fun totalCompras(desde: Long, hasta: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM gasto WHERE fecha >= :desde AND fecha < :hasta")
    fun totalGastos(desde: Long, hasta: Long): Flow<Int>

    // Gastos sumados por categoría (para la barra de colores de la pantalla Gastos).
    @Query(
        """
        SELECT categoria, SUM(monto) AS monto FROM gasto
        WHERE fecha >= :desde AND fecha < :hasta
        GROUP BY categoria ORDER BY monto DESC
        """
    )
    fun gastosPorCategoria(desde: Long, hasta: Long): Flow<List<MontoPorCategoria>>

    // Ranking de productos por unidades vendidas.
    // GROUP BY junta todas las filas del mismo producto en una sola.
    @Query(
        """
        SELECT pr.nombre AS nombre,
               SUM(d.cantidad) AS unidades,
               SUM(d.cantidad * (d.precio_unitario - l.precio_compra)) AS ganancia
        FROM detalle_venta d
        JOIN venta v ON v.id_venta = d.id_venta
        JOIN lote l ON l.id_lote = d.id_lote
        JOIN producto pr ON pr.id_producto = l.id_producto
        WHERE v.fecha >= :desde AND v.fecha < :hasta
        GROUP BY pr.id_producto
        ORDER BY unidades DESC
        LIMIT :limite
        """
    )
    fun masVendidos(desde: Long, hasta: Long, limite: Int): Flow<List<FilaMasVendido>>

    // Unidades vendidas de un producto desde una fecha.
    @Query(
        """
        SELECT COALESCE(SUM(d.cantidad), 0)
        FROM detalle_venta d
        JOIN venta v ON v.id_venta = d.id_venta
        JOIN lote l ON l.id_lote = d.id_lote
        WHERE l.id_producto = :idProducto AND v.fecha >= :desde
        """
    )
    fun unidadesVendidas(idProducto: Long, desde: Long): Flow<Int>

    // --- Por mes, para el gráfico ---
    // strftime('%Y-%m', ...) convierte la fecha a texto "2026-11".
    // fecha / 1000 porque SQLite usa segundos y nosotros guardamos milisegundos.
    // 'localtime' para usar la hora del teléfono y no la de Londres (UTC).

    @Query(
        """
        SELECT strftime('%Y-%m', v.fecha / 1000, 'unixepoch', 'localtime') AS mes,
               SUM(d.cantidad * (d.precio_unitario - l.precio_compra)) AS monto
        FROM detalle_venta d
        JOIN venta v ON v.id_venta = d.id_venta
        JOIN lote l ON l.id_lote = d.id_lote
        WHERE v.fecha >= :desde
        GROUP BY mes
        """
    )
    fun gananciaPorMes(desde: Long): Flow<List<MontoPorMes>>

    @Query(
        """
        SELECT strftime('%Y-%m', fecha / 1000, 'unixepoch', 'localtime') AS mes,
               SUM(monto) AS monto
        FROM gasto WHERE fecha >= :desde GROUP BY mes
        """
    )
    fun gastosPorMes(desde: Long): Flow<List<MontoPorMes>>

    @Query(
        """
        SELECT strftime('%Y-%m', p.fecha / 1000, 'unixepoch', 'localtime') AS mes,
               SUM(p.cantidad * l.precio_compra) AS monto
        FROM perdida p JOIN lote l ON l.id_lote = p.id_lote
        WHERE p.fecha >= :desde GROUP BY mes
        """
    )
    fun mermaPorMes(desde: Long): Flow<List<MontoPorMes>>
}
