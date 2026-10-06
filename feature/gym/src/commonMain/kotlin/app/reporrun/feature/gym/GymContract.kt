// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.gym

import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId

/**
 * La lista de gimnasios y el formulario del nombre: los dos destinos comparten estado porque
 * comparten ViewModel.
 *
 * - [gyms] es lo guardado, tal cual, del más antiguo al más reciente. Cuál está activo lo dice
 *   [Gym.isActive]; aquí no se guarda aparte.
 * - [isLoading] empieza en `true`: hasta que Room contesta no se pinta ni la lista ni el estado
 *   vacío, para que el vacío no parpadee antes de que aparezcan los gimnasios.
 */
data class GymUiState(
    val isLoading: Boolean = true,
    val gyms: List<Gym> = emptyList(),
    val editor: GymEditorState = GymEditorState(),
)

/**
 * El formulario del nombre. Sirve para crear y para renombrar.
 *
 * @property gymId el gimnasio que se renombra, o `null` si se está creando uno.
 * @property error el motivo que dio el caso de uso, con su límite. La frase la pone la pantalla.
 */
data class GymEditorState(
    val gymId: GymId? = null,
    val name: String = "",
    val error: FieldIssue? = null,
    val isSaving: Boolean = false,
)

sealed interface GymEvent {
    /** Crear un gimnasio: abre el formulario vacío. */
    data object AddClicked : GymEvent

    /** Renombrar [gymId]: abre el formulario con su nombre. */
    data class RenameClicked(val gymId: GymId) : GymEvent

    /** Usar [gymId] como gimnasio activo. */
    data class ActivateClicked(val gymId: GymId) : GymEvent

    data class NameChanged(val value: String) : GymEvent

    /** Fin del formulario: crea o renombra, según cómo se abrió. */
    data object SaveClicked : GymEvent
}

sealed interface GymEffect {
    /** El formulario está listo para mostrarse. */
    data object EditorOpened : GymEffect

    /** El formulario terminó: se vuelve a la lista. */
    data object EditorClosed : GymEffect
}
