package cl.miproyecto.minegocio

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cl.miproyecto.minegocio.presentation.productos.AgregarProductoScreen
import cl.miproyecto.minegocio.presentation.productos.AgregarProductoUiState
import cl.miproyecto.minegocio.presentation.productos.ListaProductosScreen
import cl.miproyecto.minegocio.ui.main.MainScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          ListaProductosScreen(onAgregarClick = {})
        }
      },
  )
}
