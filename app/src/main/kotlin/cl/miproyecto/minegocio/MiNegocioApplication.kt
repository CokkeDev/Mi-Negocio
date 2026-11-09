package cl.miproyecto.minegocio

import android.app.Application
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.repository.ProductoRepositoryImpl
import cl.miproyecto.minegocio.domain.repository.ProductoRepository

class MiNegocioApplication : Application() {

    // onCreate es lo primero que ejecuta Android al abrir la app.
    // Aquí la Application se guarda a sí misma en "instancia".
    override fun onCreate() {
        super.onCreate()
        instancia = this
    }

    val database: MiNegocioDatabase by lazy {
        MiNegocioDatabase.getInstance(this)
    }

    val productoRepository: ProductoRepository by lazy {
        ProductoRepositoryImpl(database.productoDao())
    }

    companion object {
        // "lateinit": se le dará valor más tarde (en onCreate), no al declararla.
        // "private set": cualquiera puede LEERLA, pero solo esta clase puede cambiarla.
        lateinit var instancia: MiNegocioApplication
            private set
    }
}
