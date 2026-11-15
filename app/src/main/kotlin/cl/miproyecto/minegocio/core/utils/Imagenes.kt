package cl.miproyecto.minegocio.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// FOTOS DE PRODUCTOS
// La foto elegida (galería o cámara) se copia, achicada, a una carpeta privada de la app.
// En la base de datos solo se guarda la RUTA del archivo (urlImagen), no la foto.

private const val LADO_MAXIMO = 800

// Copia la imagen de "origen" a filesDir/imagenes/<nombre-al-azar>.jpg y devuelve la ruta.
suspend fun guardarImagenProducto(context: Context, origen: Uri): String = withContext(Dispatchers.IO) {
    val bitmap = leerAchicada(context, origen)
    val carpeta = File(context.filesDir, "imagenes").apply { mkdirs() }
    val archivo = File(carpeta, "${UUID.randomUUID()}.jpg")
    archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    bitmap.recycle()
    archivo.absolutePath
}

private fun leerAchicada(context: Context, origen: Uri): Bitmap {
    // En Android 9+ ImageDecoder además gira la foto según cómo se sacó.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val fuente = ImageDecoder.createSource(context.contentResolver, origen)
        return ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
            val lado = maxOf(info.size.width, info.size.height)
            if (lado > LADO_MAXIMO) {
                val escala = LADO_MAXIMO.toFloat() / lado
                decoder.setTargetSize((info.size.width * escala).toInt(), (info.size.height * escala).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
    // Android 7 y 8: BitmapFactory con inSampleSize.
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(origen).use { BitmapFactory.decodeStream(it, null, opciones) }
    var escala = 1
    while (maxOf(opciones.outWidth, opciones.outHeight) / (escala * 2) >= LADO_MAXIMO) escala *= 2
    return context.contentResolver.openInputStream(origen).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = escala })
    } ?: error("No se pudo leer la imagen")
}

// Crea un archivo vacío donde la app de cámara guardará la foto, y su Uri para compartirlo.
fun crearUriFotoTemporal(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}
