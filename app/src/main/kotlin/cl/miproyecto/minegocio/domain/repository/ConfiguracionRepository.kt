package cl.miproyecto.minegocio.domain.repository

import android.net.Uri
import cl.miproyecto.minegocio.domain.model.Configuracion
import kotlinx.coroutines.flow.StateFlow

// Guardar y leer las preferencias del usuario.
interface ConfiguracionRepository {
    // StateFlow: siempre tiene un valor, y avisa cuando cambia.
    val configuracion: StateFlow<Configuracion>

    // "copy" cambia solo lo que se pide. Ej: actualizar { it.copy(sonidoAlEscanear = false) }
    fun actualizar(cambio: (Configuracion) -> Configuracion)
}

// Copiar la base de datos a un archivo y volver a cargarla.
interface RespaldoRepository {
    // Copia la base de datos al archivo elegido por el usuario.
    suspend fun crearRespaldo(destino: Uri)

    // Reemplaza la base de datos por la del archivo. Después hay que reiniciar la app.
    // Si falla ANTES de cerrar la base, lanza IOException y la app sigue normal.
    // Si falla DESPUÉS, lanza RestauracionInterrumpidaException: hay que reiniciar igual.
    suspend fun restaurarRespaldo(origen: Uri)
}

// La base de datos ya se cerró, pero no se pudo poner el respaldo en su lugar.
// Los datos anteriores siguen guardados; al reiniciar, la app los vuelve a abrir.
class RestauracionInterrumpidaException(causa: Throwable) :
    Exception("No se pudo terminar de restaurar el respaldo", causa)
