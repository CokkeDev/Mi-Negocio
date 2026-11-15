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
    suspend fun restaurarRespaldo(origen: Uri)
}
