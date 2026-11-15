package cl.miproyecto.minegocio.data.respaldo

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

// Tests de la revisión que se hace ANTES de restaurar un respaldo.
// Si se aceptara un archivo equivocado, la app quedaría sin poder abrir.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RespaldoValidacionTest {

    private val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun la_version_de_la_validacion_es_la_misma_que_la_de_la_base_de_datos() {
        val db = Room.inMemoryDatabaseBuilder(contexto, MiNegocioDatabase::class.java).build()
        assertEquals(db.openHelper.readableDatabase.version, VERSION_BASE_DE_DATOS)
        db.close()
    }

    @Test
    fun acepta_un_respaldo_real_de_mi_negocio() {
        val nombre = "respaldo-real.db"
        contexto.deleteDatabase(nombre)
        val db = Room.databaseBuilder(contexto, MiNegocioDatabase::class.java, nombre)
            .allowMainThreadQueries().build()
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        db.close()

        assertTrue(esRespaldoValido(contexto.getDatabasePath(nombre)))
    }

    @Test
    fun rechaza_un_archivo_que_no_es_base_de_datos() {
        val archivo = File(contexto.cacheDir, "foto.jpg").apply { writeText("esto no es una base de datos") }
        assertFalse(esRespaldoValido(archivo))
    }

    @Test
    fun rechaza_la_base_de_datos_de_otra_app() {
        val archivo = File(contexto.cacheDir, "otra-app.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use {
            it.execSQL("CREATE TABLE contactos (id INTEGER PRIMARY KEY, nombre TEXT)")
            it.version = 1
        }
        assertFalse(esRespaldoValido(archivo))
    }

    @Test
    fun rechaza_un_respaldo_de_una_version_mas_nueva_de_la_app() {
        val archivo = File(contexto.cacheDir, "futuro.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use {
            it.execSQL("CREATE TABLE producto (id_producto INTEGER PRIMARY KEY)")
            it.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            it.version = VERSION_BASE_DE_DATOS + 1
        }
        assertFalse(esRespaldoValido(archivo))
    }
}
