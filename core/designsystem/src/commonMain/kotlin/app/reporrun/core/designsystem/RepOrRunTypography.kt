package app.reporrun.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.reporrun.core.designsystem.resources.Res
import app.reporrun.core.designsystem.resources.outfit_bold
import app.reporrun.core.designsystem.resources.outfit_medium
import app.reporrun.core.designsystem.resources.outfit_regular
import app.reporrun.core.designsystem.resources.outfit_semibold
import org.jetbrains.compose.resources.Font

/**
 * Outfit en los cuatro pesos que se empaquetan (TTF estáticos, OFL 1.1; ver [RepOrRunLicenses]).
 * Son estáticos y no la fuente variable para que Android e iOS pinten igual.
 */
@Composable
internal fun outfitFontFamily(): FontFamily = FontFamily(
    Font(Res.font.outfit_regular, FontWeight.Normal),
    Font(Res.font.outfit_medium, FontWeight.Medium),
    Font(Res.font.outfit_semibold, FontWeight.SemiBold),
    Font(Res.font.outfit_bold, FontWeight.Bold),
)

/**
 * Estilos de texto del diseño que Material 3 no tiene.
 *
 * Las cifras (series, repeticiones, kg) van en `number*`, en 500-600 para leerlas en el
 * gimnasio. [overline] se escribe en mayúsculas: el estilo no las fuerza, las pone quien lo usa.
 */
@Immutable
data class RepOrRunExtendedTypography(
    val numberLarge: TextStyle,
    val numberMedium: TextStyle,
    val numberSmall: TextStyle,
    val overline: TextStyle,
)

private fun style(fontFamily: FontFamily, weight: FontWeight, size: Int, lineHeight: Int, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
    )

/**
 * La escala de Material 3 con [fontFamily] y los estilos de texto de la librería de Figma:
 * display y headline en 700 (headline pequeño en 600), title en 600, body en 400 y label en 500.
 *
 * Figma solo define `Display/Large`; `displayMedium` y `displaySmall` conservan el tamaño de
 * Material 3 con el peso de la familia.
 */
internal fun repOrRunTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = style(fontFamily, FontWeight.Bold, size = 56, lineHeight = 60, tracking = -1.0),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = style(fontFamily, FontWeight.Bold, size = 36, lineHeight = 40, tracking = -0.5),
        headlineMedium = style(fontFamily, FontWeight.Bold, size = 28, lineHeight = 32, tracking = -0.25),
        headlineSmall = style(fontFamily, FontWeight.SemiBold, size = 24, lineHeight = 28),
        titleLarge = style(fontFamily, FontWeight.SemiBold, size = 22, lineHeight = 28),
        titleMedium = style(fontFamily, FontWeight.SemiBold, size = 16, lineHeight = 24, tracking = 0.1),
        titleSmall = style(fontFamily, FontWeight.SemiBold, size = 14, lineHeight = 20, tracking = 0.1),
        bodyLarge = style(fontFamily, FontWeight.Normal, size = 16, lineHeight = 24, tracking = 0.1),
        bodyMedium = style(fontFamily, FontWeight.Normal, size = 14, lineHeight = 20, tracking = 0.1),
        bodySmall = style(fontFamily, FontWeight.Normal, size = 12, lineHeight = 16, tracking = 0.2),
        labelLarge = style(fontFamily, FontWeight.Medium, size = 14, lineHeight = 20, tracking = 0.1),
        labelMedium = style(fontFamily, FontWeight.Medium, size = 12, lineHeight = 16, tracking = 0.4),
        labelSmall = style(fontFamily, FontWeight.Medium, size = 11, lineHeight = 16, tracking = 0.5),
    )
}

/** Los estilos `Number` y `Overline` de la librería de Figma con [fontFamily]. */
internal fun repOrRunExtendedTypography(fontFamily: FontFamily) = RepOrRunExtendedTypography(
    numberLarge = style(fontFamily, FontWeight.SemiBold, size = 44, lineHeight = 48, tracking = -1.0),
    numberMedium = style(fontFamily, FontWeight.SemiBold, size = 20, lineHeight = 24),
    numberSmall = style(fontFamily, FontWeight.Medium, size = 14, lineHeight = 20, tracking = 0.2),
    overline = style(fontFamily, FontWeight.SemiBold, size = 11, lineHeight = 16, tracking = 1.5),
)
