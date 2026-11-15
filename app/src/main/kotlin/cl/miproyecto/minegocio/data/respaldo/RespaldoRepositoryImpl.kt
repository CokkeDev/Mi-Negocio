package cl.miproyecto.minegocio.data.respaldo

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.domain.repository.RespaldoRepository
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
        // 1) Se copia primero a un archivo temporal y se revisa que sea una base de datos SQLite.
        val temporal = File(context.cacheDir, "respaldo_restaurar.db")
        val entrada = context.contentResolver.openInputStream(origen)
            ?: throw IOException("No se pudo abrir el respaldo")
        entrada.use { input -> temporal.outputStream().use { input.copyTo(it) } }

        // Todo archivo SQLite empieza con el texto "SQLite format 3".
        val cabecera = temporal.inputStream().use { it.readNBytes(15) }
        if (String(cabecera) != "SQLite format 3") {
            temporal.delete()
            throw IOException("El archivo no es un respaldo de Mi Negocio")
        }

        // 2) Se cierra la base de datos actual y se reemplaza el archivo.
        database.close()
        val archivo = context.getDatabasePath(MiNegocioDatabase.NOMBRE_ARCHIVO)
        File(archivo.path + "-wal").delete()
        File(archivo.path + "-shm").delete()
        temporal.copyTo(archivo, overwrite = true)
        temporal.delete()
        // 3) Al reiniciar la app, Room abre el archivo nuevo
        //    (y si es de una versión antigua, aplica las migraciones).
    }
}

// readNBytes no existe en Android antiguo; esta versión simple lee hasta "n" bytes.
private fun java.io.InputStream.readNBytes(n: Int): ByteArray {
    val buffer = ByteArray(n)
    var leidos = 0
    while (leidos < n) {
        val r = read(buffer, leidos, n - leidos)
        if (r < 0) break
        leidos += r
    }
    return buffer.copyOf(leidos)
}
