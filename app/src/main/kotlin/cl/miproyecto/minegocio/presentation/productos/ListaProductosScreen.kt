package cl.miproyecto.minegocio.presentation.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.miproyecto.minegocio.domain.model.Producto

@Composable
fun ListaProductosScreen(
    onAgregarClick: () -> Unit,
    viewModel: ListaProductosViewModel = viewModel(factory = ListaProductosViewModel.Factory),
) {
    // ── BLOQUE A: lo que la pantalla recuerda ──
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    // ── BLOQUE B: el esqueleto con el botón flotante ──
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAgregarClick) {
                Text("+ Agregar producto")
            }
        },
    ) { innerPadding ->

        // ── BLOQUE C: lista vacía o lista con productos ──
        if (productos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Aún no hay productos")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(productos, key = { it.idProducto }) { producto ->
                    FilaProducto(
                        producto = producto,
                        onEliminarClick = { productoAEliminar = producto },
                    )
                }
            }
        }
    }

    // ── BLOQUE D: confirmar antes de eliminar ──
    productoAEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Seguro que quieres eliminar \"${producto.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarProducto(producto.idProducto)
                    productoAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

// ── BLOQUE E: cómo se ve UNA fila ──
@Composable
private fun FilaProducto(
    producto: Producto,
    onEliminarClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text("$ ${producto.precioVenta}", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onEliminarClick) {
                Text("Eliminar")
            }
        }
    }
}