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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.component.LoadingState
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.designsystem.component.RepOrRunField
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.model.GymId
import app.reporrun.resources.Res
import app.reporrun.resources.field_invalid
import app.reporrun.resources.field_required
import app.reporrun.resources.field_too_long
import app.reporrun.resources.scaffold_active_gym
import app.reporrun.resources.scaffold_active_gym_none
import app.reporrun.resources.scaffold_create
import app.reporrun.resources.scaffold_gym_name_label
import app.reporrun.resources.scaffold_gym_name_placeholder
import app.reporrun.resources.scaffold_intro
import app.reporrun.resources.scaffold_pending
import app.reporrun.resources.scaffold_title
import app.reporrun.ui.CollectEffects
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * La mitad con estado de la pantalla, y la única que conoce el ViewModel. Hace tres cosas:
 * recoge el estado, traduce cada efecto a una lambda de navegación y pasa `onEvent` hacia
 * abajo. No pinta nada.
 *
 * Recibe **lambdas**, no el `NavController`: un feature no sabe qué hay al otro lado.
 */
@Composable
internal fun ScaffoldRoute(onGymCreated: (GymId) -> Unit, viewModel: ScaffoldViewModel = koinViewModel()) {
    // `WithLifecycle`: con la app en segundo plano deja de recoger y no recompone para nadie.
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ScaffoldEffect.GymCreated -> onGymCreated(effect.gymId)
        }
    }

    ScaffoldScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * La mitad sin estado: una función de `(state, onEvent)`. No hay ViewModel, ni Koin, ni
 * navegación, así que es la que se previsualiza y se captura para el PR con un estado
 * escrito a mano.
 *
 * La pantalla **solo** llama a [onEvent]: no valida, no decide si el botón hace algo, no
 * guarda nada con `remember` que debiera estar en el estado.
 */
@Composable
internal fun ScaffoldScreen(state: ScaffoldUiState, onEvent: (ScaffoldEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
    ) {
        Text(stringResource(Res.string.scaffold_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.scaffold_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            SummaryCard(state)
        }

        RepOrRunField(
            value = state.gymName,
            onValueChange = { onEvent(ScaffoldEvent.GymNameChanged(it)) },
            label = stringResource(Res.string.scaffold_gym_name_label),
            placeholder = stringResource(Res.string.scaffold_gym_name_placeholder),
            errorText = state.gymNameIssue?.text(),
        )
        RepOrRunButton(
            text = stringResource(Res.string.scaffold_create),
            onClick = { onEvent(ScaffoldEvent.CreateGymClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
        )
    }
}

@Composable
private fun SummaryCard(state: ScaffoldUiState) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        RepOrRunListRow(
            title = stringResource(Res.string.scaffold_active_gym),
            subtitle = state.activeGymName ?: stringResource(Res.string.scaffold_active_gym_none),
            trailing = {},
        )
        RepOrRunDivider()
        RepOrRunListRow(
            title = stringResource(Res.string.scaffold_pending),
            subtitle = state.pendingToGenerate.toString(),
            trailing = {},
        )
    }
}

/**
 * El texto de un [FieldIssue]. La traducción de error tipado a texto es de la pantalla: el
 * dominio da el motivo y el límite, y aquí se elige cómo decirlo.
 */
@Composable
private fun FieldIssue.text(): String = when (this) {
    FieldIssue.Required -> stringResource(Res.string.field_required)
    is FieldIssue.TooLong -> stringResource(Res.string.field_too_long, maxLength)
    else -> stringResource(Res.string.field_invalid)
}
