package cl.miproyecto.minegocio.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.core.ui.BarraInferior
import cl.miproyecto.minegocio.core.ui.Pestana
import cl.miproyecto.minegocio.core.utils.formatoPesos
import cl.miproyecto.minegocio.core.utils.formatoPesosConSigno
import cl.miproyecto.minegocio.core.utils.nombreMes
import cl.miproyecto.minegocio.core.utils.nombreMesCorto
import cl.miproyecto.minegocio.domain.model.GananciaMes
import cl.miproyecto.minegocio.domain.model.ResumenPeriodo
import cl.miproyecto.minegocio.ui.theme.extra
import kotlin.math.abs
import kotlin.math.roundToInt

// REPORTES: cuánto se ganó, el gráfico de los últimos meses y los más vendidos.
@Composable
fun ReportesScreen(
    onPestana: (Pestana) -> Unit,
    viewModel: ReportesViewModel = viewModel(factory = ReportesViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(bottomBar = { BarraInferior(Pestana.REPORTES, onPestana) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Reportes", fontSize = 26.sp, fontWeight = FontWeight.Medium) }

            // ── Semana / Mes / Año ──
            item {
                val opciones = listOf(
                    Periodo.SEMANA to "Semana",
                    Periodo.MES to nombreMes(System.currentTimeMillis()),
                    Periodo.ANIO to "Año",
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opciones.forEachIndexed { i, (periodo, texto) ->
                        SegmentedButton(
                            selected = estado.periodo == periodo,
                            onClick = { viewModel.elegirPeriodo(periodo) },
                            shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                            label = { Text(texto) },
                            icon = {},
                        )
                    }
                }
            }

            item { TarjetaGanancia(estado.periodo, estado.resumen) }
            item { GraficoMeses(estado.meses) }

            // ── Más vendidos ──
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Más vendidos", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                    if (estado.masVendidos.isEmpty()) {
                        Text(
                            "Todavía no hay ventas en este periodo.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    estado.masVendidos.forEachIndexed { i, item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${i + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (i == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Text(item.nombre, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.unidades} u.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatoPesosConSigno(item.ganancia),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (item.ganancia < 0) MaterialTheme.colorScheme.error else MaterialTheme.extra.exito,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaGanancia(periodo: Periodo, resumen: ResumenPeriodo) {
    val colores = MaterialTheme.colorScheme
    val titulo = when (periodo) {
        Periodo.SEMANA -> "Ganancia neta de la semana"
        Periodo.MES -> "Ganancia neta del mes"
        Periodo.ANIO -> "Ganancia neta del año"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colores.primary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(titulo, fontSize = 13.sp, color = colores.onPrimary.copy(alpha = 0.9f))
        Text(formatoPesos(resumen.gananciaNeta), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colores.onPrimary)
        // Cómo se llega a la ganancia neta: ventas − costo − gastos − merma.
        val costo = resumen.ventas - resumen.gananciaVentas
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cifra("Ventas", formatoPesos(resumen.ventas), Modifier.weight(1f))
            Cifra("Costo", formatoPesos(-costo), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cifra("Gastos", formatoPesos(-resumen.gastos), Modifier.weight(1f))
            Cifra("Merma", formatoPesos(-resumen.merma), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Cifra(titulo: String, valor: String, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colores.onPrimary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(titulo, fontSize = 12.sp, color = colores.onPrimary.copy(alpha = 0.85f))
        Text(valor, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.onPrimary, maxLines = 1)
    }
}

// Gráfico de barras hecho con cajas: el alto de cada barra es proporcional a la ganancia.
@Composable
private fun GraficoMeses(meses: List<GananciaMes>) {
    val colores = MaterialTheme.colorScheme
    val extra = MaterialTheme.extra
    val maximo = meses.maxOfOrNull { abs(it.gananciaNeta) }?.takeIf { it > 0 } ?: 1
    val conDatos = meses.any { it.gananciaNeta != 0 }
    val mejor = if (conDatos) meses.maxByOrNull { it.gananciaNeta } else null
    val peor = if (conDatos && meses.size > 1) meses.minByOrNull { it.gananciaNeta } else null
    // Descripción para lectores de pantalla.
    val descripcion = meses.joinToString(", ") { "${nombreMes(it.inicioMes)} ${formatoPesos(it.gananciaNeta)}" }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colores.surfaceContainerLowest)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Ganancia neta por mes", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("miles de $", fontSize = 12.sp, color = colores.onSurfaceVariant)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics { contentDescription = "Ganancia neta: $descripcion" },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            meses.forEach { mes ->
                val color: Color = when (mes) {
                    mejor -> colores.primary
                    peor -> extra.barraPeor
                    else -> extra.barraGrafico
                }
                val fraccion = (abs(mes.gananciaNeta).toFloat() / maximo).coerceIn(0.02f, 1f)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        (mes.gananciaNeta / 1000.0).roundToInt().toString(),
                        fontSize = 11.sp,
                        color = colores.onSurfaceVariant,
                    )
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(fraccion * 0.85f)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(color),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            meses.forEach { mes ->
                Text(
                    nombreMesCorto(mes.inicioMes),
                    fontSize = 12.sp,
                    color = colores.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        if (conDatos) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Leyenda(colores.primary, "Mejor mes")
                if (peor != null) Leyenda(extra.barraPeor, "Peor mes")
            }
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(texto, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
