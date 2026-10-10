package app.reporrun.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunDimens

/**
 * Todos los componentes del design system en una columna, con el tema que le llegue. Es la
 * base de los previews y de las capturas del PR; los datos son de ejemplo. No se navega a
 * este composable desde la app.
 */
@Composable
fun RepOrRunCatalog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
    ) {
        TypographySample()
        EmphasisCardSample()
        FieldSample()
        ChipSample()
        SectionSample()
        StepperSample()
        StatCardSample()
        ListRowSample()
        ButtonSample()
        StateSample()
        BottomNavSample()
    }
}
