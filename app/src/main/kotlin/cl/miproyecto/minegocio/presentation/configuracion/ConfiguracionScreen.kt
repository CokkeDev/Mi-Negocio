package cl.miproyecto.minegocio.presentation.configuracion

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.AvatarProducto
import cl.miproyecto.minegocio.core.ui.BarraSuperior
import cl.miproyecto.minegocio.core.ui.EstadoVacio
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.utils.formatoFecha
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.core.utils.iniciales
import cl.miproyecto.minegocio.domain.model.Tema
import java.text.SimpleDateFormat
import java.util.Locale

// CONFIGURACIÓN: nombre del negocio, respaldos, alertas, escáner y apariencia.
@Composable
fun ConfiguracionScreen(
    onVolver: () -> Unit,
    onProductosEliminados: () -> Unit,
    onCategoriasGastos: () -> Unit,
    viewModel: ConfiguracionViewModel = viewModel(factory = ConfiguracionViewModel.Factory),
) {
    val config by viewModel.configuracion.collectAsStateWithLifecycle()
    val eliminados by viewModel.eliminados.collectAsStateWithLifecycle()
    val trabajando by viewModel.trabajando.collectAsStateWithLifecycle()
    val reiniciar by viewModel.reiniciar.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editarNombre by remember { mutableStateOf(false) }
    var confirmarRestaurar by remember { mutableStateOf(false) }

    // "Guardar como...": el usuario elige dónde guardar el archivo del respaldo.
    val crear = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.crearRespaldo(uri)
    }
    // "Abrir...": el usuario elige el archivo del respaldo.
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restaurarRespaldo(uri)
    }

    // Después de restaurar hay que reiniciar la app para abrir la base de datos nueva.
    LaunchedEffect(reiniciar) { if (reiniciar) reiniciarApp(context) }

    Scaffold(topBar = { BarraSuperior("Configuración", onVolver) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── El negocio ──
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(iniciales(config.nombreNegocio), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(config.nombreNegocio, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Todo se guarda en este teléfono · sin cuenta",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { editarNombre = true }) {
                        Icon(Iconos.Lapiz, contentDescription = "Editar nombre del negocio")
                    }
                }
            }

            // ── Respaldo ──
            item {
                Seccion("Respaldo de datos") {
                    FilaBoton(
                        icono = Iconos.Descargar,
                        titulo = "Crear respaldo",
                        detalle = (config.ultimoRespaldo?.let { "Último: ${formatoFecha(it)}" } ?: "Nunca") + " · guardar en archivo",
                        habilitado = !trabajando,
                    ) {
                        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
                        crear.launch("mi-negocio-respaldo-$fecha.db")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    FilaBoton(
                        icono = Iconos.Subir,
                        titulo = "Restaurar respaldo",
                        detalle = "Reemplaza los datos actuales",
                        habilitado = !trabajando,
                    ) { confirmarRestaurar = true }
                    if (trabajando) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("  Copiando datos…", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Alertas ──
            item {
                Seccion("Alertas de inventario") {
                    FilaOpciones(
                        titulo = "Avisar vencimiento",
                        detalle = "Días antes de que venza un lote",
                        valor = config.diasAvisoVencimiento,
                        opciones = listOf(1, 3, 5, 7, 15),
                        formato = { if (it == 1) "1 día" else "$it días" },
                        onElegir = { dias -> viewModel.cambiar { it.copy(diasAvisoVencimiento = dias) } },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    FilaOpciones(
                        titulo = "Stock bajo",
                        detalle = "Marcar cuando queden menos de",
                        valor = config.stockBajo,
                        opciones = listOf(3, 5, 10, 20),
                        formato = { "$it u." },
                        onElegir = { n -> viewModel.cambiar { it.copy(stockBajo = n) } },
                    )
                }
            }

            // ── Venta y escáner ──
            item {
                Seccion("Venta y escáner") {
                    FilaInterruptor("Sonido al escanear", config.sonidoAlEscanear) { v ->
                        viewModel.cambiar { it.copy(sonidoAlEscanear = v) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    FilaInterruptor("Vibrar al agregar producto", config.vibrarAlAgregar) { v ->
                        viewModel.cambiar { it.copy(vibrarAlAgregar = v) }
                    }
                }
            }

            // ── Datos ──
            item {
                Seccion("Datos") {
                    FilaBoton(
                        icono = Iconos.Eliminar,
                        titulo = "Productos eliminados",
                        detalle = when (eliminados.size) {
                            0 -> "Ninguno"
                            1 -> "1 producto · ver y reactivar"
                            else -> "${eliminados.size} productos · ver y reactivar"
                        },
                        conFlecha = true,
                        onClick = onProductosEliminados,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    FilaBoton(
                        icono = Iconos.Gastos,
                        titulo = "Categorías de gastos",
                        detalle = "Servicios, arriendo, transporte, insumos y otros",
                        conFlecha = true,
                        onClick = onCategoriasGastos,
                    )
                }
            }

            // ── Apariencia ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TituloSeccion("Apariencia")
                    val opciones = listOf(Tema.SISTEMA to "Sistema", Tema.CLARO to "Claro", Tema.OSCURO to "Oscuro")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        opciones.forEachIndexed { i, (tema, texto) ->
                            SegmentedButton(
                                selected = config.tema == tema,
                                onClick = { viewModel.cambiar { it.copy(tema = tema) } },
                                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                                label = { Text(texto) },
                                icon = {},
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Mi Negocio · versión 1.0 · funciona sin internet",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (editarNombre) {
        var nombre by remember { mutableStateOf(config.nombreNegocio) }
        AlertDialog(
            onDismissRequest = { editarNombre = false },
            title = { Text("Nombre del negocio") },
            text = {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it.take(40) }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.cambiarNombre(nombre); editarNombre = false },
                    enabled = nombre.isNotBlank(),
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editarNombre = false }) { Text("Cancelar") } },
        )
    }

    if (confirmarRestaurar) {
        AlertDialog(
            onDismissRequest = { confirmarRestaurar = false },
            icon = { Icon(Iconos.Alerta, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restaurar respaldo") },
            text = {
                Text(
                    "Los productos, lotes, ventas y gastos actuales se reemplazarán por los del respaldo. " +
                        "Esto no se puede deshacer. La app se reiniciará al terminar.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestaurar = false
                    abrir.launch(arrayOf("*/*"))
                }) { Text("Elegir archivo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarRestaurar = false }) { Text("Cancelar") } },
        )
    }
}

// Cierra la app y la vuelve a abrir desde cero.
private fun reiniciarApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccion(titulo)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            content = contenido,
        )
    }
}

@Composable
private fun FilaBoton(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    habilitado: Boolean = true,
    conFlecha: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 15.sp)
            Text(detalle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (conFlecha) Icon(Iconos.Adelante, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilaOpciones(
    titulo: String,
    detalle: String,
    valor: Int,
    opciones: List<Int>,
    formato: (Int) -> String,
    onElegir: (Int) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 15.sp)
            Text(detalle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            TextButton(onClick = { abierto = true }) { Text(formato(valor) + "  ▾") }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(formato(opcion)) },
                        onClick = { onElegir(opcion); abierto = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaInterruptor(titulo: String, activado: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCambio(!activado) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titulo, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = activado, onCheckedChange = onCambio)
    }
}

// PRODUCTOS ELIMINADOS: la lista para reactivarlos.
@Composable
fun ProductosEliminadosScreen(
    onVolver: () -> Unit,
    viewModel: ConfiguracionViewModel = viewModel(factory = ConfiguracionViewModel.Factory),
) {
    val eliminados by viewModel.eliminados.collectAsStateWithLifecycle()
    Scaffold(topBar = { BarraSuperior("Productos eliminados", onVolver) }) { padding ->
        if (eliminados.isEmpty()) {
            EstadoVacio(
                icono = Iconos.Eliminar,
                titulo = "No hay productos eliminados",
                mensaje = "Cuando elimines un producto aparecerá aquí y podrás reactivarlo.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(eliminados, key = { it.idProducto }) { producto ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarProducto(producto.nombre, producto.urlImagen, apagado = true)
                    Column(Modifier.weight(1f)) {
                        Text(producto.nombre, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(formatoPesos(producto.precioVenta), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { viewModel.reactivar(producto) }) { Text("Reactivar") }
                }
            }
        }
    }
}
