package cl.miproyecto.minegocio.presentation.productos

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.BarraSuperior
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.BotonSecundario
import cl.miproyecto.minegocio.core.ui.Campo
import cl.miproyecto.minegocio.core.ui.EscanerPantallaCompleta
import cl.miproyecto.minegocio.core.ui.FranjaAcciones
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.ui.MilesTransformation
import cl.miproyecto.minegocio.core.ui.avisarEscaneo
import cl.miproyecto.minegocio.core.ui.imagenLocal
import cl.miproyecto.minegocio.core.ui.soloDigitos
import cl.miproyecto.minegocio.core.utils.crearUriFotoTemporal
import cl.miproyecto.minegocio.core.utils.guardarImagenProducto
import kotlinx.coroutines.launch

@Composable
fun AgregarProductoScreen(
    onVolver: () -> Unit,
    onListo: (Long) -> Unit,
    skuInicial: String? = null,
    viewModel: ProductoViewModel = viewModel(factory = ProductoViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // rememberSaveable: lo escrito no se pierde si se gira el teléfono.
    var nombre by rememberSaveable { mutableStateOf("") }
    var sku by rememberSaveable { mutableStateOf(skuInicial ?: "") }
    var precio by rememberSaveable { mutableStateOf("") }
    var rutaImagen by rememberSaveable { mutableStateOf<String?>(null) }
    var elegirFoto by remember { mutableStateOf(false) }
    var escaneando by remember { mutableStateOf(false) }
    var guardandoFoto by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val vista = LocalView.current
    val scope = rememberCoroutineScope()
    // Se "escucha" la configuración: si cambia, la pantalla usa el valor nuevo.
    val config by viewModel.configuracion.collectAsStateWithLifecycle()

    // Copia la foto elegida a la carpeta de la app.
    val guardarFoto: (Uri) -> Unit = { uri ->
        scope.launch {
            guardandoFoto = true
            rutaImagen = runCatching { guardarImagenProducto(context, uri) }.getOrElse {
                Mensajero.mostrar("No se pudo cargar la foto")
                rutaImagen
            }
            guardandoFoto = false
        }
    }

    // Galería (selector de fotos de Android).
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) guardarFoto(uri)
    }
    // Cámara: guarda la foto en "uriFoto".
    var uriFoto by remember { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriFoto
        if (ok && uri != null) guardarFoto(uri)
    }
    val abrirCamara = {
        val uri = crearUriFotoTemporal(context)
        uriFoto = uri
        camara.launch(uri)
    }
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) abrirCamara() else Mensajero.mostrar("Sin permiso de cámara no se puede sacar la foto")
    }

    // Reacciona cuando el ViewModel termina de guardar.
    LaunchedEffect(uiState) {
        when (val estado = uiState) {
            is AgregarProductoUiState.ProductoAgregado -> {
                Mensajero.mostrar("Producto agregado exitosamente")
                onListo(estado.idProducto)
            }
            is AgregarProductoUiState.ProductoReactivado -> {
                Mensajero.mostrar("Producto reactivado exitosamente")
                onListo(estado.idProducto)
            }
            else -> Unit
        }
    }

    val error = uiState as? AgregarProductoUiState.Error
    val guardando = uiState is AgregarProductoUiState.Guardando

    Scaffold(
        topBar = { BarraSuperior("Agregar producto", onVolver) },
        bottomBar = {
            FranjaAcciones {
                BotonSecundario("Cancelar", onVolver, Modifier.weight(1f))
                BotonPrincipal(
                    texto = if (guardando) "Guardando…" else "Agregar",
                    onClick = { viewModel.agregarProducto(nombre, sku, rutaImagen, precio) },
                    habilitado = !guardando && !guardandoFoto,
                    modifier = Modifier.weight(2f),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── Foto ──
            SelectorFoto(rutaImagen, guardandoFoto) { elegirFoto = true }

            // Si ya existe un producto igual: por nombre y/o por código de barras.
            val yaExiste = uiState as? AgregarProductoUiState.YaExiste

            // ── Nombre ──
            Campo(
                etiqueta = "Nombre *",
                valor = nombre,
                onCambio = { nombre = it; viewModel.limpiarError() },
                placeholder = "Ej: Leche entera 1L",
                error = when {
                    yaExiste?.porNombre == true ->
                        "Ya existe un producto con este nombre"
                    error?.campo == CampoProducto.NOMBRE -> error.mensaje
                    else -> null
                },
            )

            // ── Código de barras + botón Escanear ──
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(
                    etiqueta = "Código de barras (opcional)",
                    valor = sku,
                    onCambio = { sku = it.trim(); viewModel.limpiarError() },
                    placeholder = "7801234567890",
                    teclado = KeyboardType.Number,
                    error = if (yaExiste?.porCodigo == true) "Este código ya lo tiene “${yaExiste.nombreExistente}”" else null,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { escaneando = true },
                    modifier = Modifier
                        // Si el campo de código muestra un error debajo, el botón sube para
                        // seguir alineado con la caja de texto y no con el mensaje.
                        // (El padding va ANTES de height, si no achica el botón.)
                        .padding(bottom = if (yaExiste?.porCodigo == true) 24.dp else 0.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Iconos.Escanear, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  Escanear")
                }
            }

            // ── Precio de venta ──
            Campo(
                etiqueta = "Precio de venta *",
                valor = precio,
                onCambio = { precio = soloDigitos(it); viewModel.limpiarError() },
                prefijo = "$ ",
                teclado = KeyboardType.Number,
                transformacion = MilesTransformation,
                ayuda = "En pesos chilenos, sin decimales",
                error = if (error?.campo == CampoProducto.PRECIO) error.mensaje else null,
            )

            if (error?.campo == CampoProducto.GENERAL) {
                Text(error.mensaje, color = MaterialTheme.colorScheme.error)
            }

            // ── Aviso: el resto va en el lote ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Iconos.Lote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    buildAnnotatedString {
                        append("El precio de compra, las unidades y el vencimiento se ingresan después, como un ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("lote") }
                        append(". Hasta entonces el producto se verá en gris.")
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }

    // ── Diálogo: ¿cámara o galería? ──
    if (elegirFoto) {
        AlertDialog(
            onDismissRequest = { elegirFoto = false },
            title = { Text("Foto del producto") },
            text = { Text("¿De dónde quieres sacar la foto?") },
            confirmButton = {
                TextButton(onClick = {
                    elegirFoto = false
                    val permitido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (permitido) abrirCamara() else permisoCamara.launch(Manifest.permission.CAMERA)
                }) { Text("Cámara") }
            },
            dismissButton = {
                Row {
                    if (rutaImagen != null) {
                        TextButton(onClick = { elegirFoto = false; rutaImagen = null }) { Text("Quitar") }
                    }
                    TextButton(onClick = {
                        elegirFoto = false
                        galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Galería") }
                }
            },
        )
    }

    // ── Diálogo: el producto existía pero estaba eliminado ──
    (uiState as? AgregarProductoUiState.OfrecerReactivar)?.let { estado ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarReactivacion() },
            title = { Text("Producto eliminado") },
            text = {
                Text("“${estado.producto.nombre}” fue eliminado antes. ¿Desea reactivarlo? Se recupera con su historial de ventas.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.reactivarProducto(estado.producto.idProducto) }) { Text("Reactivar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarReactivacion() }) { Text("Cancelar") }
            },
        )
    }

    // ── Escáner a pantalla completa ──
    if (escaneando) {
        Dialog(
            onDismissRequest = { escaneando = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101416)),
            ) {
                EscanerPantallaCompleta(
                    onCodigo = { codigo ->
                        avisarEscaneo(vista, config.sonidoAlEscanear, config.vibrarAlAgregar)
                        sku = codigo
                        escaneando = false
                    },
                    onCerrar = { escaneando = false },
                )
            }
        }
    }
}

// El recuadro grande para agregar la foto (punteado si no hay foto).
@Composable
private fun SelectorFoto(ruta: String?, cargando: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val imagen = imagenLocal(ruta)
    val colorBorde = colores.outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colores.surfaceContainer)
            .drawBehind {
                if (imagen == null) {
                    drawRoundRect(
                        color = colorBorde,
                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                    )
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cargando -> CircularProgressIndicator()
            imagen != null -> Image(
                bitmap = imagen,
                contentDescription = "Foto del producto. Toca para cambiarla",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colores.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Iconos.Camara, contentDescription = null, tint = colores.primary) }
                Text("Agregar foto", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Cámara o galería · opcional", fontSize = 13.sp, color = colores.onSurfaceVariant)
            }
        }
    }
}
