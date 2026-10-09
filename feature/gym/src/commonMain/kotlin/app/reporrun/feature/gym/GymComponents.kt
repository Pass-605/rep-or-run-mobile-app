package app.reporrun.feature.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.painter.Painter
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunTopBar
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.gym_back
import app.reporrun.feature.gym.resources.gym_error_above_zero
import app.reporrun.feature.gym.resources.gym_error_invalid
import app.reporrun.feature.gym.resources.gym_error_out_of_range
import app.reporrun.feature.gym.resources.gym_error_required
import app.reporrun.feature.gym.resources.gym_error_too_long
import app.reporrun.feature.gym.resources.gym_error_too_short
import org.jetbrains.compose.resources.stringResource

/* Lo que comparten la lista y el formulario. Todo se compone con piezas de `:core:designsystem`. */

/**
 * Los efectos de los gimnasios, traducidos a navegación. Los dos destinos llaman a esta misma
 * función: comparten ViewModel y, durante la transición entre uno y otro, cualquiera de los dos
 * puede ser quien recoja el efecto. Así da igual cuál sea.
 */
@Composable
internal fun GymEffects(viewModel: GymViewModel, onOpenEditor: () -> Unit, onCloseEditor: () -> Unit) {
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            GymEffect.EditorOpened -> onOpenEditor()
            GymEffect.EditorClosed -> onCloseEditor()
        }
    }
}

/**
 * El armazón de una pantalla del feature: la barra de arriba, el contenido con scroll y la
 * acción fija al pie, que sigue al alcance con el teclado abierto o la fuente grande.
 *
 * @param onBack `null` en la lista: es el primer destino y no hay a dónde volver.
 */
@Composable
internal fun GymScaffold(
    title: String,
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
            RepOrRunTopBar(
                title = title,
                backContentDescription = stringResource(Res.string.gym_back),
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
            icon = actionIcon,
            enabled = actionEnabled,
        )
    }
}

/** Texto de apoyo sobre el fondo de la pantalla. */
@Composable
internal fun SupportingText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * La frase de un error de un campo. Los límites vienen del caso de uso; aquí no hay ningún
 * número. El nombre de un gimnasio solo da `Required` y `TooLong`; el resto son de una máquina.
 */
@Composable
internal fun FieldIssue.message(): String = when (this) {
    FieldIssue.Required -> stringResource(Res.string.gym_error_required)

    is FieldIssue.TooLong -> stringResource(Res.string.gym_error_too_long, maxLength)

    is FieldIssue.TooShort -> stringResource(Res.string.gym_error_too_short, minLength)

    is FieldIssue.OutOfRange -> if (minExclusive) {
        stringResource(Res.string.gym_error_above_zero, max.toKgText())
    } else {
        stringResource(Res.string.gym_error_out_of_range, min.toKgText(), max.toKgText())
    }

    is FieldIssue.TooMany -> stringResource(Res.string.gym_error_invalid)
}
