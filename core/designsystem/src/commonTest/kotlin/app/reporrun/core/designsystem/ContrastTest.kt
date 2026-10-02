package app.reporrun.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * La auditoría de contraste del Anexo A, sobre los pares de color que usan los componentes.
 * Texto normal, 4,5:1; iconos accionables y marcas de estado, 3:1. Si un test de aquí falla,
 * no se baja el umbral: se cambia el rol del componente.
 */
class ContrastTest {
    private class Theme(val name: String, val scheme: ColorScheme, val extended: RepOrRunExtendedColors)

    private val themes = listOf(
        Theme("claro", LightColors, LightExtendedColors),
        Theme("oscuro", DarkColors, DarkExtendedColors),
    )

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= LINEAR_LIMIT) c / LINEAR_DIVISOR else ((c + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
    }

    private fun luminance(color: Color): Double =
        RED * channel(color.red) + GREEN * channel(color.green) + BLUE * channel(color.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + FLARE) / (min(la, lb) + FLARE)
    }

    private fun assertContrast(minimum: Double, what: String, pair: (Theme) -> Pair<Color, Color>) {
        themes.forEach { theme ->
            val (foreground, background) = pair(theme)
            val measured = ratio(foreground, background)
            assertTrue(measured >= minimum, "$what en ${theme.name}: $measured < $minimum")
        }
    }

    @Test
    fun primaryTextIsReadableOnSurfaceAndBackground() {
        assertContrast(TEXT, "onSurface sobre surface") { it.scheme.onSurface to it.scheme.surface }
        assertContrast(TEXT, "onBackground sobre background") { it.scheme.onBackground to it.scheme.background }
    }

    @Test
    fun secondaryTextIsReadableOnSurfaceAndBackground() {
        assertContrast(TEXT, "onSurfaceVariant sobre surface") { it.scheme.onSurfaceVariant to it.scheme.surface }
        // Chip de estado neutro, mensajes de LoadingState y ErrorState, subtítulos de pantalla.
        assertContrast(TEXT, "onSurfaceVariant sobre background") {
            it.scheme.onSurfaceVariant to it.scheme.background
        }
    }

    @Test
    fun emphasisBlockTextIsReadable() {
        // Botón principal, chip elegido, ítem activo de la barra inferior.
        assertContrast(TEXT, "inverseOnSurface sobre inverseSurface") {
            it.scheme.inverseOnSurface to it.scheme.inverseSurface
        }
    }

    @Test
    fun textOnAccentIsReadable() {
        assertContrast(TEXT, "onPrimary sobre primary") { it.scheme.onPrimary to it.scheme.primary }
    }

    @Test
    fun accentTextIsReadableOnSurface() {
        // Título destructivo de una fila y error de un campo: los dos van dentro de una tarjeta.
        assertContrast(TEXT, "accentText sobre surface") { it.extended.accentText to it.scheme.surface }
    }

    @Test
    fun activeIconsReachThreeToOne() {
        assertContrast(GRAPHIC, "iconPrimary sobre surface") { it.extended.iconPrimary to it.scheme.surface }
        // Icono de una fila, dentro de su círculo.
        assertContrast(GRAPHIC, "iconPrimary sobre background") { it.extended.iconPrimary to it.scheme.background }
    }

    @Test
    fun accentMarksReachThreeToOne() {
        // Nodo en curso del stepper (sobre el bloque de énfasis) e indicador de LoadingState.
        assertContrast(GRAPHIC, "primary sobre inverseSurface") { it.scheme.primary to it.scheme.inverseSurface }
        assertContrast(GRAPHIC, "primary sobre background") { it.scheme.primary to it.scheme.background }
    }

    @Test
    fun grayFourHundredIsNeverATextRole() {
        themes.forEach { theme ->
            val textRoles = listOf(
                theme.scheme.onSurface,
                theme.scheme.onBackground,
                theme.scheme.onSurfaceVariant,
                theme.scheme.inverseOnSurface,
                theme.scheme.onPrimary,
                theme.extended.accentText,
            )
            assertTrue(RepOrRunPalette.Gray400 !in textRoles, "#979797 es un rol de texto en ${theme.name}")
        }
    }

    private companion object {
        const val TEXT = 4.5
        const val GRAPHIC = 3.0
        const val LINEAR_LIMIT = 0.03928
        const val LINEAR_DIVISOR = 12.92
        const val GAMMA_OFFSET = 0.055
        const val GAMMA_DIVISOR = 1.055
        const val GAMMA = 2.4
        const val RED = 0.2126
        const val GREEN = 0.7152
        const val BLUE = 0.0722
        const val FLARE = 0.05
    }
}
