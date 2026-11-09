package cl.miproyecto.minegocio.presentation.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AgregarProductoScreen(
    viewModel: ProductoViewModel = viewModel(factory = ProductoViewModel.Factory),
) {
    // ── BLOQUE A: lo que la pantalla recuerda ──
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var nombre by rememberSaveable { mutableStateOf("") }
    var sku by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }

    // ── BLOQUE B: el formulario ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Agregar producto", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = sku,
            onValueChange = { sku = it },
            label = { Text("Código de barras (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = { Text("Precio de venta") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { viewModel.agregarProducto(nombre, sku, null, precio) },
            enabled = uiState !is AgregarProductoUiState.Guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Agregar")
        }

        // ── BLOQUE C: qué mostrar según el estado ──
        when (val estado = uiState) {
            AgregarProductoUiState.Inicial -> {}
            AgregarProductoUiState.Guardando -> CircularProgressIndicator()
            AgregarProductoUiState.ProductoAgregado -> Text("Producto agregado exitosamente")
            AgregarProductoUiState.ProductoReactivado -> Text("Producto reactivado exitosamente")
            AgregarProductoUiState.YaExiste -> Text(
                "No se pudo agregar, el producto ya existe",
                color = MaterialTheme.colorScheme.error,
            )
            is AgregarProductoUiState.Error -> Text(
                estado.mensaje,
                color = MaterialTheme.colorScheme.error,
            )
            is AgregarProductoUiState.OfrecerReactivar -> AlertDialog(
                onDismissRequest = { viewModel.cancelarReactivacion() },
                title = { Text("Producto eliminado") },
                text = { Text("\"${estado.producto.nombre}\" fue eliminado antes. ¿Desea reactivarlo?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.reactivarProducto(estado.producto.idProducto) }) {
                        Text("Reactivar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelarReactivacion() }) {
                        Text("Cancelar")
                    }
                },
            )
        }
    }
}