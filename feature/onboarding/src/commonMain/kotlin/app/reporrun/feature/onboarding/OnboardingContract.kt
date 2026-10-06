// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.onboarding

/**
 * Todo lo que la bienvenida necesita para pintarse, y nada más.
 *
 * - **Inmutable y con valores por defecto**: `OnboardingUiState()` es el primer fotograma.
 * - Los textos del aviso no están aquí: son fijos y salen de `strings.xml`. El estado solo
 *   lleva lo que cambia.
 * - [isSaving] es estado y no un `remember` de la pantalla: sobrevive a un giro porque
 *   sobrevive el ViewModel.
 */
data class OnboardingUiState(val isSaving: Boolean = false)

/**
 * Lo que la persona hizo, no lo que el ViewModel debe hacer: `AcceptClicked`, no
 * `SaveConsent`. La pantalla informa; qué pasa después lo decide el ViewModel.
 *
 * `sealed interface` para que el `when` de `onEvent` sea exhaustivo: un evento nuevo sin
 * tratar no compila.
 */
sealed interface OnboardingEvent {
    data object AcceptClicked : OnboardingEvent

    data object ReadNoticeClicked : OnboardingEvent
}

/**
 * Algo que ocurre una vez y no se puede volver a pintar: navegar, un aviso breve, compartir.
 * Si al girar la pantalla tendría que seguir ahí, no es un efecto: es estado.
 *
 * El efecto dice **qué** pasó (`ConsentAccepted`), no a dónde ir: a dónde lleva lo decide
 * quien monta la navegación.
 */
sealed interface OnboardingEffect {
    data object ConsentAccepted : OnboardingEffect

    data object NoticeRequested : OnboardingEffect
}
