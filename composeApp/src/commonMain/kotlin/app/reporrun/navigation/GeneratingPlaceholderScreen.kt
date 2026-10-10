// El archivo se llama como la pantalla, no como su ruta.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunScreenScaffold
import app.reporrun.resources.Res
import app.reporrun.resources.generating_placeholder_back
import app.reporrun.resources.generating_placeholder_body
import app.reporrun.resources.generating_placeholder_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/** A dónde lleva «Generar plan» mientras no exista la pantalla Generando (C-11). */
@Serializable
internal data object GeneratingPlaceholderRoute

/**
 * Destino provisional: dice que la generación aún no está y deja volver al resumen. No genera
 * nada ni llama a nadie. **Se borra con C-11**, con sus tres textos.
 */
@Composable
internal fun GeneratingPlaceholderScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val back = stringResource(Res.string.generating_placeholder_back)
    RepOrRunScreenScaffold(
        title = stringResource(Res.string.generating_placeholder_title),
        backContentDescription = back,
        actionText = back,
        actionEnabled = true,
        onAction = onBack,
        modifier = modifier,
        onBack = onBack,
    ) {
        RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.generating_placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
