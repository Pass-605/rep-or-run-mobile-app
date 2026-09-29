package app.reporrun.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Colores del sistema que Material 3 no cubre.
 *
 * `accentText` existe porque el naranja no se puede usar como texto: en claro hay que
 * oscurecerlo y en oscuro aclararlo. Nunca uses [RepOrRunPalette.Accent] para una letra.
 */
@Immutable
data class RepOrRunExtendedColors(
    val accentText: Color,
)

private val LocalRepOrRunExtendedColors = staticCompositionLocalOf {
    RepOrRunExtendedColors(accentText = RepOrRunPalette.AccentInk)
}

private val LightColors = lightColorScheme(
    background = RepOrRunPalette.Canvas,
    onBackground = RepOrRunPalette.Gray900,
    surface = RepOrRunPalette.Paper,
    onSurface = RepOrRunPalette.Gray900,
    onSurfaceVariant = RepOrRunPalette.Gray600,
    inverseSurface = RepOrRunPalette.Gray900,
    inverseOnSurface = RepOrRunPalette.Paper,
    primary = RepOrRunPalette.Accent,
    onPrimary = RepOrRunPalette.InkOnAccent,
    outline = RepOrRunPalette.Gray400,
    outlineVariant = RepOrRunPalette.Gray400,
)

private val DarkColors = darkColorScheme(
    background = RepOrRunPalette.Gray900,
    onBackground = RepOrRunPalette.Paper,
    surface = RepOrRunPalette.Gray800,
    onSurface = RepOrRunPalette.Paper,
    onSurfaceVariant = RepOrRunPalette.Gray300,
    inverseSurface = RepOrRunPalette.Paper,
    inverseOnSurface = RepOrRunPalette.Gray900,
    primary = RepOrRunPalette.Accent,
    onPrimary = RepOrRunPalette.InkOnAccent,
    outline = RepOrRunPalette.Gray400,
    outlineVariant = RepOrRunPalette.Gray600,
)

/** Radios del Anexo A. Sin elevación ni sombras en ningún componente. */
val RepOrRunShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RepOrRunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extended = RepOrRunExtendedColors(
        accentText = if (darkTheme) RepOrRunPalette.AccentLight else RepOrRunPalette.AccentInk,
    )
    CompositionLocalProvider(LocalRepOrRunExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = RepOrRunShapes,
            content = content,
        )
    }
}

object RepOrRunTheme {
    val extendedColors: RepOrRunExtendedColors
        @Composable @ReadOnlyComposable get() = LocalRepOrRunExtendedColors.current
}
