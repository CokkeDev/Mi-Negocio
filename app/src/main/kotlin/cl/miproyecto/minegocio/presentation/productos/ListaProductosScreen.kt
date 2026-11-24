package cl.miproyecto.minegocio.presentation.productos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.AvatarProducto
import cl.miproyecto.minegocio.core.ui.BarraInferior
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.Etiqueta
import cl.miproyecto.minegocio.core.ui.EstadoVacio
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.ui.Pestana
import cl.miproyecto.minegocio.core.utils.diasHasta
import cl.miproyecto.minegocio.core.utils.fechaLarga
import cl.miproyecto.minegocio.core.utils.formatoDiaMes
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.domain.model.Configuracion
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import cl.miproyecto.minegocio.ui.theme.extra

// PANTALLA DE INICIO: resumen del día + lista de productos con su stock.
@Composable
fun ListaProductosScreen(
    onAgregarClick: () -> Unit,
    onProductoClick: (Long) -> Unit,
    onIngresarLote: (Long) -> Unit,
    onEscanear: () -> Unit,
    onConfiguracion: () -> Unit,
    onPestana: (Pestana) -> Unit,
    viewModel: ListaProductosViewModel = viewModel(factory = ListaProductosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { BarraInferior(Pestana.INICIO, onPestana) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAgregarClick,
                icon = { Icon(Iconos.Agregar, contentDescription = null) },
                text = { Text("Agregar producto") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Encabezado: fecha, nombre del negocio, escanear y configuración ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fechaLarga(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(estado.configuracion.nombreNegocio, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                    }
                    FilledTonalIconButton(onClick = onEscanear, modifier = Modifier.size(48.dp)) {
                        Icon(Iconos.Escanear, contentDescription = "Escanear para vender")
                    }
                    IconButton(onClick = onConfiguracion, modifier = Modifier.size(48.dp)) {
                        Icon(Iconos.Configuracion, contentDescription = "Configuración")
                    }
                }
            }
            // ── Las 2 tarjetas del día ──
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    TarjetaDia(
                        titulo = "Ventas de hoy",
                        valor = formatoPesos(estado.hoy.ventas),
                        detalle = if (estado.hoy.boletas == 1) "1 boleta" else "${estado.hoy.boletas} boletas",
                        destacada = true,
                        modifier = Modifier.weight(1f),
                    )
                    TarjetaDia(
                        titulo = "Ganancia de hoy",
                        valor = formatoPesos(estado.hoy.ventasMenosCompras),
                        detalle = "ventas − costo de lotes",
                        destacada = false,
                        negativo = estado.hoy.ventasMenosCompras < 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // ── Buscador ──
            item {
                Buscador(estado.busqueda, viewModel::buscar, Modifier.padding(top = 6.dp))
            }
            // ── Filtros ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Filtro("Todos · ${estado.totalProductos}", estado.filtro == FiltroProductos.TODOS) {
                        viewModel.elegirFiltro(FiltroProductos.TODOS)
                    }
                    Filtro("Por vencer · ${estado.porVencer}", estado.filtro == FiltroProductos.POR_VENCER) {
                        viewModel.elegirFiltro(FiltroProductos.POR_VENCER)
                    }
                    Filtro("Sin stock · ${estado.sinStock}", estado.filtro == FiltroProductos.SIN_STOCK) {
                        viewModel.elegirFiltro(FiltroProductos.SIN_STOCK)
                    }
                }
            }

            when {
                estado.cargando -> Unit
                // No hay ningún producto todavía.
                estado.totalProductos == 0 -> item {
                    EstadoVacio(
                        icono = Iconos.Lote,
                        titulo = "Aún no hay productos",
                        mensaje = "Agrega lo que vendes en tu kiosko para empezar a llevar el stock.",
                        modifier = Modifier.fillMaxWidth(),
                        accion = { BotonPrincipal("Agregar producto", onAgregarClick, icono = Iconos.Agregar) },
                    )
                }
                // Hay productos, pero ninguno coincide con la búsqueda o el filtro.
                estado.productos.isEmpty() -> item {
                    Text(
                        "No hay productos que coincidan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                else -> items(estado.productos, key = { it.producto.idProducto }) { resumen ->
                    FilaProducto(
                        resumen = resumen,
                        config = estado.configuracion,
                        onClick = {
                            // Un producto que nunca ha tenido lotes va directo a ingresar uno (como en el diseño).
                            if (resumen.totalLotes == 0) onIngresarLote(resumen.producto.idProducto)
                            else onProductoClick(resumen.producto.idProducto)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaDia(
    titulo: String,
    valor: String,
    detalle: String,
    destacada: Boolean,
    modifier: Modifier,
    // Si el valor es negativo se muestra en rojo (no en verde).
    negativo: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (destacada) colores.primary else colores.surfaceContainerLowest)
            .then(if (destacada) Modifier else Modifier.border(1.dp, colores.surfaceVariant, RoundedCornerShape(16.dp)))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val texto = if (destacada) colores.onPrimary else colores.onSurfaceVariant
        Text(titulo, fontSize = 12.sp, color = texto)
        Text(
            valor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                destacada -> colores.onPrimary
                negativo -> colores.error
                else -> MaterialTheme.extra.exito
            },
        )
        Text(detalle, fontSize = 12.sp, color = texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Buscador(texto: String, onCambio: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Iconos.Buscar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f)) {
            if (texto.isEmpty()) {
                Text("Buscar producto o código", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = texto,
                onValueChange = onCambio,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (texto.isNotEmpty()) {
            IconButton(onClick = { onCambio("") }, modifier = Modifier.size(32.dp)) {
                Icon(Iconos.Cerrar, contentDescription = "Borrar búsqueda", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Filtro(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = if (seleccionado) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    )
}

// Una tarjeta de producto. Si no tiene stock se ve en gris con borde punteado.
@Composable
private fun FilaProducto(resumen: ProductoResumen, config: Configuracion, onClick: () -> Unit) {
    val producto = resumen.producto
    val apagado = resumen.stock == 0
    val colores = MaterialTheme.colorScheme
    val colorBorde = colores.outlineVariant
    val forma = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(if (apagado) colores.surfaceContainer else colores.surfaceContainerLowest)
            .then(
                if (apagado) Modifier.drawBehind {
                    // Borde punteado: PathEffect.dashPathEffect dibuja rayas de 8 px con espacios de 6 px.
                    drawRoundRect(
                        color = colorBorde,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                    )
                } else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarProducto(producto.nombre, producto.urlImagen, apagado = apagado)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                producto.nombre,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (apagado) colores.onSurfaceVariant else colores.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitulo(resumen),
                fontSize = 13.sp,
                color = colores.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val stockBajo = !apagado && resumen.stock < config.stockBajo
            Text(
                "${resumen.stock} u.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    apagado -> colores.onSurfaceVariant
                    stockBajo -> colores.error
                    else -> colores.onSurface
                },
            )
            EtiquetaVencimiento(resumen, config)
        }
    }
}

// "$2.500 · 2 lotes", "$700 · sin vencimiento", "Sin lotes · toca para ingresar uno"...
private fun subtitulo(resumen: ProductoResumen): String {
    val precio = formatoPesos(resumen.producto.precioVenta)
    return when {
        resumen.totalLotes == 0 -> "Sin lotes · toca para ingresar uno"
        resumen.stock == 0 -> "$precio · sin stock"
        resumen.proximoVencimiento == null -> "$precio · sin vencimiento"
        resumen.lotesConStock == 1 -> "$precio · 1 lote"
        else -> "$precio · ${resumen.lotesConStock} lotes"
    }
}

// Etiqueta de color según cuánto falta para que venza el lote más próximo.
@Composable
fun EtiquetaVencimiento(resumen: ProductoResumen, config: Configuracion) {
    val vence = resumen.proximoVencimiento ?: return
    if (resumen.stock == 0) return
    val dias = diasHasta(vence)
    val colores = MaterialTheme.colorScheme
    when {
        dias < 0 -> Etiqueta("Vencido", colores.errorContainer, colores.onErrorContainer)
        dias == 0 -> Etiqueta("Vence hoy", colores.errorContainer, colores.onErrorContainer)
        dias == 1 -> Etiqueta("Vence mañana", colores.errorContainer, colores.onErrorContainer)
        dias <= config.diasAvisoVencimiento ->
            Etiqueta("Vence ${formatoDiaMes(vence)}", MaterialTheme.extra.avisoContenedor, MaterialTheme.extra.onAvisoContenedor)
    }
}
