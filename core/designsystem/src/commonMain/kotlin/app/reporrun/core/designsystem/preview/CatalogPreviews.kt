package app.reporrun.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme

/** El mismo contenido en claro y en oscuro, uno encima del otro. */
@Composable
private fun BothThemes(content: @Composable () -> Unit) {
    Column {
        listOf(false, true).forEach { dark ->
            RepOrRunTheme(darkTheme = dark) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(RepOrRunDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
                ) { content() }
            }
        }
    }
}

@Preview
@Composable
private fun TypographyPreview() = BothThemes { TypographySample() }

@Preview
@Composable
private fun CardPreview() = BothThemes { EmphasisCardSample() }

@Preview
@Composable
private fun FieldPreview() = BothThemes { FieldSample() }

@Preview
@Composable
private fun ChipPreview() = BothThemes { ChipSample() }

@Preview
@Composable
private fun StepperPreview() = BothThemes { StepperSample() }

@Preview
@Composable
private fun ListRowAndSwitchPreview() = BothThemes { ListRowSample() }

@Preview
@Composable
private fun ButtonPreview() = BothThemes { ButtonSample() }

@Preview
@Composable
private fun StatePreview() = BothThemes { StateSample() }

@Preview
@Composable
private fun BottomNavPreview() = BothThemes { BottomNavSample() }
