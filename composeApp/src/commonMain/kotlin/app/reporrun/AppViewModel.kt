package app.reporrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Por dónde empieza la app. */
sealed interface StartDestination {
    /** DataStore todavía no ha contestado: se pinta solo el fondo. */
    data object Loading : StartDestination

    /** No hay consentimiento guardado: bienvenida y aviso. */
    data object Onboarding : StartDestination

    /** El aviso ya se aceptó en este dispositivo: se entra por el perfil. */
    data object Main : StartDestination
}

data class AppUiState(val start: StartDestination = StartDestination.Loading)

/**
 * Decide el destino inicial: la bienvenida solo aparece mientras no haya consentimiento.
 *
 * El consentimiento se lee **una vez**, al arrancar, y no se observa. Si el destino inicial
 * cambiara al aceptar, `NavHost` reconstruiría el grafo y competiría con la navegación que
 * dispara el efecto de la bienvenida.
 *
 * No tiene eventos ni efectos porque la raíz no ofrece nada que pulsar.
 */
class AppViewModel(observeConsent: ObserveConsentUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hasConsent = observeConsent().first() != null
            _state.update {
                it.copy(start = if (hasConsent) StartDestination.Main else StartDestination.Onboarding)
            }
        }
    }
}
