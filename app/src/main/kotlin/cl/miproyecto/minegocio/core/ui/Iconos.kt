package cl.miproyecto.minegocio.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// ÍCONOS DE LA APP
// Son los mismos del diseño: dibujos de "línea" (trazo) en un cuadro de 24 x 24.
// Cada ícono es una lista de "paths" (instrucciones de dibujo, igual que en SVG):
//   M = mover el lápiz, L/l = línea, h/v = línea horizontal/vertical, a = arco, z = cerrar.
// Se usan así: Icon(Iconos.Agregar, contentDescription = "Agregar")

// Arma un ImageVector con trazos (sin relleno). El color lo pone Icon() con "tint".
private fun icono(nombre: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = nombre,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { datos ->
            addPath(
                pathData = addPathNodes(datos),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

// Un círculo escrito como path (SVG <circle> no existe en los paths).
private fun circulo(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

// Un rectángulo con esquinas redondeadas escrito como path.
private fun rectangulo(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

object Iconos {
    val Escanear = icono(
        "escanear",
        "M4 7V5a1 1 0 0 1 1-1h2M17 4h2a1 1 0 0 1 1 1v2M20 17v2a1 1 0 0 1-1 1h-2M7 20H5a1 1 0 0 1-1-1v-2M8 8v8M11 8v8M14 8v8M17 8v8",
    )
    val Configuracion = icono(
        "configuracion",
        circulo(12f, 12f, 3f),
        "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1",
    )
    val Buscar = icono("buscar", circulo(11f, 11f, 7f), "M20 20l-3.5-3.5")
    val Agregar = icono("agregar", "M12 5v14M5 12h14")
    val Quitar = icono("quitar", "M5 12h14")
    val Inicio = icono("inicio", "M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z")
    val Vender = icono("vender", circulo(9f, 20f, 1.5f), circulo(17f, 20f, 1.5f), "M3 4h2l2.5 11h11L21 8H6.2")
    val Reportes = icono("reportes", "M5 20V11M11 20V5M17 20v-6M3 20h18")
    val Gastos = icono("gastos", rectangulo(3f, 6f, 18f, 14f, 2f), "M3 10h18M16 15h2")
    val Volver = icono("volver", "M15 5l-7 7 7 7")
    val Adelante = icono("adelante", "M9 5l7 7-7 7")
    val Cerrar = icono("cerrar", "M6 6l12 12M18 6L6 18")
    val Camara = icono("camara", "M4 8h3l2-3h6l2 3h3v11H4z", circulo(12f, 13f, 3.5f))
    val Lote = icono("lote", "M3 7l9-4 9 4v10l-9 4-9-4z", "M3 7l9 4 9-4M12 11v10")
    val Calendario = icono("calendario", rectangulo(4f, 5f, 16f, 15f, 2f), "M4 10h16M9 3v4M15 3v4")
    val Eliminar = icono("eliminar", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Alerta = icono("alerta", "M12 3l10 18H2z", "M12 10v5M12 18v.5")
    val Listo = icono("listo", "M5 12l5 5 9-10")
    val Linterna = icono("linterna", "M8 3h8l-1 6H9zM9 9v11h6V9M12 13v2")
    val Rayo = icono("rayo", "M13 3L5 13h6l-1 8 8-10h-6z")
    val Camion = icono(
        "camion",
        "M3 16V7h11v9M14 10h4l3 3v3h-7",
        circulo(7f, 17.5f, 1.8f),
        circulo(17f, 17.5f, 1.8f),
    )
    val Bolsa = icono("bolsa", "M6 8h12l-1 12H7zM9 8V6a3 3 0 0 1 6 0v2")
    val Casa = icono("casa", "M4 11l8-7 8 7v8a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z")
    val Lapiz = icono("lapiz", "M4 20h4L19 9l-4-4L4 16z")
    val Descargar = icono("descargar", "M12 4v11M7 10l5 5 5-5M5 20h14")
    val Subir = icono("subir", "M12 20V9M7 14l5-5 5 5M5 4h14")
    val Puntos = icono("puntos", "M6 12h.01M12 12h.01M18 12h.01")
    val Galeria = icono("galeria", rectangulo(3f, 4f, 18f, 16f, 2f), circulo(9f, 10f, 2f), "M21 16l-5-5L5 20")
}
