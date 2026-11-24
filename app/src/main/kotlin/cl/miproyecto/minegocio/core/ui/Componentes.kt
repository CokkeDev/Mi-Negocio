package cl.miproyecto.minegocio.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.miproyecto.minegocio.core.utils.iniciales
import cl.miproyecto.minegocio.core.utils.separarMiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// COMPONENTES REUTILIZABLES
// Piezas de pantalla que se repiten en varias partes de la app.
// Así cada pantalla queda más corta y todas se ven iguales (como en el diseño).

// ── Barra de arriba con flecha para volver ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    onVolver: (() -> Unit)?,
    subtitulo: String? = null,
    iconoVolver: ImageVector = Iconos.Volver,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver) { Icon(iconoVolver, contentDescription = "Volver") }
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

// TAMAÑO DE LETRA CON TOPE
// Cuando el usuario agranda el texto del teléfono (accesibilidad), las letras crecen en todas
// partes. En lugares de ancho fijo (barra inferior, botones, filtros) eso las rompía:
// "Reporte/s", "Recha", "Sin stock" en vertical. Esta función deja que la letra crezca,
// pero solo hasta "tope" veces el tamaño normal (1,3 = 30 % más grande).
@Composable
fun spConTope(base: Float, tope: Float = 1.3f): TextUnit {
    val escala = LocalDensity.current.fontScale
    return (base * minOf(escala, tope) / escala).sp
}

// ── Las 4 pestañas de abajo: Inicio, Vender, Reportes, Gastos ──
enum class Pestana(val etiqueta: String, val icono: ImageVector) {
    INICIO("Inicio", Iconos.Inicio),
    VENDER("Vender", Iconos.Vender),
    REPORTES("Reportes", Iconos.Reportes),
    GASTOS("Gastos", Iconos.Gastos),
}

@Composable
fun BarraInferior(seleccionada: Pestana, onSeleccionar: (Pestana) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Pestana.entries.forEach { pestana ->
            NavigationBarItem(
                selected = pestana == seleccionada,
                onClick = { if (pestana != seleccionada) onSeleccionar(pestana) },
                icon = { Icon(pestana.icono, contentDescription = null) },
                label = { Text(pestana.etiqueta, fontSize = spConTope(12f), maxLines = 1, softWrap = false) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}

// ── Botones con forma de "píldora" (52 dp de alto, como en el diseño) ──
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        // Menos margen interno (antes 24 dp) para que "Rechazar" quepa con texto grande.
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Box(Modifier.width(8.dp))
        }
        Text(
            texto,
            fontSize = spConTope(15f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icono: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Box(Modifier.width(8.dp))
        }
        Text(
            texto,
            fontSize = spConTope(15f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── La franja de abajo que tienen los formularios (Cancelar / Guardar) ──
// Sube junto con el teclado (ime) y no queda debajo de la barra de gestos del sistema.
@Composable
fun FranjaAcciones(contenido: @Composable RowScope.() -> Unit) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido,
        )
    }
}

// ── Foto del producto, o sus iniciales si no tiene foto ──
@Composable
fun AvatarProducto(
    nombre: String,
    urlImagen: String?,
    modifier: Modifier = Modifier,
    tamano: Dp = 52.dp,
    apagado: Boolean = false,
) {
    val forma = RoundedCornerShape(tamano * 0.23f)
    val imagen = imagenLocal(urlImagen)
    Box(
        modifier = modifier
            .size(tamano)
            .clip(forma)
            .background(
                if (apagado) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamano),
                alpha = if (apagado) 0.5f else 1f,
            )
        } else {
            Text(
                iniciales(nombre),
                fontWeight = FontWeight.Bold,
                fontSize = (tamano.value * 0.3f).sp,
                color = if (apagado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// Lee una imagen guardada en el teléfono SIN congelar la pantalla (produceState + IO).
// Se lee en tamaño reducido (inSampleSize) para no gastar memoria.
@Composable
fun imagenLocal(ruta: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, ruta) {
        value = if (ruta == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val archivo = File(ruta)
                if (!archivo.exists()) return@runCatching null
                val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(ruta, opciones)
                var escala = 1
                while (opciones.outWidth / (escala * 2) >= 300) escala *= 2
                BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = escala })
                    ?.let { b: Bitmap -> b.asImageBitmap() }
            }.getOrNull()
        }
    }
    return bitmap
}

// ── Etiqueta pequeña de color ("Vence 01/12", "Próximo en venderse") ──
@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
    )
}

