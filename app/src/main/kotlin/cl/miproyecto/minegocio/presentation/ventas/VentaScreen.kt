package cl.miproyecto.minegocio.presentation.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.AvatarProducto
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.BotonSecundario
import cl.miproyecto.minegocio.core.ui.EscanerCodigo
import cl.miproyecto.minegocio.core.ui.EstadoVacio
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.ui.MarcoEscaner
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.ui.avisarEscaneo
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.core.utils.normalizarNombre
import cl.miproyecto.minegocio.domain.model.LineaVenta
import cl.miproyecto.minegocio.domain.model.ProductoResumen
import kotlinx.coroutines.delay

// VENTA: la cámara arriba lee los códigos y abajo se arma la boleta.
@Composable
fun VentaScreen(
    onCerrar: () -> Unit,
    onRegistrarProducto: (String) -> Unit,
    onVerReportes: () -> Unit,
    viewModel: VentaViewModel = viewModel(factory = VentaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val resultado by viewModel.resultado.collectAsStateWithLifecycle()
    val config by viewModel.configuracion.collectAsStateWithLifecycle()
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    val vista = LocalView.current

    var linterna by remember { mutableStateOf(false) }
    var elegirProducto by remember { mutableStateOf(false) }
    var confirmarDescartar by remember { mutableStateOf(false) }

    val salir = { if (estado.lineas.isEmpty()) onCerrar() else confirmarDescartar = true }

    // El resultado de "Aceptar venta".
    LaunchedEffect(resultado) {
        when (val r = resultado) {
            is ResultadoVenta.Exito -> {
                viewModel.resultadoMostrado()
                Mensajero.mostrar("Venta realizada · ${formatoPesos(r.total)}", "Ver") { onVerReportes() }
                onCerrar()
            }
            ResultadoVenta.Fallo -> {
                viewModel.resultadoMostrado()
                Mensajero.mostrar("No se pudo registrar la venta. No se descontó stock.")
            }
            null -> Unit
        }
    }
    // Los avisos sobre la cámara se esconden solos después de 2 segundos.
    LaunchedEffect(estado.aviso) {
        if (estado.aviso != null && estado.aviso !is AvisoVenta.CodigoNoRegistrado) {
            delay(2_000)
            viewModel.cerrarAviso()
        }
    }

    // En horizontal la cámara va a la izquierda y la boleta a la derecha: con la cámara
    // arriba (280 dp) no quedaba espacio para ver la boleta ni los botones.
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val camaraContenido: @Composable BoxScope.() -> Unit = {
            var hayCamara by remember { mutableStateOf(true) }
            EscanerCodigo(
                onPermisoCambio = { hayCamara = it },
                onCodigo = { codigo ->
                    viewModel.escanearSku(codigo) {
                        avisarEscaneo(vista, config.sonidoAlEscanear, config.vibrarAlAgregar)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                linternaEncendida = linterna,
            )
            MarcoEscaner(
                mostrarVisor = hayCamara,
                titulo = "Nueva venta",
                onCerrar = salir,
                linterna = linterna,
                onLinterna = { linterna = !linterna },
                modifier = Modifier.fillMaxSize(),
            )
            // Aviso flotante ("Galletas agregadas +1").
            val aviso = estado.aviso
            if (aviso != null && aviso !is AvisoVenta.CodigoNoRegistrado) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 18.dp)
                        .padding(horizontal = 16.dp),
                ) { AvisoFlotante(aviso) }
            }
    }

    val boleta: @Composable (Modifier) -> Unit = { modificador ->
            Column(
                modificador.padding(start = 16.dp, end = 16.dp, top = if (horizontal) 12.dp else 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (estado.lineas.isEmpty()) "Boleta" else "Boleta · ${if (estado.unidades == 1) "1 unidad" else "${estado.unidades} unidades"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { elegirProducto = true }) {
                        Icon(Iconos.Agregar, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(" Sin código", fontSize = 14.sp)
                    }
                }
                if (estado.lineas.isEmpty()) {
                    EstadoVacio(
                        icono = Iconos.Escanear,
                        titulo = "Escanea un producto",
                        mensaje = "O toca “Sin código” para elegirlo de la lista.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        items(estado.lineas, key = { it.idProducto }) { linea ->
                            FilaBoleta(
                                linea = linea,
                                onMenos = { viewModel.restar(linea.idProducto) },
                                onMas = { viewModel.sumar(linea.idProducto) },
                            )
                            if (linea != estado.lineas.last()) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        }
                    }
                }
            }
    }

    val totales: @Composable () -> Unit = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Total", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(formatoPesos(estado.total), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonSecundario("Rechazar", salir, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                    BotonPrincipal(
                        if (estado.registrando) "Registrando…" else "Aceptar venta",
                        onClick = viewModel::confirmarVenta,
                        habilitado = estado.lineas.isNotEmpty() && !estado.registrando,
                        icono = Iconos.Listo,
                        modifier = Modifier.weight(2f),
                    )
                }
            }
    }

    if (!horizontal) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101416))
                    .statusBarsPadding()
                    .height(280.dp)
                    .clipToBounds(),
                content = camaraContenido,
            )
            boleta(Modifier.weight(1f))
            totales()
        }
    } else {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF101416))
                    .statusBarsPadding()
                    .clipToBounds(),
                content = camaraContenido,
            )
            Column(Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
                boleta(Modifier.weight(1f))
                totales()
            }
        }
    }

    // ── Código que no está registrado ──
    (estado.aviso as? AvisoVenta.CodigoNoRegistrado)?.let { aviso ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarAviso,
            icon = { Icon(Iconos.Escanear, contentDescription = null) },
            title = { Text("Código ${aviso.codigo} no registrado", textAlign = TextAlign.Center) },
            text = { Text("Este producto aún no está en tu inventario.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cerrarAviso()
                    onRegistrarProducto(aviso.codigo)
                }) { Text("Registrar producto") }
            },
            dismissButton = { TextButton(onClick = viewModel::cerrarAviso) { Text("Seguir escaneando") } },
        )
    }

    if (confirmarDescartar) {
        AlertDialog(
            onDismissRequest = { confirmarDescartar = false },
            title = { Text("¿Descartar la boleta?") },
            text = { Text(
                if (estado.unidades == 1) "Se perderá la unidad agregada. No se descuenta stock."
                else "Se perderán las ${estado.unidades} unidades agregadas. No se descuenta stock.",
            ) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescartar = false
                    Mensajero.mostrar("Venta cancelada")
                    onCerrar()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarDescartar = false }) { Text("Seguir vendiendo") } },
        )
    }

    if (elegirProducto) {
        SelectorProducto(
            productos = productos,
            onElegir = {
                viewModel.agregarProducto(it)
                elegirProducto = false
            },
            onCerrar = { elegirProducto = false },
        )
    }
}

