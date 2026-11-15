package cl.miproyecto.minegocio.data.preferencias

import android.content.Context
import androidx.core.content.edit
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.Tema
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Guarda la configuración en SharedPreferences:
// un archivo pequeño de "clave = valor" dentro del teléfono. Ideal para ajustes.
class ConfiguracionRepositoryImpl(context: Context) : ConfiguracionRepository {

    private val prefs = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)

    // Al crear la clase se lee lo guardado; si no hay nada, se usan los valores por defecto.
    private val _configuracion = MutableStateFlow(leer())
    override val configuracion: StateFlow<Configuracion> = _configuracion.asStateFlow()

    override fun actualizar(cambio: (Configuracion) -> Configuracion) {
        val nueva = cambio(_configuracion.value)
        guardar(nueva)
        _configuracion.value = nueva
    }

    private fun leer(): Configuracion {
        val porDefecto = Configuracion()
        return Configuracion(
            nombreNegocio = prefs.getString(NOMBRE, null) ?: porDefecto.nombreNegocio,
            diasAvisoVencimiento = prefs.getInt(DIAS_AVISO, porDefecto.diasAvisoVencimiento),
            stockBajo = prefs.getInt(STOCK_BAJO, porDefecto.stockBajo),
            sonidoAlEscanear = prefs.getBoolean(SONIDO, porDefecto.sonidoAlEscanear),
            vibrarAlAgregar = prefs.getBoolean(VIBRAR, porDefecto.vibrarAlAgregar),
            tema = runCatching { Tema.valueOf(prefs.getString(TEMA, null) ?: "") }
                .getOrDefault(porDefecto.tema),
            ultimoRespaldo = prefs.getLong(ULTIMO_RESPALDO, 0L).takeIf { it > 0 },
        )
    }

    // "edit { }" abre el archivo, escribe y lo guarda.
    private fun guardar(c: Configuracion) {
        prefs.edit {
            putString(NOMBRE, c.nombreNegocio)
            putInt(DIAS_AVISO, c.diasAvisoVencimiento)
            putInt(STOCK_BAJO, c.stockBajo)
            putBoolean(SONIDO, c.sonidoAlEscanear)
            putBoolean(VIBRAR, c.vibrarAlAgregar)
            putString(TEMA, c.tema.name)
            putLong(ULTIMO_RESPALDO, c.ultimoRespaldo ?: 0L)
        }
    }

    // Los nombres de cada clave. Se escriben una sola vez para no equivocarse.
    private companion object {
        const val NOMBRE = "nombre_negocio"
        const val DIAS_AVISO = "dias_aviso_vencimiento"
        const val STOCK_BAJO = "stock_bajo"
        const val SONIDO = "sonido_escanear"
        const val VIBRAR = "vibrar_agregar"
        const val TEMA = "tema"
        const val ULTIMO_RESPALDO = "ultimo_respaldo"
    }
}
