// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.scaffold

import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.model.GymId

/**
 * Todo lo que la pantalla necesita para pintarse, y nada más.
 *
 * - **Inmutable y con valores por defecto**: `ScaffoldUiState()` es el primer fotograma, antes
 *   de que Room conteste. Por eso [isLoading] nace en `true`.
 * - **Ya masticado**: lleva `activeGymName`, no el `Gym`. La pantalla no decide ni calcula.
 * - **El error va tipado** ([FieldIssue]), no como texto: el texto sale de `strings.xml` y el
 *   ViewModel no conoce recursos. Va en el estado y no en un efecto porque dura hasta que la
 *   persona corrige el campo.
 * - Lo que la persona teclea ([gymName]) también es estado: sobrevive a un giro de pantalla
 *   porque sobrevive el ViewModel.
 */
data class ScaffoldUiState(
    val isLoading: Boolean = true,
    val activeGymName: String? = null,
    val pendingToGenerate: Int = 0,
    val gymName: String = "",
    val gymNameIssue: FieldIssue? = null,
    val isSaving: Boolean = false,
)

/**
 * Lo que la persona hizo, no lo que el ViewModel debe hacer: `CreateGymClicked`, no
 * `CreateGym`. La pantalla informa; qué pasa después lo decide el ViewModel.
 *
 * `sealed interface` para que el `when` de `onEvent` sea exhaustivo: un evento nuevo sin
 * tratar no compila.
 */
sealed interface ScaffoldEvent {
    data class GymNameChanged(val value: String) : ScaffoldEvent

    data object CreateGymClicked : ScaffoldEvent
}

/**
 * Algo que ocurre una vez y no se puede volver a pintar: navegar, un aviso breve, compartir.
 * Si al girar la pantalla tendría que seguir ahí, no es un efecto: es estado.
 *
 * El efecto dice **qué** pasó (`GymCreated`), no a dónde ir: a dónde lleva lo decide quien
 * monta la navegación.
 */
sealed interface ScaffoldEffect {
    data class GymCreated(val gymId: GymId) : ScaffoldEffect
}
