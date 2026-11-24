package cl.miproyecto.minegocio.presentation.productos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.AvatarProducto
import cl.miproyecto.minegocio.core.ui.BarraSuperior
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.BotonSecundario
import cl.miproyecto.minegocio.core.ui.Etiqueta
import cl.miproyecto.minegocio.core.ui.EstadoVacio
import cl.miproyecto.minegocio.core.ui.FranjaAcciones
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.utils.diasHasta
import cl.miproyecto.minegocio.core.utils.formatoDiaMes
import cl.miproyecto.minegocio.core.utils.formatoFecha
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.Lote
import cl.miproyecto.minegocio.ui.theme.extra

// DETALLE DE PRODUCTO: datos, números del mes y sus lotes en orden FEFO.
@Composable
fun DetalleProductoScreen(
    idProducto: Long,
    onVolver: () -> Unit,
    onIngresarLote: () -> Unit,
    onMerma: () -> Unit,
    viewModel: DetalleProductoViewModel = viewModel(factory = DetalleProductoViewModel.factory(idProducto)),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmarEliminar by remember { mutableStateOf(false) }
    val resumen = estado.resumen

    Scaffold(
        topBar = {
            BarraSuperior(titulo = "", onVolver = onVolver) {
                if (resumen != null) {
                    IconButton(onClick = { confirmarEliminar = true }) {
                        Icon(Iconos.Eliminar, contentDescription = "Eliminar producto", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        bottomBar = {
            if (resumen != null) {
                FranjaAcciones {
                    BotonSecundario(
                        "Merma",
                        onClick = onMerma,
                        icono = Iconos.Alerta,
                        modifier = Modifier.weight(1f),
                    )
                    BotonPrincipal(
                        "Ingresar lote",
                        onClick = onIngresarLote,
                        icono = Iconos.Agregar,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        },
    ) { padding ->
        if (resumen == null) {
            if (!estado.cargando) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Este producto no existe.")
                }
            }
            return@Scaffold
        }
        val producto = resumen.producto
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Foto, nombre, código y precio ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AvatarProducto(producto.nombre, producto.urlImagen, tamano = 88.dp, apagado = resumen.stock == 0)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(producto.nombre, fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp)
                        Text(
                            producto.sku?.let { "SKU $it" } ?: "Sin código de barras",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatoPesos(producto.precioVenta),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            // ── 3 números ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dato("Stock", "${resumen.stock} u.", Modifier.weight(1f))
                    Dato(
                        "Ganancia/u.",
                        estado.gananciaPorUnidad?.let { formatoPesos(it) } ?: "—",
                        Modifier.weight(1f),
                        color = if ((estado.gananciaPorUnidad ?: 0) < 0) MaterialTheme.colorScheme.error else MaterialTheme.extra.exito,
                    )
                    Dato("Vendidas mes", estado.vendidasMes.toString(), Modifier.weight(1f))
                }
            }
            // ── Lotes ──
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Lotes con unidades", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (estado.lotes.size > 1) {
                        Text(
                            "Se vende primero el que vence antes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (estado.lotes.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Iconos.Lote,
                        titulo = if (resumen.totalLotes == 0) "Aún no tiene lotes" else "Sin stock",
                        mensaje = "Ingresa un lote con el precio de compra, las unidades y el vencimiento.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            itemsIndexed(estado.lotes, key = { _, lote -> lote.idLote }) { indice, lote ->
                TarjetaLote(lote, esProximo = indice == 0 && estado.lotes.size > 1, config = estado.configuracion)
            }
        }
    }

    if (confirmarEliminar && resumen != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            icon = { Icon(Iconos.Eliminar, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar producto") },
            text = {
                Text(
                    "¿Seguro que quieres eliminar “${resumen.producto.nombre}”? Dejará de aparecer, " +
                        "pero sus ventas se conservan en los reportes.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    viewModel.eliminar(onVolver)
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun TarjetaLote(lote: Lote, esProximo: Boolean, config: Configuracion) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(colores.surfaceContainerLowest)
            .then(if (esProximo) Modifier.border(2.dp, colores.primary, forma) else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lote #${lote.idLote}", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (esProximo) Etiqueta("Próximo en venderse", colores.primary, colores.onPrimary)
        }
        // Barra: cuánto queda del lote.
        LinearProgressIndicator(
            progress = { lote.unidadesDisponibles.toFloat() / lote.unidadesIniciales.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
            color = if (esProximo) colores.primary else colores.secondary,
            trackColor = colores.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colores.onSurface)) {
                        append(lote.unidadesDisponibles.toString())
                    }
                    append(" de ${lote.unidadesIniciales} u.")
                },
                fontSize = 13.sp,
                color = colores.onSurfaceVariant,
            )
            Text("Compra ${formatoPesos(lote.precioCompra)}", fontSize = 13.sp, color = colores.onSurfaceVariant)
            val vence = lote.fechaVencimiento
            if (vence == null) {
                Text("No vence", fontSize = 13.sp, color = colores.onSurfaceVariant)
            } else {
                val dias = diasHasta(vence)
                Text(
                    if (dias < 0) "Venció ${formatoDiaMes(vence)}" else "Vence ${formatoDiaMes(vence)}",
                    fontSize = 13.sp,
                    fontWeight = if (dias <= config.diasAvisoVencimiento) FontWeight.Medium else FontWeight.Normal,
                    color = when {
                        dias <= 1 -> colores.error
                        dias <= config.diasAvisoVencimiento -> MaterialTheme.extra.onAvisoContenedor
                        else -> colores.onSurfaceVariant
                    },
                )
            }
        }
        Text("Ingresado el ${formatoFecha(lote.fechaIngreso)}", fontSize = 12.sp, color = colores.onSurfaceVariant)
    }
}
