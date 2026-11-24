package cl.miproyecto.minegocio.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Lo que se dibuja ENCIMA de la cámara: barra con cerrar y linterna,
// el recuadro celeste con la línea roja, y el texto de ayuda.
@Composable
fun MarcoEscaner(
    titulo: String,
    onCerrar: () -> Unit,
    linterna: Boolean,
    onLinterna: () -> Unit,
    modifier: Modifier = Modifier,
    // false = sin permiso de cámara: se esconden el recuadro y el texto de ayuda
    // para que no se encimen con el mensaje de "Dar permiso".
    mostrarVisor: Boolean = true,
) {
    val claro = Color(0xFFE0E3E6)
    Column(modifier = modifier.statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCerrar) { Icon(Iconos.Cerrar, contentDescription = "Cerrar", tint = claro) }
            Text(
                titulo,
                color = claro,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onLinterna) {
                Icon(
                    Iconos.Linterna,
                    contentDescription = if (linterna) "Apagar linterna" else "Encender linterna",
                    tint = if (linterna) Color(0xFFFFDDB3) else claro,
                )
            }
        }
        if (!mostrarVisor) return@Column
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .size(width = 250.dp, height = 130.dp)
                        .border(3.dp, Color(0xFF87CFF9), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color(0xFFFFB4AB)),
                    )
                }
                Text(
                    "Apunta al código de barras",
                    color = Color(0xFFBFC8CF),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0x66000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
