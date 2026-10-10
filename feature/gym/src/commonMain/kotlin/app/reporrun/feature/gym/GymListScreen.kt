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
import app.reporrun.core.designsystem.component.RepOrRunScreenScaffold
import app.reporrun.core.designsystem.component.RepOrRunSupportingText
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.gym_active
import app.reporrun.feature.gym.resources.gym_add
import app.reporrun.feature.gym.resources.gym_back
import app.reporrun.feature.gym.resources.gym_continue
import app.reporrun.feature.gym.resources.gym_create_first
import app.reporrun.feature.gym.resources.gym_empty_body
import app.reporrun.feature.gym.resources.gym_empty_hint
import app.reporrun.feature.gym.resources.gym_empty_title
import app.reporrun.feature.gym.resources.gym_list_subtitle
import app.reporrun.feature.gym.resources.gym_list_title
import app.reporrun.feature.gym.resources.gym_machines
import app.reporrun.feature.gym.resources.gym_rename
import app.reporrun.feature.gym.resources.gym_tap_to_activate
import org.jetbrains.compose.resources.stringResource

/** La lista, con estado. El ViewModel se lo pasa el grafo: es el mismo que el del formulario. */
@Composable
internal fun GymListRoute(
    viewModel: GymViewModel,
    onOpenEditor: () -> Unit,
    onCloseEditor: () -> Unit,
    onOpenInventory: (GymId) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    GymEffects(viewModel, onOpenEditor = onOpenEditor, onCloseEditor = onCloseEditor)

    GymListScreen(state = state, onEvent = viewModel::onEvent, onOpenInventory = onOpenInventory)
}

/**
 * Los gimnasios de la persona, con el activo marcado. Figma v2 no dibuja esta pantalla (su 03
 * es el inventario de un gimnasio): se compone con las piezas del design system y toma de la 03
 * la fila en tarjeta con la marca de selección al final.
 *
 * Sin gimnasios, la acción del pie es crear el primero. Con ellos es «Continuar», que lleva a
 * las máquinas del activo, y añadir otro pasa a ser la última fila de la lista. Activar,
 * renombrar y ver las máquinas se hacen sobre cada gimnasio.
 */
@Composable
internal fun GymListScreen(
    state: GymUiState,
    onEvent: (GymEvent) -> Unit,
    onOpenInventory: (GymId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = state.gyms.isEmpty()
    val active = state.gyms.firstOrNull { it.isActive }
    RepOrRunScreenScaffold(
        title = stringResource(Res.string.gym_list_title),
        backContentDescription = stringResource(Res.string.gym_back),
        actionText = stringResource(if (isEmpty) Res.string.gym_create_first else Res.string.gym_continue),
        // Con gimnasios pero ninguno activo no hay inventario al que seguir: primero se elige uno.
        actionEnabled = !state.isLoading && (isEmpty || active != null),
        onAction = { if (active == null) onEvent(GymEvent.AddClicked) else onOpenInventory(active.id) },
        modifier = modifier,
        actionIcon = if (isEmpty) RepOrRunIcons.Add else RepOrRunIcons.ArrowForward,
    ) {
        when {
            // Hasta que Room contesta no se pinta nada: ni la lista ni el estado vacío.
            state.isLoading -> Unit

            isEmpty -> EmptyGyms()

            else -> {
                RepOrRunSupportingText(stringResource(Res.string.gym_list_subtitle))
                state.gyms.forEach { gym -> GymCard(gym, onEvent, onOpenInventory) }
                AddGymCard(onEvent)
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
 * Un gimnasio: su nombre con la marca de selección y, debajo, sus máquinas y renombrar. Tocar el
 * nombre de uno que no está activo lo activa; el activo no se pulsa, porque no hay nada que
 * cambiar.
 */
@Composable
private fun GymCard(gym: Gym, onEvent: (GymEvent) -> Unit, onOpenInventory: (GymId) -> Unit) {
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
            title = stringResource(Res.string.gym_machines),
            onClick = { onOpenInventory(gym.id) },
        )
        RepOrRunDivider()
        RepOrRunListRow(
            title = stringResource(Res.string.gym_rename),
            onClick = { onEvent(GymEvent.RenameClicked(gym.id)) },
        )
    }
}

/** Añadir otro gimnasio: la última fila de la lista, porque el pie es de «Continuar». */
@Composable
private fun AddGymCard(onEvent: (GymEvent) -> Unit) {
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RepOrRunDimens.CardHorizontalPadding),
    ) {
        RepOrRunListRow(
            title = stringResource(Res.string.gym_add),
            leadingIcon = RepOrRunIcons.Add,
            onClick = { onEvent(GymEvent.AddClicked) },
        )
    }
}
