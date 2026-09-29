package app.gymdi.core.designsystem

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
 * oscurecerlo y en oscuro aclararlo. Nunca uses [GymdiPalette.Accent] para una letra.
 */
@Immutable
data class GymdiExtendedColors(
    val accentText: Color,
)

private val LocalGymdiExtendedColors = staticCompositionLocalOf {
    GymdiExtendedColors(accentText = GymdiPalette.AccentInk)
}

private val LightColors = lightColorScheme(
    background = GymdiPalette.Canvas,
    onBackground = GymdiPalette.Gray900,
    surface = GymdiPalette.Paper,
    onSurface = GymdiPalette.Gray900,
    onSurfaceVariant = GymdiPalette.Gray600,
    inverseSurface = GymdiPalette.Gray900,
    inverseOnSurface = GymdiPalette.Paper,
    primary = GymdiPalette.Accent,
    onPrimary = GymdiPalette.InkOnAccent,
    outline = GymdiPalette.Gray400,
    outlineVariant = GymdiPalette.Gray400,
)

private val DarkColors = darkColorScheme(
    background = GymdiPalette.Gray900,
    onBackground = GymdiPalette.Paper,
    surface = GymdiPalette.Gray800,
    onSurface = GymdiPalette.Paper,
    onSurfaceVariant = GymdiPalette.Gray300,
    inverseSurface = GymdiPalette.Paper,
    inverseOnSurface = GymdiPalette.Gray900,
    primary = GymdiPalette.Accent,
    onPrimary = GymdiPalette.InkOnAccent,
    outline = GymdiPalette.Gray400,
    outlineVariant = GymdiPalette.Gray600,
)

/** Radios del Anexo A. Sin elevación ni sombras en ningún componente. */
val GymdiShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GymdiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extended = GymdiExtendedColors(
        accentText = if (darkTheme) GymdiPalette.AccentLight else GymdiPalette.AccentInk,
    )
    CompositionLocalProvider(LocalGymdiExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = GymdiShapes,
            content = content,
        )
    }
}

object GymdiTheme {
    val extendedColors: GymdiExtendedColors
        @Composable @ReadOnlyComposable get() = LocalGymdiExtendedColors.current
}
