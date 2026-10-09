package app.reporrun.feature.plansetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunTopBar
import app.reporrun.feature.plansetup.resources.Res
import app.reporrun.feature.plansetup.resources.prefs_back
import org.jetbrains.compose.resources.stringResource

/* Lo que compartirán las preferencias y el resumen. Todo se compone con piezas de `:core:designsystem`. */

/**
 * El armazón de una pantalla del feature: la barra de arriba, el contenido con scroll y la
 * acción fija al pie, que sigue al alcance con el teclado abierto o la fuente grande.
 */
@Composable
internal fun PlanSetupScaffold(
    title: String,
    actionText: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Incluye el teclado: con él abierto la acción sube y el contenido sigue haciendo scroll.
            .safeDrawingPadding()
            .padding(horizontal = RepOrRunDimens.ScreenPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = RepOrRunDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
        ) {
            RepOrRunTopBar(
                title = title,
                backContentDescription = stringResource(Res.string.prefs_back),
                onBack = onBack,
            )
            content()
        }
        RepOrRunButton(
            text = actionText,
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RepOrRunDimens.SectionSpacing),
            enabled = actionEnabled,
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

/** Texto de apoyo sobre el fondo de la pantalla. */
@Composable
internal fun SupportingText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Chips que saltan de línea cuando no caben. [selected] dice cuáles están elegidos. */
@Composable
internal fun <T> ChipGroup(
    options: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onClick: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
    ) {
        options.forEach { option ->
            RepOrRunChip(label = label(option), selected = selected(option), onClick = { onClick(option) })
        }
    }
}

/**
 * El error de un grupo de chips. Va **dentro de una tarjeta**: `accentText` sobre el fondo de
 * la pantalla se queda en 4,07:1 en claro (Anexo A), y sobre `surface` llega a 4,61:1.
 */
@Composable
internal fun SectionError(message: String?) {
    if (message == null) return
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = RepOrRunTheme.extendedColors.accentText)
    }
}
