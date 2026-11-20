package cl.miproyecto.minegocio.data.respaldo

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.data.crearProducto
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
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

// Tests de RESTAURAR un respaldo de punta a punta, con la base de datos real en un archivo.
// (Las funciones que no existen en Android antiguo no se detectan aquí, porque los tests
// corren en el Java del computador: eso lo revisa el lint, con ./gradlew lintDebug.)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RespaldoRestaurarTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val archivoBase = contexto.getDatabasePath(MiNegocioDatabase.NOMBRE_ARCHIVO)
    private val preparado = File(archivoBase.path + ".restaurando")
    private lateinit var db: MiNegocioDatabase

    @Before
    fun abrirBase() {
        contexto.deleteDatabase(MiNegocioDatabase.NOMBRE_ARCHIVO)
        db = abrir()
    }

    @After
    fun limpiar() {
        db.close()
        contexto.deleteDatabase(MiNegocioDatabase.NOMBRE_ARCHIVO)
    }

    private fun abrir(): MiNegocioDatabase =
        Room.databaseBuilder(contexto, MiNegocioDatabase::class.java, MiNegocioDatabase.NOMBRE_ARCHIVO)
            .allowMainThreadQueries().build()

    private suspend fun nombres(base: MiNegocioDatabase) =
        base.productoDao().obtenerActivos().first().map { it.nombre }

    @Test
    fun restaura_el_respaldo_y_descarta_lo_que_se_hizo_despues() = runTest {
        db.crearProducto("Pan", 1000)
        val respaldo = File(contexto.cacheDir, "respaldo.db")
        RespaldoRepositoryImpl(contexto, db).crearRespaldo(Uri.fromFile(respaldo))

        // Algo que se registra DESPUÉS del respaldo: debe desaparecer al restaurar.
        db.crearProducto("Leche", 1200)
        RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(respaldo))

        // Igual que al reiniciar la app: se vuelve a abrir el archivo.
        db = abrir()
        assertEquals(listOf("Pan"), nombres(db))
        assertFalse("No debe quedar el archivo preparado", preparado.exists())
    }

    @Test
    fun un_archivo_invalido_no_cierra_ni_toca_la_base_actual() = runTest {
        db.crearProducto("Pan", 1000)
        val falso = File(contexto.cacheDir, "foto.jpg").apply { writeText("esto no es una base de datos") }

        try {
            RespaldoRepositoryImpl(contexto, db).restaurarRespaldo(Uri.fromFile(falso))
            fail("Debió rechazar el archivo")
        } catch (e: IOException) {
            // Esperado.
        }

        // La app puede seguir usando la base sin reiniciarse, con sus datos intactos.
        assertTrue(db.isOpen)
        assertEquals(listOf("Pan"), nombres(db))
        assertFalse("No debe quedar el archivo preparado", preparado.exists())
    }

    @Test
    fun reemplazar_archivo_deja_el_contenido_nuevo_y_no_deja_restos() {
        val destino = File(contexto.cacheDir, "destino.txt").apply { writeText("viejo") }
        val nuevo = File(contexto.cacheDir, "nuevo.txt").apply { writeText("nuevo") }

        reemplazarArchivo(nuevo, destino)

        assertEquals("nuevo", destino.readText())
        assertFalse(nuevo.exists())
    }
}
