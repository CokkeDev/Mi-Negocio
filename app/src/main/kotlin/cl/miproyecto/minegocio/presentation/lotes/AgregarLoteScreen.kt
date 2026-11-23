package cl.miproyecto.minegocio.presentation.lotes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.AvatarProducto
import cl.miproyecto.minegocio.core.ui.BarraSuperior
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.Campo
import cl.miproyecto.minegocio.core.ui.Contador
import cl.miproyecto.minegocio.core.ui.FilaDato
import cl.miproyecto.minegocio.core.ui.FranjaAcciones
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.ui.MilesTransformation
import cl.miproyecto.minegocio.core.ui.soloDigitos
import cl.miproyecto.minegocio.core.utils.formatoDiaMes
import cl.miproyecto.minegocio.core.utils.formatoFecha
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.core.utils.formatoPesosConSigno
import cl.miproyecto.minegocio.core.utils.inicioDelDia
import cl.miproyecto.minegocio.domain.usecase.costoPorUnidad
import cl.miproyecto.minegocio.ui.theme.extra
import java.util.Calendar
import java.util.TimeZone

// INGRESAR LOTE: lo que se compró de un producto (precio, unidades y vencimiento).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarLoteScreen(
    idProducto: Long,
    onVolver: () -> Unit,
    viewModel: AgregarLoteViewModel = viewModel(factory = AgregarLoteViewModel.factory(idProducto)),
) {
    val producto by viewModel.producto.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var precio by rememberSaveable { mutableStateOf("") }
    var unidades by rememberSaveable { mutableStateOf(12) }
    var vencimiento by rememberSaveable { mutableStateOf<Long?>(null) }
    var noVence by rememberSaveable { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AgregarLoteUiState.Guardado) {
            Mensajero.mostrar("Lote agregado exitosamente")
            onVolver()
        }
    }

    val error = uiState as? AgregarLoteUiState.Error
    val costoTotal = precio.toIntOrNull() ?: 0
    val costoUnidad = if (unidades > 0) costoPorUnidad(costoTotal, unidades) else 0
    val precioVenta = producto?.producto?.precioVenta ?: 0

    Scaffold(
        topBar = { BarraSuperior("Ingresar lote", onVolver) },
        bottomBar = {
            FranjaAcciones {
                BotonPrincipal(
                    "Guardar lote",
                    onClick = { viewModel.guardarLote(precio, unidades, vencimiento, noVence) },
                    habilitado = uiState !is AgregarLoteUiState.Guardando && producto != null,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── El producto ──
            producto?.let { resumen ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarProducto(resumen.producto.nombre, resumen.producto.urlImagen, tamano = 48.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(resumen.producto.nombre, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Venta ${formatoPesos(resumen.producto.precioVenta)} · stock actual ${resumen.stock} u.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Costo total del lote ──
            // Se pide lo que se pagó por TODO el lote; el costo por unidad lo calcula la app.
            Campo(
                etiqueta = "Costo total del lote *",
                valor = precio,
                onCambio = { precio = soloDigitos(it); viewModel.limpiarError() },
                prefijo = "$ ",
                teclado = KeyboardType.Number,
                transformacion = MilesTransformation,
                error = if (error?.campo == CampoLote.PRECIO) error.mensaje else null,
            )

            // ── Unidades ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Unidades compradas *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Contador(valor = unidades, onCambio = { unidades = it; viewModel.limpiarError() })
                if (error?.campo == CampoLote.UNIDADES) Text(error.mensaje, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }

            // ── Vencimiento ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val errorFecha = error?.campo == CampoLote.VENCIMIENTO
                val colores = MaterialTheme.colorScheme
                Text(
                    "Fecha de vencimiento",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (errorFecha) colores.error else colores.onSurfaceVariant,
                )
                // Parece un campo de texto, pero al tocarlo abre el calendario.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, if (errorFecha) colores.error else colores.outline, RoundedCornerShape(8.dp))
                        .background(if (noVence) colores.surfaceContainer else colores.surfaceContainerLowest)
                        .clickable(enabled = !noVence) { mostrarCalendario = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Iconos.Calendario, contentDescription = null, tint = colores.onSurfaceVariant)
                    Text(
                        vencimiento?.let { formatoFecha(it) } ?: if (noVence) "No vence" else "Toca para elegir",
                        fontSize = 16.sp,
                        color = if (vencimiento == null) colores.onSurfaceVariant else colores.onSurface,
                    )
                }
                if (errorFecha) Text(error.mensaje, fontSize = 12.sp, color = colores.error)
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable {
                            noVence = !noVence
                            if (noVence) vencimiento = null
                            viewModel.limpiarError()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = noVence, onCheckedChange = null)
                    Text("  Este producto no vence", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Resumen ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilaDato("Fecha de ingreso", "Hoy, ${formatoDiaMes(System.currentTimeMillis())}")
                FilaDato("Inversión del lote", formatoPesos(costoTotal))
                // Lo que se guarda como precio de compra de cada unidad.
                FilaDato("Costo por unidad", if (costoTotal > 0) formatoPesos(costoUnidad) else "—")
                // Sin costo todavía no se puede calcular: se muestra "—".
                val ganancia = (precioVenta - costoUnidad) * unidades
                FilaDato(
                    "Ganancia estimada",
                    if (costoTotal > 0) formatoPesosConSigno(ganancia) else "—",
                    colorValor = if (ganancia < 0) MaterialTheme.colorScheme.error else MaterialTheme.extra.exito,
                    destacado = true,
                )
            }
        }
    }

    // ── Calendario ──
    if (mostrarCalendario) {
        val hoyUtc = localAUtc(inicioDelDia())
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = vencimiento?.let { localAUtc(it) },
            // Solo deja elegir desde hoy en adelante.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hoyUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { vencimiento = utcALocal(it) }
                    mostrarCalendario = false
                    viewModel.limpiarError()
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

// El calendario de Material trabaja en hora UTC (Londres). Estas funciones pasan
// "el mismo día" entre UTC y la hora del teléfono, para que no quede un día corrido.
fun utcALocal(utc: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear()
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun localAUtc(local: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
