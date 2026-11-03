package cl.miproyecto.minegocio.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.miproyecto.minegocio.data.local.dao.ProductoDao
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity

@Database(
    entities = [ProductoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MiNegocioDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
}