package cl.miproyecto.minegocio.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImagenesTest {

    @get:Rule
    val carpetaTemporal = TemporaryFolder()

    private val hace2Dias = System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000
    private val hace1Dia = System.currentTimeMillis() - 24L * 60 * 60 * 1000

    private fun foto(carpeta: File, nombre: String, modificada: Long): File =
        File(carpeta, nombre).apply { writeText("x"); setLastModified(modificada) }

    @Test
    fun borra_solo_las_fotos_sin_uso_y_antiguas() {
        val carpeta = carpetaTemporal.newFolder("imagenes")
        val enUso = foto(carpeta, "en-uso.jpg", hace2Dias)
        val huerfanaVieja = foto(carpeta, "huerfana-vieja.jpg", hace2Dias)
        val huerfanaNueva = foto(carpeta, "huerfana-nueva.jpg", System.currentTimeMillis())

        val borradas = limpiarImagenesHuerfanas(carpeta, setOf(enUso.path), hace1Dia)

        assertEquals(1, borradas)
        assertTrue(enUso.exists())
        assertFalse(huerfanaVieja.exists())
        assertTrue("una foto recién elegida no se toca", huerfanaNueva.exists())
    }

    @Test
    fun una_carpeta_que_no_existe_no_da_error() {
        assertEquals(0, limpiarImagenesHuerfanas(File(carpetaTemporal.root, "no-existe"), emptySet(), hace1Dia))
    }

    @Test
    fun borrar_imagen_solo_borra_archivos_de_la_carpeta_imagenes() {
        val imagenes = carpetaTemporal.newFolder("imagenes")
        val dentro = foto(imagenes, "a.jpg", hace2Dias)
        val otraCarpeta = carpetaTemporal.newFolder("documentos")
        val fuera = foto(otraCarpeta, "importante.jpg", hace2Dias)

        borrarImagenProducto(dentro.path)
        borrarImagenProducto(fuera.path)
        borrarImagenProducto(null)

        assertFalse(dentro.exists())
        assertTrue("no debe borrar archivos fuera de la carpeta de fotos", fuera.exists())
    }
}