// ── Campo de texto con la etiqueta ARRIBA, como en el diseño ──
@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    prefijo: String? = null,
    transformacion: VisualTransformation = VisualTransformation.None,
    unaLinea: Boolean = true,
    final: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            etiqueta,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it) } },
            prefix = prefijo?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            isError = error != null,
            singleLine = unaLinea,
            minLines = if (unaLinea) 1 else 2,
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            visualTransformation = transformacion,
            trailingIcon = final,
            shape = RoundedCornerShape(8.dp),
        )
        val textoAbajo = error ?: ayuda
        if (textoAbajo != null) {
            Text(
                textoAbajo,
                fontSize = 12.sp,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Deja solo los números (máximo 9 cifras) de lo que escribe el usuario.
fun soloDigitos(texto: String, maximo: Int = 9): String = texto.filter { it.isDigit() }.take(maximo)

// Muestra "1190" como "1.190" mientras se escribe, sin cambiar el valor guardado.
// OffsetMapping le dice al cursor dónde quedar cuando aparecen los puntos.
object MilesTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty() || original.any { !it.isDigit() } || original.length > 9) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val n = original.length
        val formateado = separarMiles(original.toInt())
        // Ajuste para ceros a la izquierda ("007"): si no calza, no se transforma.
        if (formateado.count { it.isDigit() } != n) return TransformedText(text, OffsetMapping.Identity)
        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset + (1..minOf(offset, n - 1)).count { (n - it) % 3 == 0 }

            override fun transformedToOriginal(offset: Int): Int =
                (0..n).last { originalToTransformed(it) <= offset }
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}

// ── Contador con botones − y + (unidades, cantidad perdida) ──
// El número del centro también se puede escribir con el teclado.
@Composable
fun Contador(
    valor: Int,
    onCambio: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Int = 1,
    maximo: Int = 99_999,
    etiquetaMenos: String = "Restar una unidad",
    etiquetaMas: String = "Sumar una unidad",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = { onCambio((valor - 1).coerceAtLeast(minimo)) },
            enabled = valor > minimo,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) { Icon(Iconos.Quitar, contentDescription = etiquetaMenos) }

        BasicTextField(
            value = valor.toString(),
            onValueChange = { texto ->
                val numero = soloDigitos(texto, 5).toIntOrNull() ?: minimo
                onCambio(numero.coerceIn(minimo, maximo))
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )

        FilledIconButton(
            onClick = { onCambio((valor + 1).coerceAtMost(maximo)) },
            enabled = valor < maximo,
            modifier = Modifier.size(44.dp),
        ) { Icon(Iconos.Agregar, contentDescription = etiquetaMas) }
    }
}

// ── Fila "etiqueta ........ valor" (resúmenes) ──
@Composable
fun FilaDato(
    etiqueta: String,
    valor: String,
    colorValor: Color = MaterialTheme.colorScheme.onSurface,
    destacado: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            etiqueta,
            fontSize = 14.sp,
            fontWeight = if (destacado) FontWeight.Medium else FontWeight.Normal,
            color = if (destacado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valor, fontSize = 14.sp, fontWeight = if (destacado) FontWeight.Medium else FontWeight.Normal, color = colorValor)
    }
}

// ── Estado vacío (cuando no hay nada que mostrar) ──
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        accion?.invoke()
    }
}
