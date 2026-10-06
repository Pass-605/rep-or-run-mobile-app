package app.reporrun.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunStep
import app.reporrun.core.designsystem.component.RepOrRunStepper
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.feature.profile.resources.Res
import app.reporrun.feature.profile.resources.profile_back
import app.reporrun.feature.profile.resources.profile_step_basics
import app.reporrun.feature.profile.resources.profile_step_goal
import app.reporrun.feature.profile.resources.profile_title
import org.jetbrains.compose.resources.stringResource

/* Lo que comparten los dos pasos. Todo se compone con piezas de `:core:designsystem`. */

internal const val STEP_BASICS = 0
internal const val STEP_GOAL = 1

/**
 * El armazón de un paso: la barra de arriba, el contenido con scroll y la acción fija al pie,
 * que sigue al alcance con el teclado abierto o la fuente grande.
 *
 * @param onBack `null` en el paso 1: es el primer destino y no hay a dónde volver.
 */
@Composable
internal fun ProfileStepScaffold(
    step: Int,
    actionText: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: Painter? = null,
    onBack: (() -> Unit)? = null,
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
            ProfileTopBar(onBack)
            RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
                RepOrRunStepper(
                    steps = listOf(
                        RepOrRunStep(stringResource(Res.string.profile_step_basics)),
                        RepOrRunStep(stringResource(Res.string.profile_step_goal)),
                    ),
                    currentStep = step,
                )
            }
            content()
        }
        RepOrRunButton(
            text = actionText,
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RepOrRunDimens.SectionSpacing),
            icon = actionIcon,
            enabled = actionEnabled,
        )
    }
}

/** Botón de volver, título centrado y un hueco del mismo ancho al otro lado para que lo esté. */
@Composable
private fun ProfileTopBar(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(RepOrRunDimens.IconButtonSize)) {
            if (onBack != null) {
                RepOrRunIconButton(
                    icon = RepOrRunIcons.ArrowBack,
                    contentDescription = stringResource(Res.string.profile_back),
                    onClick = onBack,
                )
            }
        }
        Text(
            stringResource(Res.string.profile_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.size(RepOrRunDimens.IconButtonSize))
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
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
internal fun SectionError(issue: FieldIssue?) {
    if (issue == null) return
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            issue.message(),
            style = MaterialTheme.typography.bodySmall,
            color = RepOrRunTheme.extendedColors.accentText,
        )
    }
}
