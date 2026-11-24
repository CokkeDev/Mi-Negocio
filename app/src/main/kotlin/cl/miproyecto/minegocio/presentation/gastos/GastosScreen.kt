package cl.miproyecto.minegocio.presentation.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.BarraInferior
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.Campo
import cl.miproyecto.minegocio.core.ui.EstadoVacio
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.ui.MilesTransformation
import cl.miproyecto.minegocio.core.ui.Pestana
import cl.miproyecto.minegocio.core.ui.soloDigitos
import cl.miproyecto.minegocio.core.utils.diasHasta
import cl.miproyecto.minegocio.core.utils.formatoDiaMes
import cl.miproyecto.minegocio.core.utils.formatoFecha
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.core.utils.inicioDelDia
import cl.miproyecto.minegocio.core.utils.nombreMes
import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.presentation.lotes.localAUtc
import cl.miproyecto.minegocio.presentation.lotes.utcALocal
import cl.miproyecto.minegocio.ui.theme.extra

// GASTOS: lo que se paga que no es mercadería (luz, arriendo, transporte...).
@Composable
fun GastosScreen(
    onPestana: (Pestana) -> Unit,
    viewModel: GastosViewModel = viewModel(factory = GastosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var agregando by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Gasto?>(null) }

    Scaffold(
        bottomBar = { BarraInferior(Pestana.GASTOS, onPestana) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { agregando = true },
                icon = { Icon(Iconos.Agregar, contentDescription = null) },
                text = { Text("Agregar gasto") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Gastos", fontSize = 26.sp, fontWeight = FontWeight.Medium) }
            item { ResumenMes(estado) }

            if (estado.estaSemana.isEmpty() && estado.antes.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Iconos.Gastos,
                        titulo = "Sin gastos registrados",
                        mensaje = "Anota la luz, el arriendo o el transporte para saber tu ganancia real.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (estado.estaSemana.isNotEmpty()) {
                item { Subtitulo("Esta semana") }
                item { ListaGastos(estado.estaSemana) { aEliminar = it } }
            }
            if (estado.antes.isNotEmpty()) {
                item { Subtitulo("Antes") }
                item { ListaGastos(estado.antes) { aEliminar = it } }
            }
        }
    }

    if (agregando) {
        HojaAgregarGasto(
            onGuardar = viewModel::agregarGasto,
            onCerrar = { agregando = false },
        )
    }

    aEliminar?.let { gasto ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar gasto") },
            text = { Text("¿Eliminar “${gasto.descripcion}” por ${formatoPesos(gasto.monto)}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarGasto(gasto)
                    aEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
}

// Total del mes, % de las ventas y la barra de colores por categoría.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumenMes(estado: GastosUiState) {
    val colores = MaterialTheme.colorScheme
    val coloresCategoria = MaterialTheme.extra.categorias
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colores.surfaceContainerLowest)
            .border(1.dp, colores.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(nombreMes(System.currentTimeMillis()), fontSize = 13.sp, color = colores.onSurfaceVariant)
                val total = formatoPesos(estado.totalMes)
                // Con totales muy grandes se achica la letra para que no se parta en varias líneas.
                Text(
                    total,
                    fontSize = if (total.length > 12) 20.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            estado.porcentajeDeVentas?.let {
                Text(
                    "$it% de las ventas",
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = colores.onSecondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colores.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        if (estado.porCategoria.isNotEmpty()) {
            // La barra: cada categoría ocupa un ancho proporcional a lo que se gastó.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                estado.porCategoria.forEach { (categoria, monto) ->
                    Box(
                        Modifier
                            .weight(monto.toFloat().coerceAtLeast(1f))
                            .height(12.dp)
                            .background(coloresCategoria[categoria.ordinal]),
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                estado.porCategoria.forEach { (categoria, monto) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(coloresCategoria[categoria.ordinal]),
                        )
                        Text("${categoria.etiqueta} ${formatoPesos(monto)}", fontSize = 12.sp, color = colores.onSurfaceVariant)
                    }
                }
            }
        } else {
            Text("Este mes aún no hay gastos.", fontSize = 13.sp, color = colores.onSurfaceVariant)
        }
    }
}

@Composable
private fun ListaGastos(gastos: List<Gasto>, onTocar: (Gasto) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 14.dp),
    ) {
        gastos.forEachIndexed { i, gasto ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onTocar(gasto) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val (icono, fondo, color) = estiloCategoria(gasto.categoria)
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(fondo),
                    contentAlignment = Alignment.Center,
                ) { Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(gasto.descripcion, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${gasto.categoria.etiqueta} · ${formatoDiaMes(gasto.fecha)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatoPesos(-gasto.monto), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Ícono y colores de cada categoría (como en el diseño).
@Composable
private fun estiloCategoria(categoria: CategoriaGasto): Triple<ImageVector, Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (categoria) {
        CategoriaGasto.SERVICIOS -> Triple(Iconos.Rayo, c.secondaryContainer, c.primary)
        CategoriaGasto.ARRIENDO -> Triple(Iconos.Casa, c.surfaceVariant, c.secondary)
        CategoriaGasto.TRANSPORTE -> Triple(Iconos.Camion, c.tertiaryContainer, c.tertiary)
        CategoriaGasto.INSUMOS -> Triple(Iconos.Bolsa, c.surfaceContainerHighest, c.onSurfaceVariant)
        CategoriaGasto.OTROS -> Triple(Iconos.Puntos, c.surfaceContainerHighest, c.onSurfaceVariant)
    }
}

// Hoja de abajo con el formulario para agregar un gasto.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HojaAgregarGasto(
    onGuardar: (String, String, CategoriaGasto, Long) -> String?,
    onCerrar: () -> Unit,
) {
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CategoriaGasto.SERVICIOS) }
    var fecha by remember { mutableStateOf(inicioDelDia()) }
    var error by remember { mutableStateOf<String?>(null) }
    var calendario by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Agregar gasto", style = MaterialTheme.typography.titleLarge)
            Campo(
                etiqueta = "¿En qué se gastó? *",
                valor = descripcion,
                onCambio = { descripcion = it.take(60); error = null },
                placeholder = "Ej: Cuenta de luz",
            )
            Campo(
                etiqueta = "Monto *",
                valor = monto,
                onCambio = { monto = soloDigitos(it); error = null },
                prefijo = "$ ",
                teclado = KeyboardType.Number,
                transformacion = MilesTransformation,
            )
            Text("Categoría", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoriaGasto.entries.forEach { opcion ->
                    FilterChip(
                        selected = categoria == opcion,
                        onClick = { categoria = opcion },
                        label = { Text(opcion.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { calendario = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Iconos.Calendario, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                val dias = diasHasta(fecha)
                Text(
                    when (dias) {
                        0 -> "Hoy, ${formatoDiaMes(fecha)}"
                        -1 -> "Ayer, ${formatoDiaMes(fecha)}"
                        else -> formatoFecha(fecha)
                    },
                    modifier = Modifier.weight(1f),
                )
                Text("Cambiar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            BotonPrincipal(
                "Guardar gasto",
                onClick = {
                    val resultado = onGuardar(descripcion, monto, categoria, fecha)
                    if (resultado == null) onCerrar() else error = resultado
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (calendario) {
        val hoyUtc = localAUtc(inicioDelDia())
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = localAUtc(fecha),
            // Un gasto no puede ser de una fecha futura.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hoyUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { fecha = utcALocal(it) }
                    calendario = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estadoFecha) }
    }
}
