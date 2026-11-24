package cl.miproyecto.minegocio.presentation.merma

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.BarraSuperior
import cl.miproyecto.minegocio.core.ui.BotonPrincipal
import cl.miproyecto.minegocio.core.ui.BotonSecundario
import cl.miproyecto.minegocio.core.ui.Campo
import cl.miproyecto.minegocio.core.ui.Contador
import cl.miproyecto.minegocio.core.ui.EstadoVacio
import cl.miproyecto.minegocio.core.ui.FranjaAcciones
import cl.miproyecto.minegocio.core.ui.Iconos
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.utils.formatoDiaMes
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.presentation.productos.etiquetasDeLotes
import cl.miproyecto.minegocio.domain.model.MOTIVOS_MERMA

// REGISTRAR MERMA: unidades que se perdieron (vencidas, dañadas, robadas...).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MermaScreen(
    idProducto: Long,
    onVolver: () -> Unit,
    viewModel: MermaViewModel = viewModel(factory = MermaViewModel.factory(idProducto)),
) {
    val producto by viewModel.producto.collectAsStateWithLifecycle()
    val lotes by viewModel.lotes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Se guarda el ID del lote elegido (null = el primero de la lista).
    var idLote by rememberSaveable { mutableStateOf<Long?>(null) }
    var cantidad by rememberSaveable { mutableStateOf(1) }
    var motivo by rememberSaveable { mutableStateOf(MOTIVOS_MERMA.first()) }
    var nota by rememberSaveable { mutableStateOf("") }

    val loteElegido = lotes.firstOrNull { it.idLote == idLote } ?: lotes.firstOrNull()
    val maximo = loteElegido?.unidadesDisponibles ?: 1

    LaunchedEffect(uiState) {
        if (uiState is MermaUiState.Registrada) {
            Mensajero.mostrar("Merma registrada exitosamente")
            onVolver()
        }
    }
    val error = uiState as? MermaUiState.Error

    Scaffold(
        topBar = { BarraSuperior("Registrar merma", onVolver, subtitulo = producto?.producto?.nombre) },
        bottomBar = {
            if (lotes.isNotEmpty()) {
                FranjaAcciones {
                    BotonSecundario("Cancelar", onVolver, Modifier.weight(1f))
                    BotonPrincipal(
                        "Registrar merma",
                        onClick = { viewModel.registrarMerma(loteElegido, cantidad, motivo, nota) },
                        habilitado = uiState !is MermaUiState.Guardando,
                        modifier = Modifier.weight(1.6f),
                    )
                }
            }
        },
    ) { padding ->
        if (lotes.isEmpty()) {
            EstadoVacio(
                icono = Iconos.Lote,
                titulo = "No hay unidades",
                mensaje = "Este producto no tiene lotes con stock, así que no hay nada que registrar como merma.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── ¿De qué lote? ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Titulo("¿De qué lote?")
                val etiquetas = etiquetasDeLotes(lotes)
                lotes.forEach { lote ->
                    val elegido = lote.idLote == loteElegido?.idLote
                    val forma = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(forma)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(
                                if (elegido) 2.dp else 1.dp,
                                if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                forma,
                            )
                            // selectable = como un botón de radio: se elige uno de varios.
                            .selectable(selected = elegido, role = Role.RadioButton) {
                                idLote = lote.idLote
                                cantidad = cantidad.coerceAtMost(lote.unidadesDisponibles)
                                viewModel.limpiarError()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = elegido, onClick = null)
                        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${etiquetas.getValue(lote.idLote)} · " + (lote.fechaVencimiento?.let { "vence ${formatoDiaMes(it)}" } ?: "no vence"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "${lote.unidadesDisponibles} u. disponibles · compra ${formatoPesos(lote.precioCompra)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Cantidad ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Titulo("Cantidad perdida (máx. $maximo)", esError = error?.enCantidad == true)
                Contador(
                    valor = cantidad,
                    onCambio = { cantidad = it; viewModel.limpiarError() },
                    maximo = maximo,
                    etiquetaMenos = "Restar",
                    etiquetaMas = "Sumar",
                )
                if (error?.enCantidad == true) Text(error.mensaje, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }

            // ── Motivo ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Titulo("Motivo")
                // FlowRow: pone los botones en fila y salta de línea si no caben.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOTIVOS_MERMA.forEach { opcion ->
                        FilterChip(
                            selected = motivo == opcion,
                            onClick = { motivo = opcion },
                            label = { Text(opcion) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            }

            // ── Nota ──
            Campo(
                etiqueta = "Nota (opcional)",
                valor = nota,
                onCambio = { nota = it.take(200) },
                placeholder = "Ej: botellas golpeadas en la bodega",
                unaLinea = false,
            )

            if (error != null && !error.enCantidad) {
                Text(error.mensaje, color = MaterialTheme.colorScheme.error)
            }

            // ── Pérdida ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Pérdida a registrar", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    formatoPesos(-(loteElegido?.precioCompra ?: 0) * cantidad),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun Titulo(texto: String, esError: Boolean = false) {
    Text(
        texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
