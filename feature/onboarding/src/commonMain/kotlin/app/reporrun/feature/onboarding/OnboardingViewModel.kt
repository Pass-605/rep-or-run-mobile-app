package app.reporrun.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.AcceptConsentUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **ViewModel de referencia.** Copia la forma del de S-09 y lo sustituye como modelo: las
 * pantallas de C-06 en adelante copian esta. No hay clase base a propósito: son pocas líneas
 * y, escritas, se ve qué hace cada una.
 *
 * Qué entra por el constructor:
 * - **Solo casos de uso.** Nunca un repositorio, un DAO ni otro ViewModel.
 * - **Nada de la plataforma ni de Compose**: ni `Context`, ni recursos, ni navegación. Por eso
 *   el test lo construye a mano, sin Koin.
 * - **Ni `Clock` ni dispatchers**, porque aquí no hacen falta: la fecha la sella el caso de
 *   uso con el reloj del grafo, y `viewModelScope` ya corre en el hilo principal. Los casos de
 *   uso se pueden llamar desde ahí (DataStore y Room trabajan en el dispatcher que les inyectó
 *   el grafo). Un ViewModel que necesite la hora o trabajo propio de CPU pide `Clock` o
 *   `AppDispatchers` por constructor; nunca escribe `Clock.System` ni `Dispatchers.Default`.
 *
 * Esta pantalla no observa nada del dominio: quien decide si se muestra es la raíz. Una que sí
 * lo haga recoge el `Flow` del caso de uso en `init` y lo vuelca con `update { copy(...) }`,
 * que solo toca sus campos y no pisa lo que la persona esté escribiendo.
 */
class OnboardingViewModel(private val acceptConsent: AcceptConsentUseCase) : ViewModel() {
    // Un único estado, y solo este archivo lo escribe. Es un `MutableStateFlow` que se
    // actualiza en el sitio (y no un `combine(...).stateIn(...)`) para que lo que la persona
    // teclea llegue al campo sin pasar por otra corrutina: con ese salto de por medio, un
    // campo de texto puede recibir un valor atrasado y mover el cursor.
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    // `Channel` y no `SharedFlow`: cada efecto se entrega una vez, a un solo colector, y si
    // la pantalla no está escuchando (app en segundo plano) espera en el búfer en vez de
    // perderse. Con un `SharedFlow` sin réplica se perdería; con réplica, se repetiría al
    // girar y se navegaría dos veces.
    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    /**
     * La única puerta de entrada desde la pantalla. El `when` es exhaustivo y cada rama es
     * una línea: o cambia el estado, o llama a una función privada.
     */
    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.AcceptClicked -> accept()

            // `trySend`: el búfer no se llena con toques, y así no hace falta una corrutina.
            OnboardingEvent.ReadNoticeClicked -> _effects.trySend(OnboardingEffect.NoticeRequested)
        }
    }

    private fun accept() {
        // Sin esto, dos toques seguidos navegan dos veces. La pantalla además desactiva el
        // botón con `isSaving`, pero la garantía está aquí.
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            acceptConsent()
            // El efecto sale después de guardar: al llegar al siguiente destino, el
            // consentimiento ya está en DataStore.
            _effects.send(OnboardingEffect.ConsentAccepted)
            // `isSaving` no vuelve a `false`: la bienvenida se va y no se acepta dos veces.
        }
    }
}
