// Un destino sin ViewModel no merece un archivo aparte para su ruta.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.start

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
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.resources.Res
import app.reporrun.resources.start_body
import app.reporrun.resources.start_notice_row
import app.reporrun.resources.start_notice_row_hint
import app.reporrun.resources.start_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/** A dónde se llega con el consentimiento dado, mientras no exista el perfil. */
@Serializable
internal data object StartPlaceholderRoute

/**
 * **Destino provisional (C-05).** Lo que hay después de aceptar el aviso hasta que entre el
 * perfil (C-06), que lo sustituye y lo borra con sus textos. No sale de Figma.
 *
 * Su única fila abre el aviso completo: es la entrada que tendrá Ajustes (Q-01) y lo que
 * demuestra que el aviso se puede leer fuera del primer arranque.
 */
@Composable
internal fun StartPlaceholderScreen(onOpenNotice: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
    ) {
        Text(stringResource(Res.string.start_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.start_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
            RepOrRunListRow(
                title = stringResource(Res.string.start_notice_row),
                subtitle = stringResource(Res.string.start_notice_row_hint),
                onClick = onOpenNotice,
            )
        }
    }
}
