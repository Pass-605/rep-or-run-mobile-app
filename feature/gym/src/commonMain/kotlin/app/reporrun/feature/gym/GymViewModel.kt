package app.reporrun.feature.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.CreateGymResult
import app.reporrun.core.domain.usecase.CreateGymUseCase
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.ObserveGymsUseCase
import app.reporrun.core.domain.usecase.RenameGymResult
import app.reporrun.core.domain.usecase.RenameGymUseCase
import app.reporrun.core.domain.usecase.SetActiveGymUseCase
import app.reporrun.core.model.GymId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Los gimnasios: la lista y el formulario del nombre. Un solo ViewModel para los dos destinos,
 * como en el perfil: vive lo que el grafo del feature.
 *
 * **No decide qué es válido ni cuál queda activo.** Qué nombre vale lo dicen [CreateGymUseCase]
 * y [RenameGymUseCase]; que el primer gimnasio quede activo, [CreateGymUseCase]; que solo se
 * active uno que existe, [SetActiveGymUseCase].
 *
 * Al revés que el perfil, la lista **sí se observa**: lo que se pinta es lo guardado, así que
 * crear, renombrar o activar no tocan [GymUiState.gyms] a mano; llega solo desde Room.
 *
 * El patrón (estado en el sitio, efectos por `Channel`, `onEvent` como única entrada) es el de
 * `OnboardingViewModel`, que explica el porqué de cada pieza.
 */
class GymViewModel(
    observeGyms: ObserveGymsUseCase,
    private val createGym: CreateGymUseCase,
    private val renameGym: RenameGymUseCase,
    private val setActiveGym: SetActiveGymUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(GymUiState())
    val state: StateFlow<GymUiState> = _state.asStateFlow()

    private val _effects = Channel<GymEffect>(Channel.BUFFERED)
    val effects: Flow<GymEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeGyms().collect { gyms -> _state.update { it.copy(isLoading = false, gyms = gyms) } }
        }
    }

    fun onEvent(event: GymEvent) {
        when (event) {
            GymEvent.AddClicked -> openEditor(GymEditorState())

            is GymEvent.RenameClicked -> openRename(event.gymId)

            is GymEvent.ActivateClicked -> activate(event.gymId)

            // El error se va en cuanto se toca el campo: vuelve, si sigue mal, al guardar.
            is GymEvent.NameChanged -> _state.update {
                it.copy(editor = it.editor.copy(name = event.value, error = null))
            }

            GymEvent.SaveClicked -> save()
        }
    }

    private fun openRename(gymId: GymId) {
        // Si ya no está en la lista no hay nada que renombrar: el toque llegó tarde.
        val gym = _state.value.gyms.firstOrNull { it.id == gymId } ?: return
        openEditor(GymEditorState(gymId = gym.id, name = gym.name))
    }

    /** Abre el formulario siempre desde cero: no arrastra el nombre ni el error de la vez anterior. */
    private fun openEditor(editor: GymEditorState) {
        _state.update { it.copy(editor = editor) }
        _effects.trySend(GymEffect.EditorOpened)
    }

    private fun activate(gymId: GymId) {
        viewModelScope.launch {
            // Con `Activated` la lista cambia sola; con `GymNotFound` no hay nada que cambiar.
            setActiveGym(gymId)
        }
    }

    private fun save() {
        val editor = _state.value.editor
        if (editor.isSaving) return
        _state.update { it.copy(editor = editor.copy(isSaving = true)) }

        viewModelScope.launch {
            val issue = if (editor.gymId == null) create(editor.name) else rename(editor.gymId, editor.name)
            if (issue == null) {
                // `isSaving` no vuelve a `false`: el formulario se va.
                _effects.send(GymEffect.EditorClosed)
            } else {
                _state.update { it.copy(editor = it.editor.copy(isSaving = false, error = issue)) }
            }
        }
    }

    /** Devuelve por qué no se pudo crear, o `null` si se creó. */
    private suspend fun create(name: String): FieldIssue? = when (val result = createGym(name)) {
        is CreateGymResult.Created -> null
        is CreateGymResult.InvalidName -> result.issue
    }

    /** Devuelve por qué no se pudo renombrar, o `null` si no queda nada que hacer en el formulario. */
    private suspend fun rename(gymId: GymId, name: String): FieldIssue? = when (val result = renameGym(gymId, name)) {
        is RenameGymResult.Renamed -> null

        // El gimnasio ya no existe: el formulario se cierra y la lista enseña lo que hay.
        RenameGymResult.GymNotFound -> null

        is RenameGymResult.InvalidName -> result.issue
    }
}
