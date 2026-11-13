package cl.miproyecto.minegocio.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.miproyecto.minegocio.data.local.dao.ProductoDao
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import androidx.room.Room
import android.content.Context
import androidx.room.AutoMigration
import cl.miproyecto.minegocio.data.local.dao.LoteDao
import cl.miproyecto.minegocio.data.local.entity.LoteEntity

// Esta clase ES la base de datos de la app.
// Une las tablas (entidades) con sus consultas (DAO).
@Database(
    // Lista de tablas que tiene la base de datos.
    // Cuando crees Lote, Venta, etc., se agregan aquí separadas por comas.
    entities = [ProductoEntity::class, LoteEntity::class],
    // Versión de la estructura de la base de datos.
    // Si cambias una tabla (por ejemplo, agregas una columna), hay que subirla a 2, 3...
    version = 2,
    // Guarda un archivo con la forma de las tablas en app/schemas.
    // Sirve para poder actualizar la base de datos sin perder los datos del usuario.
    exportSchema = true,
    // MIGRACIONES: cómo pasar de una versión a otra SIN perder los datos del usuario.
    // AutoMigration le dice a Room: "escribe tú el cambio de la versión 1 a la 2".
    // Room compara app/schemas/.../1.json con la estructura nueva y genera
    // solo el "CREATE TABLE lote". Los productos guardados no se tocan.
    // Por eso es importante exportSchema = true y subir los .json a git.
    // NUNCA usar fallbackToDestructiveMigration: borraría todos los datos.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
// "abstract" significa que la clase no está completa:
// Room la completa sola (MiNegocioDatabase_Impl) al compilar.
abstract class MiNegocioDatabase : RoomDatabase() {
    // Entrega el DAO de productos. El resto de la app lo usa así:
    // database.productoDao().insertar(...)
    abstract fun productoDao(): ProductoDao

    // Entrega el DAO de lotes.
    abstract fun loteDao(): LoteDao


    // "companion object": lo que está aquí adentro se usa directo con el nombre de la clase,
    // sin crear nada antes: MiNegocioDatabase.getInstance(context)
    companion object {
        // Aquí se guarda la ÚNICA base de datos de la app (patrón "singleton").
        // Empieza en null porque al abrir la app todavía no se ha creado.
        // "private": solo esta clase puede cambiarla.
        // "@Volatile": si una parte de la app la crea, todas las demás lo ven al instante.
        @Volatile
        private var INSTANCE: MiNegocioDatabase? = null

        // Entrega la base de datos. Si no existe, la crea; si ya existe, devuelve la misma.
        // Así toda la app usa una sola base de datos y no se abren varias.
        fun getInstance(context: Context): MiNegocioDatabase {
            // "?:" significa "si es null, haz lo de la derecha".
            // Si INSTANCE ya existe, se devuelve de inmediato.
            // "synchronized" funciona como una puerta con llave:
            // si dos partes de la app piden la base de datos al mismo tiempo,
            // pasa una sola a la vez, y así no se crean dos.
            return INSTANCE ?: synchronized(this){
                // Se vuelve a revisar adentro, porque mientras alguien esperaba
                // en la "puerta", otro pudo haberla creado ya.
                INSTANCE ?: Room.databaseBuilder(
                    // applicationContext: el contexto de toda la app (no de una pantalla),
                    // porque la base de datos dura mientras la app esté abierta.
                    // MiNegocioDatabase::class.java: le dice a Room qué base de datos construir.
                    context.applicationContext, MiNegocioDatabase::class.java,
                    // Nombre del archivo en el celular. No cambiarlo nunca,
                    // o la app crearía una base de datos nueva y vacía.
                    "mi_negocio.db",
                    // build() crea la base de datos y also { } la guarda en INSTANCE
                    // para entregarla las próximas veces.
                ).build().also { INSTANCE = it }

            }
        }
    }
}
