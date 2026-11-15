package cl.miproyecto.minegocio.core.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// MENSAJERO: muestra los mensajes de abajo (Snackbar) desde cualquier parte de la app.
// Sirve, por ejemplo, para que al guardar un lote se vuelva atrás
// y el mensaje "Lote agregado exitosamente" se vea en la pantalla anterior.
data class Mensaje(
    val texto: String,
    // Texto del botón del mensaje (ej: "Ver"). null = sin botón.
    val accion: String? = null,
    val alAccion: (() -> Unit)? = null,
)

object Mensajero {
    // SharedFlow: como un Flow, pero para "eventos" que ocurren una vez.
    // extraBufferCapacity: guarda hasta 4 mensajes si llegan varios seguidos.
    private val _mensajes = MutableSharedFlow<Mensaje>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<Mensaje> = _mensajes.asSharedFlow()

    fun mostrar(texto: String, accion: String? = null, alAccion: (() -> Unit)? = null) {
        _mensajes.tryEmit(Mensaje(texto, accion, alAccion))
    }
}
