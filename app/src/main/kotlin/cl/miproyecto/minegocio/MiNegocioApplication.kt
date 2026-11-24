package cl.miproyecto.minegocio

import android.app.Application
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import cl.miproyecto.minegocio.core.utils.limpiarImagenesHuerfanas
import cl.miproyecto.minegocio.core.utils.carpetaImagenes
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.preferencias.ConfiguracionRepositoryImpl
import cl.miproyecto.minegocio.data.repository.GastoRepositoryImpl
import cl.miproyecto.minegocio.data.repository.LoteRepositoryImpl
import cl.miproyecto.minegocio.data.repository.MermaRepositoryImpl
import cl.miproyecto.minegocio.data.repository.ProductoRepositoryImpl
import cl.miproyecto.minegocio.data.repository.ReporteRepositoryImpl
import cl.miproyecto.minegocio.data.repository.VentaRepositoryImpl
import cl.miproyecto.minegocio.data.respaldo.RespaldoRepositoryImpl
import cl.miproyecto.minegocio.domain.repository.ConfiguracionRepository
import cl.miproyecto.minegocio.domain.repository.GastoRepository
import cl.miproyecto.minegocio.domain.repository.LoteRepository
import cl.miproyecto.minegocio.domain.repository.MermaRepository
import cl.miproyecto.minegocio.domain.repository.ProductoRepository
import cl.miproyecto.minegocio.domain.repository.ReporteRepository
import cl.miproyecto.minegocio.domain.repository.RespaldoRepository
import cl.miproyecto.minegocio.domain.repository.VentaRepository

// La "caja de herramientas" de la app: aquí se crean UNA VEZ la base de datos
// y todos los repositorios, y los ViewModels los piden desde aquí.
// (Esto se llama "inyección de dependencias manual").
class MiNegocioApplication : Application() {

    // onCreate es lo primero que ejecuta Android al abrir la app.
    // Aquí la Application se guarda a sí misma en "instancia".
    override fun onCreate() {
        super.onCreate()
        instancia = this
        limpiarFotosSinUso()
    }

    // Borra las fotos de productos que ya nadie usa. Corre en segundo plano al abrir la app.
    // Solo se tocan fotos de más de 1 día: una foto recién elegida de un formulario que quedó
    // abierto (por ejemplo, si el sistema cerró la app) no se borra.
    private fun limpiarFotosSinUso() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val enUso = database.productoDao().rutasImagenes().toSet()
                val hace1Dia = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                limpiarImagenesHuerfanas(carpetaImagenes(this@MiNegocioApplication), enUso, hace1Dia)
            }
        }
    }

    // "by lazy": se crea la primera vez que alguien lo pide, no antes.
    val database: MiNegocioDatabase by lazy {
        MiNegocioDatabase.getInstance(this)
    }

    // El tipo es la INTERFAZ (el "menú"), no el Impl (la "cocina").
    val productoRepository: ProductoRepository by lazy {
        ProductoRepositoryImpl(database.productoDao())
    }

    val loteRepository: LoteRepository by lazy {
        LoteRepositoryImpl(database.loteDao())
    }

    val ventaRepository: VentaRepository by lazy {
        VentaRepositoryImpl(database)
    }

    val mermaRepository: MermaRepository by lazy {
        MermaRepositoryImpl(database)
    }

    val gastoRepository: GastoRepository by lazy {
        GastoRepositoryImpl(database.gastoDao())
    }

    val reporteRepository: ReporteRepository by lazy {
        ReporteRepositoryImpl(database.reporteDao())
    }

    val configuracionRepository: ConfiguracionRepository by lazy {
        ConfiguracionRepositoryImpl(this)
    }

    val respaldoRepository: RespaldoRepository by lazy {
        RespaldoRepositoryImpl(this, database)
    }

    companion object {
        // "lateinit": se le dará valor más tarde (en onCreate), no al declararla.
        // "private set": cualquiera puede LEERLA, pero solo esta clase puede cambiarla.
        lateinit var instancia: MiNegocioApplication
            private set
    }
}
