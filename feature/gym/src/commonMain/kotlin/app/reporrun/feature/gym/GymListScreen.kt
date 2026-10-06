package app.reporrun.feature.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunCheckMark
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.model.Gym
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.gym_active
import app.reporrun.feature.gym.resources.gym_add
import app.reporrun.feature.gym.resources.gym_create_first
import app.reporrun.feature.gym.resources.gym_empty_body
import app.reporrun.feature.gym.resources.gym_empty_hint
import app.reporrun.feature.gym.resources.gym_empty_title
import app.reporrun.feature.gym.resources.gym_list_subtitle
import app.reporrun.feature.gym.resources.gym_list_title
import app.reporrun.feature.gym.resources.gym_rename
import app.reporrun.feature.gym.resources.gym_tap_to_activate
import org.jetbrains.compose.resources.stringResource

/** La lista, con estado. El ViewModel se lo pasa el grafo: es el mismo que el del formulario. */
@Composable
internal fun GymListRoute(viewModel: GymViewModel, onOpenEditor: () -> Unit, onCloseEditor: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    GymEffects(viewModel, onOpenEditor = onOpenEditor, onCloseEditor = onCloseEditor)

    GymListScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * Los gimnasios de la persona, con el activo marcado. Figma v2 no dibuja esta pantalla (su 03
 * es el inventario de un gimnasio): se compone con las piezas del design system y toma de la 03
 * la fila en tarjeta con la marca de selección al final.
 *
 * La acción del pie es siempre crear: es lo único que la pantalla puede empezar. Activar y
 * renombrar se hacen sobre cada gimnasio.
 */
@Composable
internal fun GymListScreen(state: GymUiState, onEvent: (GymEvent) -> Unit, modifier: Modifier = Modifier) {
    val isEmpty = state.gyms.isEmpty()
    GymScaffold(
        title = stringResource(Res.string.gym_list_title),
        actionText = stringResource(if (isEmpty) Res.string.gym_create_first else Res.string.gym_add),
        actionEnabled = !state.isLoading,
        onAction = { onEvent(GymEvent.AddClicked) },
        modifier = modifier,
        actionIcon = RepOrRunIcons.Add,
    ) {
        when {
            // Hasta que Room contesta no se pinta nada: ni la lista ni el estado vacío.
            state.isLoading -> Unit

            isEmpty -> EmptyGyms()

            else -> {
                SupportingText(stringResource(Res.string.gym_list_subtitle))
                state.gyms.forEach { gym -> GymCard(gym, onEvent) }
            }
        }
    }
}

/** El estado vacío: qué es un gimnasio aquí y por dónde se empieza. La acción está al pie. */
@Composable
private fun EmptyGyms() {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.StateSpacing)) {
            Text(stringResource(Res.string.gym_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.gym_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.gym_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Un gimnasio: su nombre con la marca de selección y, debajo, renombrar. Tocar el nombre de uno
 * que no está activo lo activa; el activo no se pulsa, porque no hay nada que cambiar.
 */
@Composable
private fun GymCard(gym: Gym, onEvent: (GymEvent) -> Unit) {
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RepOrRunDimens.CardHorizontalPadding),
    ) {
        RepOrRunListRow(
            title = gym.name,
            // La marca es decorativa: el estado se dice en el subtítulo y se anuncia como selección.
            modifier = Modifier.semantics { selected = gym.isActive },
            subtitle = stringResource(if (gym.isActive) Res.string.gym_active else Res.string.gym_tap_to_activate),
            onClick = if (gym.isActive) null else ({ onEvent(GymEvent.ActivateClicked(gym.id)) }),
            trailing = { RepOrRunCheckMark(checked = gym.isActive) },
        )
        RepOrRunDivider()
        RepOrRunListRow(
            title = stringResource(Res.string.gym_rename),
            onClick = { onEvent(GymEvent.RenameClicked(gym.id)) },
        )
    }
}
