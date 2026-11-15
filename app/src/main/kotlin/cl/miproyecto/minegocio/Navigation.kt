package cl.miproyecto.minegocio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cl.miproyecto.minegocio.core.ui.Mensajero
import cl.miproyecto.minegocio.core.ui.Pestana
import cl.miproyecto.minegocio.presentation.configuracion.ConfiguracionScreen
import cl.miproyecto.minegocio.presentation.configuracion.ProductosEliminadosScreen
import cl.miproyecto.minegocio.presentation.dashboard.ReportesScreen
import cl.miproyecto.minegocio.presentation.gastos.GastosScreen
import cl.miproyecto.minegocio.presentation.lotes.AgregarLoteScreen
import cl.miproyecto.minegocio.presentation.merma.MermaScreen
import cl.miproyecto.minegocio.presentation.productos.AgregarProductoScreen
import cl.miproyecto.minegocio.presentation.productos.DetalleProductoScreen
import cl.miproyecto.minegocio.presentation.productos.ListaProductosScreen
import cl.miproyecto.minegocio.presentation.ventas.VentaScreen

// NAVEGACIÓN
// "backStack" es la PILA de pantallas: la de arriba es la que se ve.
//   backStack.add(X)          -> abre la pantalla X encima
//   backStack.removeLastOrNull() -> vuelve atrás
@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(ListaProductos)
    val volver: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    val irAPestana: (Pestana) -> Unit = { irAPestana(backStack, it) }

    // Los mensajes de abajo (Snackbar) de toda la app se muestran aquí.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        Mensajero.mensajes.collect { mensaje ->
            val resultado = snackbar.showSnackbar(mensaje.texto, actionLabel = mensaje.accion, withDismissAction = mensaje.accion == null)
            if (resultado == SnackbarResult.ActionPerformed) mensaje.alAccion?.invoke()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = volver,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<ListaProductos> {
                    ListaProductosScreen(
                        onAgregarClick = { backStack.add(AgregarProducto()) },
                        onProductoClick = { backStack.add(DetalleProducto(it)) },
                        onIngresarLote = { backStack.add(AgregarLote(it)) },
                        onEscanear = { backStack.add(Venta) },
                        onConfiguracion = { backStack.add(Configuracion) },
                        onPestana = irAPestana,
                    )
                }
                entry<AgregarProducto> { clave ->
                    AgregarProductoScreen(
                        skuInicial = clave.sku,
                        onVolver = volver,
                        // Al guardar, el formulario se reemplaza por el detalle del producto.
                        onListo = { id ->
                            backStack.removeLastOrNull()
                            backStack.add(DetalleProducto(id))
                        },
                    )
                }
                entry<DetalleProducto> { clave ->
                    DetalleProductoScreen(
                        idProducto = clave.idProducto,
                        onVolver = volver,
                        onIngresarLote = { backStack.add(AgregarLote(clave.idProducto)) },
                        onMerma = { backStack.add(RegistrarMerma(clave.idProducto)) },
                    )
                }
                entry<AgregarLote> { clave ->
                    AgregarLoteScreen(idProducto = clave.idProducto, onVolver = volver)
                }
                entry<RegistrarMerma> { clave ->
                    MermaScreen(idProducto = clave.idProducto, onVolver = volver)
                }
                entry<Venta> {
                    VentaScreen(
                        onCerrar = volver,
                        onRegistrarProducto = { sku -> backStack.add(AgregarProducto(sku)) },
                        onVerReportes = { irAPestana(Pestana.REPORTES) },
                    )
                }
                entry<Reportes> {
                    ReportesScreen(onPestana = irAPestana)
                }
                entry<Gastos> {
                    GastosScreen(onPestana = irAPestana)
                }
                entry<Configuracion> {
                    ConfiguracionScreen(
                        onVolver = volver,
                        onProductosEliminados = { backStack.add(ProductosEliminados) },
                        onCategoriasGastos = { irAPestana(Pestana.GASTOS) },
                    )
                }
                entry<ProductosEliminados> {
                    ProductosEliminadosScreen(onVolver = volver)
                }
            },
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // En Inicio y Gastos hay barra inferior + botón flotante: el aviso va más arriba
                // para no taparlos.
                .padding(bottom = if (backStack.lastOrNull() in listOf(ListaProductos, Gastos)) 160.dp else 88.dp),
        )
    }
}

// Las pestañas de abajo no se "apilan": siempre queda Inicio al fondo y encima la pestaña elegida.
// Así, al apretar "atrás" desde Reportes se vuelve a Inicio.
private fun irAPestana(backStack: NavBackStack<NavKey>, pestana: Pestana) {
    val destino: NavKey = when (pestana) {
        Pestana.INICIO -> ListaProductos
        Pestana.VENDER -> Venta
        Pestana.REPORTES -> Reportes
        Pestana.GASTOS -> Gastos
    }
    if (backStack.firstOrNull() != ListaProductos) backStack.add(0, ListaProductos)
    // Se quitan todas las pantallas que estén encima de Inicio...
    while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    // ...y se pone la pestaña elegida.
    if (destino != ListaProductos) backStack.add(destino)
}
