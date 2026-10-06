package app.reporrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.usecase.LoadCatalogSeedUseCase
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Por dónde empieza la app. */
sealed interface StartDestination {
    /** El almacenamiento todavía no ha contestado: se pinta solo el fondo. */
    data object Loading : StartDestination

    /** No hay consentimiento guardado: bienvenida y aviso. */
    data object Onboarding : StartDestination

    /** El aviso ya se aceptó en este dispositivo, pero aún no hay perfil: se entra por él. */
    data object Profile : StartDestination

    /** Hay consentimiento y perfil: se entra por los gimnasios, que es el paso siguiente. */
    data object Gyms : StartDestination
}

data class AppUiState(val start: StartDestination = StartDestination.Loading)

/**
 * Decide el destino inicial: el primer paso que falta. La bienvenida solo aparece mientras no
 * haya consentimiento; el perfil, mientras no haya uno guardado; después, los gimnasios, que
 * con ninguno enseñan su estado vacío. El consentimiento va primero aunque haya un perfil.
 *
 * Los dos datos se leen **una vez**, al arrancar, y no se observan. Si el destino inicial
 * cambiara al aceptar o al guardar el perfil, `NavHost` reconstruiría el grafo y competiría con
 * la navegación que disparan los efectos de esas pantallas.
 *
 * Con un gimnasio ya creado el inicio sigue siendo la lista de gimnasios, no su inventario:
 * desde ella «Continuar» lleva a las máquinas del activo, y el arranque no puede saber si el
 * inventario «falta» (cuántas máquinas bastan lo decide el resumen previo a generar).
 *
 * **También carga el catálogo semilla**, en cada arranque: es idempotente, y sin él el inventario
 * no tiene de dónde marcar.
 *
 * No tiene eventos ni efectos porque la raíz no ofrece nada que pulsar.
 */
class AppViewModel(
    observeConsent: ObserveConsentUseCase,
    observeProfile: ObserveProfileUseCase,
    loadCatalogSeed: LoadCatalogSeedUseCase,
    dispatchers: AppDispatchers,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        // Aparte y sin esperar: el destino inicial no depende del catálogo, y el inventario lo
        // observa, así que las máquinas aparecen cuando terminan de cargarse. Fuera del hilo
        // principal: leer la semilla es parsear un JSON justo cuando se pinta el primer fotograma.
        viewModelScope.launch(dispatchers.default) { loadCatalogSeed() }
        viewModelScope.launch {
            val start = when {
                observeConsent().first() == null -> StartDestination.Onboarding
                observeProfile().first() == null -> StartDestination.Profile
                else -> StartDestination.Gyms
            }
            _state.update { it.copy(start = start) }
        }
    }
}
