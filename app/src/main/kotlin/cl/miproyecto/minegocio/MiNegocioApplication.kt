package cl.miproyecto.minegocio

import android.app.Application
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.repository.ProductoRepositoryImpl
import cl.miproyecto.minegocio.domain.repository.ProductoRepository

class MiNegocioApplication : Application(){

    val database: MiNegocioDatabase by lazy {
        MiNegocioDatabase.getInstance(this)
    }

    val productoRepository: ProductoRepository by lazy {
        ProductoRepositoryImpl(database.productoDao())
    }
}
