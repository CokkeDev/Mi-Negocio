package cl.miproyecto.minegocio

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cl.miproyecto.minegocio.presentation.productos.ListaProductosScreen
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import cl.miproyecto.minegocio.presentation.productos.AgregarProductoScreen


@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(ListaProductos)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider =
      entryProvider {
        entry<ListaProductos> {
          ListaProductosScreen(onAgregarClick = { backStack.add(AgregarProducto) })
        }
        entry<AgregarProducto> {
          AgregarProductoScreen()
        }
      },
  )
}