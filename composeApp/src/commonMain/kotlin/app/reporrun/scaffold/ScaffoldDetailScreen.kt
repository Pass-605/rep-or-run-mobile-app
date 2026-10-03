package app.reporrun.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.model.GymId
import app.reporrun.resources.Res
import app.reporrun.resources.scaffold_back
import app.reporrun.resources.scaffold_detail_id
import app.reporrun.resources.scaffold_detail_title
import org.jetbrains.compose.resources.stringResource

/**
 * Segundo destino de prueba: recibe por la ruta el id que creó el primero. No tiene ViewModel
 * porque no tiene nada que observar ni que guardar; una pantalla real que cargue algo por id
 * sí lo tendría, con el id en su constructor.
 */
@Composable
internal fun ScaffoldDetailScreen(gymId: GymId, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
    ) {
        RepOrRunIconButton(
            icon = RepOrRunIcons.ArrowBack,
            contentDescription = stringResource(Res.string.scaffold_back),
            onClick = onBack,
        )
        Text(stringResource(Res.string.scaffold_detail_title), style = MaterialTheme.typography.headlineSmall)
        RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
            RepOrRunListRow(
                title = stringResource(Res.string.scaffold_detail_id),
                subtitle = gymId.value,
                trailing = {},
            )
        }
    }
}
