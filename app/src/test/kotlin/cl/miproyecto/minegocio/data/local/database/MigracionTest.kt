package cl.miproyecto.minegocio.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.data.local.entity.GastoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

// TEST DE MIGRACIÓN
// Simula un teléfono con la app VIEJA (base de datos versión 1 o 2) y datos guardados.
// Después abre esa base de datos con la app ACTUAL (versión 4):
// Room aplica las AutoMigrations y revisa que las tablas queden exactamente como en 3.json.
// Si algo no calza, Room lanza un error y el test falla.
// Los esquemas viejos se leen de app/schemas/.../1.json y 2.json.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MigracionTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val nombreBase = "migracion-test.db"

    @Test
    fun de_version_2_a_3_conserva_productos_y_lotes() = runTest {
        crearBaseVieja(version = 2) {
            execSQL(
                "INSERT INTO producto (id_producto, sku, nombre, nombre_normalizado, URL_imagen, precio_venta, activo) " +
                    "VALUES (1, '7801', 'Leche', 'leche', NULL, 1200, 1)",
            )
            execSQL(
                "INSERT INTO lote (id_lote, id_producto, precio_compra, fecha_ingreso, fecha_vencimiento, " +
                    "unidades_iniciales, unidades_disponibles) VALUES (1, 1, 800, 1000, NULL, 10, 7)",
            )
        }

        val db = abrirConLaAppActual()
        val resumen = db.productoDao().obtenerConStock().first().single()
        assertEquals("Leche", resumen.producto.nombre)
        assertEquals("7801", resumen.producto.sku)
        assertEquals(7, resumen.stock)
        // Las tablas nuevas de la versión 3 existen y funcionan.
        db.gastoDao().insertar(GastoEntity(descripcion = "Luz", monto = 1000, categoria = "SERVICIOS", fecha = 1))
        assertEquals(1, db.gastoDao().obtenerTodos().first().size)
        db.close()
    }

    @Test
    fun de_version_3_a_4_agrega_costo_total_sin_perder_lotes() = runTest {
        crearBaseVieja(version = 3) {
            execSQL(
                "INSERT INTO producto (id_producto, sku, nombre, nombre_normalizado, URL_imagen, precio_venta, activo) " +
                    "VALUES (1, NULL, 'Leche', 'leche', NULL, 1200, 1)",
            )
            execSQL(
                "INSERT INTO lote (id_lote, id_producto, precio_compra, fecha_ingreso, fecha_vencimiento, " +
                    "unidades_iniciales, unidades_disponibles) VALUES (1, 1, 800, 1000, NULL, 10, 7)",
            )
        }
        val db = abrirConLaAppActual()
        val lote = db.loteDao().obtenerPorProductoUnaVez(1).single()
        assertEquals(7, lote.unidadesDisponibles)
        // Un lote antiguo no tiene costo total: queda en -1 y los reportes usan precio x unidades.
        assertEquals(-1, lote.costoTotal)
        db.close()
    }

    @Test
    fun de_version_1_a_3_conserva_productos() = runTest {
        crearBaseVieja(version = 1) {
            execSQL(
                "INSERT INTO producto (id_producto, sku, nombre, nombre_normalizado, URL_imagen, precio_venta, activo) " +
                    "VALUES (1, NULL, 'Pan', 'pan', NULL, 1000, 1)",
            )
        }

        val db = abrirConLaAppActual()
        val resumen = db.productoDao().obtenerConStock().first().single()
        assertEquals("Pan", resumen.producto.nombre)
        assertEquals(0, resumen.stock)
        db.close()
    }

    // Crea el archivo de base de datos tal como lo dejaba la app en esa versión,
    // usando el esquema exportado (las mismas sentencias CREATE que usaba Room).
    private fun crearBaseVieja(version: Int, datos: SQLiteDatabase.() -> Unit) {
        val archivo = contexto.getDatabasePath(nombreBase)
        archivo.parentFile?.mkdirs()
        archivo.delete()
        val esquema = JSONObject(
            File("schemas/${MiNegocioDatabase::class.java.name}/$version.json").readText(),
        ).getJSONObject("database")

        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { sql ->
            val tablas = esquema.getJSONArray("entities")
            for (i in 0 until tablas.length()) {
                val tabla = tablas.getJSONObject(i)
                val nombre = tabla.getString("tableName")
                sql.execSQL(tabla.getString("createSql").replace("\${TABLE_NAME}", nombre))
                val indices = tabla.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", nombre))
                }
            }
            // room_master_table: la "firma" con la que Room reconoce la versión del esquema.
            val inicial = esquema.getJSONArray("setupQueries")
            for (i in 0 until inicial.length()) sql.execSQL(inicial.getString(i))
            sql.datos()
            sql.version = version
        }
    }

    private fun abrirConLaAppActual(): MiNegocioDatabase =
        Room.databaseBuilder(contexto, MiNegocioDatabase::class.java, nombreBase)
            .allowMainThreadQueries()
            .build()
}