@Composable
private fun AvisoFlotante(aviso: AvisoVenta) {
    val (texto, extra, ok) = when (aviso) {
        is AvisoVenta.Agregado -> Triple("${aviso.nombre} agregado", "+1", true)
        is AvisoVenta.SinStock -> Triple("${aviso.nombre} no tiene stock", "", false)
        is AvisoVenta.StockMaximo -> Triple("Solo hay ${aviso.stock} de ${aviso.nombre}", "", false)
        is AvisoVenta.BoletaMaxima -> Triple("La boleta llegó al monto máximo", "", false)
        is AvisoVenta.CodigoNoRegistrado -> Triple("", "", false)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (ok) Iconos.Listo else Iconos.Alerta,
            contentDescription = null,
            tint = if (ok) Color(0xFFA8E6B8) else Color(0xFFFFB4AB),
        )
        Text(
            texto,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (extra.isNotEmpty()) Text(extra, color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FilaBoleta(linea: LineaVenta, onMenos: () -> Unit, onMas: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(linea.nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatoPesos(linea.precioUnitario)} c/u · quedan ${linea.stock - linea.cantidad}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMenos,
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            ) {
                Icon(
                    if (linea.cantidad == 1) Iconos.Eliminar else Iconos.Quitar,
                    contentDescription = if (linea.cantidad == 1) "Quitar de la boleta" else "Quitar una",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                linea.cantidad.toString(),
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalIconButton(
                onClick = onMas,
                enabled = linea.cantidad < linea.stock,
                modifier = Modifier.size(36.dp),
            ) { Icon(Iconos.Agregar, contentDescription = "Agregar una", modifier = Modifier.size(18.dp)) }
        }
        Text(
            formatoPesos(linea.subtotal),
            // widthIn + sin salto de línea: con texto grande el precio ocupa más espacio en vez
            // de partirse en dos líneas ("$3.50" / "0").
            modifier = Modifier.widthIn(min = 72.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// Hoja que sube desde abajo para elegir un producto sin código de barras.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorProducto(
    productos: List<ProductoResumen>,
    onElegir: (ProductoResumen) -> Unit,
    onCerrar: () -> Unit,
) {
    var busqueda by remember { mutableStateOf("") }
    val buscado = normalizarNombre(busqueda)
    // Primero los que tienen stock.
    val lista = productos
        .filter { buscado.isEmpty() || it.producto.nombreNormalizado.contains(buscado) }
        .sortedBy { it.stock == 0 }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Agregar producto sin código", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar producto") },
                leadingIcon = { Icon(Iconos.Buscar, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(lista, key = { it.producto.idProducto }) { resumen ->
                    val sinStock = resumen.stock == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onElegir(resumen) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarProducto(resumen.producto.nombre, resumen.producto.urlImagen, tamano = 44.dp, apagado = sinStock)
                        Column(Modifier.weight(1f)) {
                            Text(resumen.producto.nombre, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatoPesos(resumen.producto.precioVenta), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (sinStock) "Sin stock" else "${resumen.stock} u.",
                            fontWeight = FontWeight.Bold,
                            color = if (sinStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
