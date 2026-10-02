package app.reporrun.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Colores del sistema que Material 3 no cubre.
 *
 * `accentText` existe porque el naranja no se puede usar como texto: en claro hay que
 * oscurecerlo y en oscuro aclararlo. Nunca uses [RepOrRunPalette.Accent] para una letra.
 * Va sobre `surface`: sobre `background` en claro se queda en 4,07:1 y no vale para texto
 * normal.
 *
 * `iconPrimary` es el `icon/primary` del Anexo A: los iconos activos. En oscuro coincide con
 * `onSurface`, pero en claro es un gris más suave que el del texto. Los inactivos van en
 * `outline` y los que están sobre el bloque de énfasis, en `inverseOnSurface`.
 */
@Immutable
data class RepOrRunExtendedColors(val accentText: Color, val iconPrimary: Color)

internal val LightExtendedColors = RepOrRunExtendedColors(
    accentText = RepOrRunPalette.AccentInk,
    iconPrimary = RepOrRunPalette.Gray800,
)

internal val DarkExtendedColors = RepOrRunExtendedColors(
    accentText = RepOrRunPalette.AccentLight,
    iconPrimary = RepOrRunPalette.Paper,
)

private val LocalRepOrRunExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LocalRepOrRunExtendedTypography = staticCompositionLocalOf {
    repOrRunExtendedTypography(FontFamily.Default)
}

internal val LightColors = lightColorScheme(
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

internal val DarkColors = darkColorScheme(
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
fun RepOrRunTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val fontFamily = outfitFontFamily()
    val typography = remember(fontFamily) { repOrRunTypography(fontFamily) }
    val extendedTypography = remember(fontFamily) { repOrRunExtendedTypography(fontFamily) }
    CompositionLocalProvider(
        LocalRepOrRunExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
        LocalRepOrRunExtendedTypography provides extendedTypography,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = RepOrRunShapes,
            typography = typography,
            content = content,
        )
    }
}

object RepOrRunTheme {
    val extendedColors: RepOrRunExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalRepOrRunExtendedColors.current

    val extendedTypography: RepOrRunExtendedTypography
        @Composable @ReadOnlyComposable
        get() = LocalRepOrRunExtendedTypography.current
}
