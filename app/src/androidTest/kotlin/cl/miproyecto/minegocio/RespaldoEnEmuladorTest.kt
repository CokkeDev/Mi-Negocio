package cl.miproyecto.minegocio

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.local.entity.LoteEntity
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import cl.miproyecto.minegocio.data.respaldo.RespaldoRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Respaldo y restauración con la base de datos REAL de la app, dentro del emulador.
@RunWith(AndroidJUnit4::class)
class RespaldoEnEmuladorTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    // Cada prueba abre su propia conexión al archivo real: restaurar CIERRA la base que se le
    // entrega, y así una prueba no deja cerrada la de la siguiente.
    private fun abrir() =
        Room.databaseBuilder(ctx, MiNegocioDatabase::class.java, MiNegocioDatabase.NOMBRE_ARCHIVO).build()

    private fun producto(nombre: String) = ProductoEntity(
        sku = null, nombre = nombre, nombreNormalizado = nombre.lowercase(), urlImagen = null, precioVenta = 1000,
    )

    @Test
    fun respaldo_y_restauracion_devuelven_los_datos_del_momento_del_respaldo() = runBlocking {
        val db = abrir()
        db.clearAllTables()
        val id = db.productoDao().insertar(producto("Respaldado"))
        db.loteDao().insertar(
            LoteEntity(idProducto = id, precioCompra = 500, fechaIngreso = 1L, fechaVencimiento = null,
                unidadesIniciales = 10, unidadesDisponibles = 7, costoTotal = 5000),
        )
        val archivo = File(ctx.cacheDir, "respaldo-test.db").apply { delete() }
        val repo = RespaldoRepositoryImpl(ctx, db)

        repo.crearRespaldo(Uri.fromFile(archivo))
        assertTrue("el respaldo debe tener contenido", archivo.length() > 0)

        // Cambios posteriores al respaldo: se deben perder al restaurar.
        db.productoDao().insertar(producto("Posterior"))
        repo.restaurarRespaldo(Uri.fromFile(archivo))

        // La app se reinicia tras restaurar: se abre la base de nuevo desde el archivo.
        val nueva = abrir()
        val nombres = nueva.productoDao().obtenerActivos().first().map { it.nombre }
        assertEquals(listOf("Respaldado"), nombres)
        assertEquals(7, nueva.loteDao().obtenerStock(id).first())
        nueva.close()
    }

    @Test
    fun un_archivo_que_no_es_respaldo_se_rechaza_y_no_se_pierden_los_datos() = runBlocking {
        val db = abrir()
        db.clearAllTables()
        db.productoDao().insertar(producto("Intacto"))
        val basura = File(ctx.cacheDir, "no-es-respaldo.db").apply { writeText("esto no es una base de datos") }
        try {
            RespaldoRepositoryImpl(ctx, db).restaurarRespaldo(Uri.fromFile(basura))
            fail("debía rechazar el archivo")
        } catch (e: Exception) { }
        // La base sigue abierta y con sus datos.
        assertEquals(listOf("Intacto"), db.productoDao().obtenerActivos().first().map { it.nombre })
        db.close()
    }
}
