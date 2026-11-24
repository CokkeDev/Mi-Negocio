package cl.miproyecto.minegocio.data.respaldo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.domain.repository.RespaldoRepository
import cl.miproyecto.minegocio.domain.repository.RestauracionInterrumpidaException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// RESPALDO: como todo se guarda solo en el teléfono, si el teléfono se pierde se pierden los datos.
// El respaldo copia el archivo de la base de datos a donde el usuario elija
// (Descargas, Google Drive, un pendrive...) y después se puede volver a cargar.
class RespaldoRepositoryImpl(
    private val context: Context,
    private val database: MiNegocioDatabase,
) : RespaldoRepository {

    // Dispatchers.IO: copiar archivos es lento, se hace fuera de la pantalla.
    override suspend fun crearRespaldo(destino: Uri): Unit = withContext(Dispatchers.IO) {
        // SQLite guarda los últimos cambios en un archivo aparte (-wal).
        // "checkpoint" los pasa al archivo principal, para que la copia quede completa.
        database.openHelper.writableDatabase
            .query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
            .use { it.moveToFirst() }

        val archivo = context.getDatabasePath(MiNegocioDatabase.NOMBRE_ARCHIVO)
        val salida = context.contentResolver.openOutputStream(destino)
            ?: throw IOException("No se pudo abrir el archivo de destino")
        // "use" cierra los archivos al terminar, aunque haya un error.
        salida.use { out -> archivo.inputStream().use { it.copyTo(out) } }
        Unit
    }

    override suspend fun restaurarRespaldo(origen: Uri): Unit = withContext(Dispatchers.IO) {
        val archivo = context.getDatabasePath(MiNegocioDatabase.NOMBRE_ARCHIVO)
        // El respaldo se prepara JUNTO a la base de datos (en la misma carpeta),
        // porque solo así el cambio final puede ser un "renombrar", que es todo o nada.
        val preparado = File(archivo.path + ".restaurando")

        // 1) Se copia el respaldo y se revisa que sea de ESTA app.
        //    Todo lo que puede fallar (sin espacio, archivo dañado) pasa AQUÍ,
        //    con la base de datos actual todavía abierta e intacta.
        try {
            val entrada = context.contentResolver.openInputStream(origen)
                ?: throw IOException("No se pudo abrir el respaldo")
            entrada.use { input -> preparado.outputStream().use { input.copyTo(it) } }
            // Si se cargara otro archivo (de otra app, o dañado), la app no podría volver a abrir.
            if (!esRespaldoValido(preparado)) {
                throw IOException("El archivo no es un respaldo de Mi Negocio")
            }
        } catch (e: Exception) {
            borrarConAuxiliares(preparado)
            throw e
        }

        // 2) Se cierra la base de datos. Desde aquí la app SIEMPRE debe reiniciarse,
        //    aunque algo falle, porque ya no se puede seguir usando la base cerrada.
        database.close()
        try {
            File(archivo.path + "-wal").delete()
            File(archivo.path + "-shm").delete()
            reemplazarArchivo(preparado, archivo)
            // Quedan solo los auxiliares que SQLite creó al revisar el respaldo.
            borrarConAuxiliares(preparado)
        } catch (e: Exception) {
            borrarConAuxiliares(preparado)
            throw RestauracionInterrumpidaException(e)
        }
        // 3) Al reiniciar la app, Room abre el archivo nuevo
        //    (y si es de una versión antigua, aplica las migraciones).
    }
}

// Cambia "destino" por "nuevo" de una sola vez: nunca queda un archivo copiado a medias.
// En Android, renombrar dentro de la misma carpeta reemplaza el archivo de forma atómica.
// (En Windows, donde corren los tests en el computador, renombrar falla si el destino
// existe; por eso se borra y se vuelve a intentar.)
internal fun reemplazarArchivo(nuevo: File, destino: File) {
    if (nuevo.renameTo(destino)) return
    if (destino.delete() && nuevo.renameTo(destino)) return
    throw IOException("No se pudo reemplazar ${destino.name}")
}

// Borra el archivo y los que SQLite crea al lado al abrirlo (-wal, -shm, -journal).
private fun borrarConAuxiliares(archivo: File) {
    for (sufijo in listOf("", "-wal", "-shm", "-journal")) File(archivo.path + sufijo).delete()
}

// Lee hasta "n" bytes. OJO: no se puede llamar "readNBytes", porque Java ya tiene una
// función con ese nombre (solo desde Android 13) y Kotlin usaría esa en vez de esta:
// en Android 7 a 12 la app se cerraría al restaurar.
private fun java.io.InputStream.leerHasta(n: Int): ByteArray {
    val buffer = ByteArray(n)
    var leidos = 0
    while (leidos < n) {
        val r = read(buffer, leidos, n - leidos)
        if (r < 0) break
        leidos += r
    }
    return buffer.copyOf(leidos)
}

// Un respaldo es válido si:
//  1) empieza con "SQLite format 3" (todo archivo SQLite empieza así),
//  2) tiene las tablas de Mi Negocio (producto y la tabla interna de Room),
//  3) su versión no es más nueva que la de esta app (no se puede "bajar" de versión).
internal fun esRespaldoValido(archivo: File): Boolean {
    val cabecera = archivo.inputStream().use { it.leerHasta(15) }
    if (String(cabecera) != "SQLite format 3") return false
    return runCatching {
        SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val tablas = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('producto', 'room_master_table')",
                null,
            ).use { it.count }
            tablas == 2 && db.version in 1..VERSION_BASE_DE_DATOS
        }
    }.getOrDefault(false)
}

// Debe ser igual a "version" en @Database (MiNegocioDatabase).
internal const val VERSION_BASE_DE_DATOS = 4
