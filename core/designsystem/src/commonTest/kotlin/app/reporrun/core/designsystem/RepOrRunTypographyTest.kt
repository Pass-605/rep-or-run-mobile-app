package app.reporrun.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepOrRunTypographyTest {
    private val family = FontFamily.Monospace
    private val typography = repOrRunTypography(family)
    private val extended = repOrRunExtendedTypography(family)

    private val allStyles: List<TextStyle> = with(typography) {
        listOf(
            displayLarge, displayMedium, displaySmall,
            headlineLarge, headlineMedium, headlineSmall,
            titleLarge, titleMedium, titleSmall,
            bodyLarge, bodyMedium, bodySmall,
            labelLarge, labelMedium, labelSmall,
        )
    } + with(extended) { listOf(numberLarge, numberMedium, numberSmall, overline) }

    @Test
    fun everyStyleUsesTheGivenFamily() {
        assertTrue(allStyles.all { it.fontFamily == family })
    }

    @Test
    fun everyStyleUsesOneOfTheFourBundledWeights() {
        val bundled = setOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
        assertTrue(allStyles.all { it.fontWeight in bundled })
    }

    @Test
    fun displayAndHeadlineAreSemiBoldOrBold() {
        val heavy = setOf(FontWeight.SemiBold, FontWeight.Bold)
        val styles = with(typography) {
            listOf(displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall)
        }
        assertTrue(styles.all { it.fontWeight in heavy })
    }

    @Test
    fun titlesAreSemiBold() {
        val styles = with(typography) { listOf(titleLarge, titleMedium, titleSmall) }
        assertTrue(styles.all { it.fontWeight == FontWeight.SemiBold })
    }

    @Test
    fun bodyIsRegular() {
        val styles = with(typography) { listOf(bodyLarge, bodyMedium, bodySmall) }
        assertTrue(styles.all { it.fontWeight == FontWeight.Normal })
    }

    @Test
    fun labelsAreMedium() {
        val styles = with(typography) { listOf(labelLarge, labelMedium, labelSmall) }
        assertTrue(styles.all { it.fontWeight == FontWeight.Medium })
    }

    @Test
    fun numbersAreMediumOrSemiBold() {
        val styles = with(extended) { listOf(numberLarge, numberMedium, numberSmall) }
        assertTrue(styles.all { it.fontWeight == FontWeight.Medium || it.fontWeight == FontWeight.SemiBold })
    }

    @Test
    fun titleMediumMatchesTheFigmaStyle() {
        assertEquals(16.sp, typography.titleMedium.fontSize)
        assertEquals(24.sp, typography.titleMedium.lineHeight)
    }
}
