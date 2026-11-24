package cl.miproyecto.minegocio.data.respaldo

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.core.utils.carpetaImagenes
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// El respaldo ahora es un .zip con la base de datos y las fotos de los productos.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RespaldoConFotosTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: MiNegocioDatabase

    @Before
    fun preparar() {
        contexto.deleteDatabase(MiNegocioDatabase.NOMBRE_ARCHIVO)
        carpetaImagenes(contexto).deleteRecursively()
        db = abrir()
    }

    @After
    fun limpiar() {
        db.close()
        contexto.deleteDatabase(MiNegocioDatabase.NOMBRE_ARCHIVO)
        carpetaImagenes(contexto).deleteRecursively()
    }

    private fun abrir() =
        Room.databaseBuilder(contexto, MiNegocioDatabase::class.java, MiNegocioDatabase.NOMBRE_ARCHIVO)
            .allowMainThreadQueries().build()

    private fun producto(nombre: String, imagen: String?) = ProductoEntity(
        sku = null, nombre = nombre, nombreNormalizado = nombre.lowercase(), urlImagen = imagen, precioVenta = 1000,
    )

    private fun foto(nombre: String, contenido: String): File =
        File(carpetaImagenes(contexto).apply { mkdirs() }, nombre).apply { writeText(contenido) }

    @Test
    fun el_respaldo_incluye_las_fotos_y_al_restaurar_vuelven_con_la_ruta_de_este_telefono() = runTest {
        val f = foto("a1.jpg", "contenido-de-la-foto")
        // La ruta se guardó en OTRO teléfono (otro usuario/carpeta): al restaurar debe corregirse.
        db.productoDao().insertar(producto("Con foto", "/data/user/999/otra.app/files/imagenes/a1.jpg"))
        val respaldo = File(contexto.cacheDir, "respaldo.zip")
        RespaldoRepositoryImpl(contexto, db).crearRespaldo(Uri.fromFile(respaldo))

        // Se pierden las fotos (como en un teléfono nuevo) y se restaura.
        f.delete()
        assertFalse(f.exists())
        RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(respaldo))

        assertEquals("contenido-de-la-foto", File(carpetaImagenes(contexto), "a1.jpg").readText())
        db = abrir()
        val ruta = db.productoDao().obtenerActivos().first().single().urlImagen
        assertEquals(File(carpetaImagenes(contexto), "a1.jpg").path, ruta)
        assertFalse(File(contexto.filesDir, "imagenes.restaurando").exists())
    }

    @Test
    fun al_restaurar_las_fotos_actuales_se_reemplazan_por_las_del_respaldo() = runTest {
        foto("vieja.jpg", "vieja")
        db.productoDao().insertar(producto("Uno", File(carpetaImagenes(contexto), "vieja.jpg").path))
        val respaldo = File(contexto.cacheDir, "respaldo.zip")
        RespaldoRepositoryImpl(contexto, db).crearRespaldo(Uri.fromFile(respaldo))

        foto("nueva-despues.jpg", "no debe quedar")
        RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(respaldo))

        val nombres = carpetaImagenes(contexto).list()!!.toSet()
        assertEquals(setOf("vieja.jpg"), nombres)
    }

    @Test
    fun un_respaldo_antiguo_sin_zip_se_sigue_pudiendo_restaurar_y_no_toca_las_fotos() = runTest {
        db.productoDao().insertar(producto("Antiguo", null))
        // Respaldo "de antes": el archivo es directamente la base de datos.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val antiguo = File(contexto.cacheDir, "respaldo-antiguo.db")
        contexto.getDatabasePath(MiNegocioDatabase.NOMBRE_ARCHIVO).copyTo(antiguo, overwrite = true)

        db.productoDao().insertar(producto("Posterior", null))
        foto("existente.jpg", "sigue aquí")
        RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(antiguo))

        db = abrir()
        assertEquals(listOf("Antiguo"), db.productoDao().obtenerActivos().first().map { it.nombre })
        assertTrue(File(carpetaImagenes(contexto), "existente.jpg").exists())
    }

    @Test
    fun un_zip_no_puede_escribir_fuera_de_la_carpeta_de_fotos() = runTest {
        db.productoDao().insertar(producto("Pan", null))
        val respaldo = File(contexto.cacheDir, "respaldo.zip")
        RespaldoRepositoryImpl(contexto, db).crearRespaldo(Uri.fromFile(respaldo))
        // Se le agrega una entrada con "../" a un zip válido.
        val malicioso = File(contexto.cacheDir, "malicioso.zip")
        ZipOutputStream(malicioso.outputStream()).use { out ->
            java.util.zip.ZipInputStream(respaldo.inputStream()).use { zin ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    out.putNextEntry(ZipEntry(e.name)); zin.copyTo(out); out.closeEntry()
                }
            }
            out.putNextEntry(ZipEntry("imagenes/../../escapado.txt")); out.write("x".toByteArray()); out.closeEntry()
        }

        RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(malicioso))

        assertFalse(File(contexto.filesDir.parentFile, "escapado.txt").exists())
        assertFalse(File(contexto.filesDir, "escapado.txt").exists())
        // Solo se usa el nombre del archivo: si se guardó, quedó DENTRO de la carpeta de fotos.
        assertTrue(File(carpetaImagenes(contexto), "escapado.txt").exists())
    }

    @Test
    fun un_zip_sin_base_de_datos_se_rechaza_y_no_cierra_la_base_actual() = runTest {
        db.productoDao().insertar(producto("Pan", null))
        val sinBase = File(contexto.cacheDir, "sin-base.zip")
        ZipOutputStream(sinBase.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("imagenes/x.jpg")); out.write("x".toByteArray()); out.closeEntry()
        }
        try {
            RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(sinBase))
            fail("Debió rechazar el zip")
        } catch (e: IOException) {
            // Esperado.
        }
        assertTrue(db.isOpen)
        assertEquals(listOf("Pan"), db.productoDao().obtenerActivos().first().map { it.nombre })
        assertFalse(File(contexto.filesDir, "imagenes.restaurando").exists())
    }
}
