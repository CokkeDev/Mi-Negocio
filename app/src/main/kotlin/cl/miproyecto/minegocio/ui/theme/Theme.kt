package cl.miproyecto.minegocio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import cl.miproyecto.minegocio.domain.model.Tema

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

// Colores que Material 3 no trae y que usa el diseño:
// verde para la ganancia, naranjo para "por vencer", y los del gráfico y los gastos.
@Immutable
data class ColoresExtra(
    val exito: Color,
    val avisoContenedor: Color,
    val onAvisoContenedor: Color,
    val barraGrafico: Color,
    val barraPeor: Color,
    val categorias: List<Color>,
)

private val ExtraClaro = ColoresExtra(
    exito = Color(0xFF1B6B3A),
    avisoContenedor = Color(0xFFFFDDB3),
    onAvisoContenedor = Color(0xFF5C3800),
    barraGrafico = Color(0xFF8DB4CA),
    barraPeor = Color(0xFFE8A29B),
    categorias = listOf(
        Color(0xFF00506E), Color(0xFF486271), Color(0xFF885E9A), Color(0xFFBFC8CF), Color(0xFF70787E),
    ),
)

private val ExtraOscuro = ColoresExtra(
    exito = Color(0xFF8AD5A0),
    avisoContenedor = Color(0xFF5C3800),
    onAvisoContenedor = Color(0xFFFFDDB3),
    barraGrafico = Color(0xFF3E6377),
    barraPeor = Color(0xFFB3615A),
    categorias = listOf(
        Color(0xFF87CFF9), Color(0xFFB0CADC), Color(0xFFE6B5F8), Color(0xFF6B7479), Color(0xFF899298),
    ),
)

// "CompositionLocal": una forma de entregar un valor a todas las pantallas
// sin pasarlo como parámetro una por una. Se lee con MaterialTheme.extra.
private val LocalColoresExtra = staticCompositionLocalOf { ExtraClaro }

val MaterialTheme.extra: ColoresExtra
    @Composable get() = LocalColoresExtra.current

@Composable
fun MiNegocioTheme(
    tema: Tema = Tema.SISTEMA,
    content: @Composable () -> Unit,
) {
    val oscuro = when (tema) {
        Tema.SISTEMA -> isSystemInDarkTheme()
        Tema.CLARO -> false
        Tema.OSCURO -> true
    }
    // Los íconos de la barra de estado (hora, batería) deben verse claros sobre el tema oscuro
    // y oscuros sobre el claro, aunque el teléfono tenga otro modo.
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = !oscuro
                isAppearanceLightNavigationBars = !oscuro
            }
        }
    }
    CompositionLocalProvider(LocalColoresExtra provides if (oscuro) ExtraOscuro else ExtraClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
