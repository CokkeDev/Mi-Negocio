package cl.miproyecto.minegocio.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.miproyecto.minegocio.data.local.database.MiNegocioDatabase
import cl.miproyecto.minegocio.data.local.entity.LoteEntity
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity

// AYUDANTES PARA LOS TESTS DE BASE DE DATOS
// Se usa una base de datos EN MEMORIA: se crea vacía en cada test y desaparece al cerrarla,
// así un test nunca afecta a otro ni toca los datos reales.

const val DIA = 24L * 60 * 60 * 1000

fun crearBaseEnMemoria(): MiNegocioDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        MiNegocioDatabase::class.java,
    ).allowMainThreadQueries().build()

// Crea un producto y devuelve su id.
suspend fun MiNegocioDatabase.crearProducto(
    nombre: String,
    precioVenta: Int,
    sku: String? = null,
): Long = productoDao().insertar(
    ProductoEntity(
        sku = sku,
        nombre = nombre,
        nombreNormalizado = nombre.lowercase(),
        urlImagen = null,
        precioVenta = precioVenta,
    ),
)

// Crea un lote y devuelve su id. vence = null significa "no vence".
suspend fun MiNegocioDatabase.crearLote(
    idProducto: Long,
    precioCompra: Int,
    unidades: Int,
    vence: Long? = null,
    ingreso: Long = System.currentTimeMillis(),
): Long = loteDao().insertar(
    LoteEntity(
        idProducto = idProducto,
        precioCompra = precioCompra,
        fechaIngreso = ingreso,
        fechaVencimiento = vence,
        unidadesIniciales = unidades,
        unidadesDisponibles = unidades,
    ),
)

// Unidades que le quedan a un lote.
suspend fun MiNegocioDatabase.disponibles(idProducto: Long, idLote: Long): Int =
    loteDao().obtenerPorProductoUnaVez(idProducto).firstOrNull { it.idLote == idLote }?.unidadesDisponibles ?: 0
