package cl.miproyecto.minegocio.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

// ESCÁNER DE CÓDIGOS DE BARRAS
// Muestra la cámara y avisa (onCodigo) cada vez que lee un código.
//  - CameraX maneja la cámara.
//  - ML Kit reconoce el código. Usa el modelo incluido en la app: funciona SIN internet.
// Si el mismo código se lee varias veces seguidas, solo avisa una vez cada 2 segundos.
@Composable
fun EscanerCodigo(
    onCodigo: (String) -> Unit,
    modifier: Modifier = Modifier,
    linternaEncendida: Boolean = false,
    // Avisa si hay permiso de cámara, para que la pantalla esconda el marco del escáner
    // cuando no hay cámara que apuntar.
    onPermisoCambio: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    // Pide el permiso de cámara la primera vez.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        tienePermiso = it
    }
    LaunchedEffect(Unit) { if (!tienePermiso) pedirPermiso.launch(Manifest.permission.CAMERA) }
    val avisarPermiso by rememberUpdatedState(onPermisoCambio)
    LaunchedEffect(tienePermiso) { avisarPermiso(tienePermiso) }

    if (!tienePermiso) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    "Para escanear códigos se necesita permiso para usar la cámara.",
                    color = Color(0xFFE0E3E6),
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { pedirPermiso.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
            }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    // rememberUpdatedState: siempre usa la versión más nueva de onCodigo.
    val avisar by rememberUpdatedState(onCodigo)
    val controlador = remember { LifecycleCameraController(context) }

    DisposableEffect(lifecycleOwner) {
        val lector = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_QR_CODE,
                )
                .build(),
        )
        var ultimoCodigo: String? = null
        var ultimaVez = 0L

        controlador.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controlador.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        controlador.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            analizador(lector) { codigo ->
                val ahora = System.currentTimeMillis()
                if (codigo != ultimoCodigo || ahora - ultimaVez > 2_000) {
                    ultimoCodigo = codigo
                    ultimaVez = ahora
                    avisar(codigo)
                }
            },
        )
        controlador.bindToLifecycle(lifecycleOwner)
        // Al salir de la pantalla se apaga la cámara.
        onDispose {
            controlador.unbind()
            lector.close()
        }
    }

    LaunchedEffect(linternaEncendida) { controlador.enableTorch(linternaEncendida) }

    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                // COMPATIBLE usa TextureView: así la imagen respeta el recorte de Compose
                // y no se dibuja fuera del recuadro de la cámara.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = controlador
            }
        },
    )
}

// Toma cada imagen de la cámara y se la pasa a ML Kit.
@OptIn(ExperimentalGetImage::class)
private fun analizador(
    lector: com.google.mlkit.vision.barcode.BarcodeScanner,
    onCodigo: (String) -> Unit,
) = ImageAnalysis.Analyzer { cuadro ->
    val imagen = cuadro.image
    if (imagen == null) {
        cuadro.close()
        return@Analyzer
    }
    lector.process(InputImage.fromMediaImage(imagen, cuadro.imageInfo.rotationDegrees))
        .addOnSuccessListener { codigos ->
            codigos.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }?.let(onCodigo)
        }
        // Siempre se cierra el cuadro, si no la cámara deja de mandar imágenes.
        .addOnCompleteListener { cuadro.close() }
}

// Un "bip" corto y/o una vibración al leer un código.
fun avisarEscaneo(vista: View, sonido: Boolean, vibrar: Boolean) {
    if (vibrar) vista.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    if (sonido) {
        runCatching {
            val tono = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tono.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            vista.postDelayed({ tono.release() }, 300)
        }
    }
}

// Pantalla completa con el escáner (para el formulario de producto).
@Composable
fun EscanerPantallaCompleta(onCodigo: (String) -> Unit, onCerrar: () -> Unit) {
    var linterna by remember { mutableStateOf(false) }
    var hayCamara by remember { mutableStateOf(true) }
    Box(
        Modifier
            .fillMaxSize()
            .padding(0.dp),
    ) {
        EscanerCodigo(
            onCodigo = onCodigo,
            modifier = Modifier.fillMaxSize(),
            linternaEncendida = linterna,
            onPermisoCambio = { hayCamara = it },
        )
        MarcoEscaner(
            mostrarVisor = hayCamara,
            titulo = "Escanear código",
            onCerrar = onCerrar,
            linterna = linterna,
            onLinterna = { linterna = !linterna },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
