package app.reporrun.feature.gym

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.component.RepOrRunField
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.gym_editor_create
import app.reporrun.feature.gym.resources.gym_editor_new_title
import app.reporrun.feature.gym.resources.gym_editor_rename_title
import app.reporrun.feature.gym.resources.gym_editor_save
import app.reporrun.feature.gym.resources.gym_editor_subtitle
import app.reporrun.feature.gym.resources.gym_name
import app.reporrun.feature.gym.resources.gym_name_placeholder
import org.jetbrains.compose.resources.stringResource

/** El formulario, con estado. El ViewModel se lo pasa el grafo: es el mismo que el de la lista. */
@Composable
internal fun GymEditorRoute(viewModel: GymViewModel, onOpenEditor: () -> Unit, onCloseEditor: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    GymEffects(viewModel, onOpenEditor = onOpenEditor, onCloseEditor = onCloseEditor)

    GymEditorScreen(state = state.editor, onEvent = viewModel::onEvent, onBack = onCloseEditor)
}

/**
 * El nombre de un gimnasio, para crearlo o para renombrarlo: el mismo formulario con otro título
 * y otra acción. El error sale dentro del campo, con el límite que da el caso de uso.
 */
@Composable
internal fun GymEditorScreen(
    state: GymEditorState,
    onEvent: (GymEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNew = state.gymId == null
    GymScaffold(
        title = stringResource(if (isNew) Res.string.gym_editor_new_title else Res.string.gym_editor_rename_title),
        actionText = stringResource(if (isNew) Res.string.gym_editor_create else Res.string.gym_editor_save),
        actionEnabled = !state.isSaving,
        onAction = { onEvent(GymEvent.SaveClicked) },
        modifier = modifier,
        onBack = onBack,
    ) {
        SupportingText(stringResource(Res.string.gym_editor_subtitle))
        RepOrRunField(
            value = state.name,
            onValueChange = { onEvent(GymEvent.NameChanged(it)) },
            label = stringResource(Res.string.gym_name),
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.gym_name_placeholder),
            errorText = state.error?.message(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )
    }
}
