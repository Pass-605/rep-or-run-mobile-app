package app.reporrun.scaffold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.CreateGymResult
import app.reporrun.core.domain.usecase.CreateGymUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **ViewModel de referencia (S-09).** Las pantallas de C-05 en adelante copian esta forma. No
 * hay clase base a propósito: son pocas líneas y, escritas, se ve qué hace cada una.
 *
 * Qué entra por el constructor:
 * - **Solo casos de uso.** Nunca un repositorio, un DAO ni otro ViewModel.
 * - **Nada de la plataforma ni de Compose**: ni `Context`, ni recursos, ni navegación. Por eso
 *   el test lo construye a mano, sin Koin.
 * - **Ni `Clock` ni dispatchers**, porque aquí no hacen falta: la fecha la sella el caso de
 *   uso con el reloj del grafo, y `viewModelScope` ya corre en el hilo principal. Los casos de
 *   uso se pueden llamar desde ahí (Room trabaja en el dispatcher que le inyectó el grafo). Un
 *   ViewModel que necesite la hora o trabajo propio de CPU pide `Clock` o `AppDispatchers` por
 *   constructor; nunca escribe `Clock.System` ni `Dispatchers.Default`.
 */
class ScaffoldViewModel(observeSummary: BuildGenerationSummaryUseCase, private val createGym: CreateGymUseCase) :
    ViewModel() {
    // Un único estado, y solo este archivo lo escribe. Es un `MutableStateFlow` que se
    // actualiza en el sitio (y no un `combine(...).stateIn(...)`) para que lo que la persona
    // teclea llegue al campo sin pasar por otra corrutina: con ese salto de por medio, un
    // campo de texto puede recibir un valor atrasado y mover el cursor.
    private val _state = MutableStateFlow(ScaffoldUiState())
    val state: StateFlow<ScaffoldUiState> = _state.asStateFlow()

    // `Channel` y no `SharedFlow`: cada efecto se entrega una vez, a un solo colector, y si
    // la pantalla no está escuchando (app en segundo plano) espera en el búfer en vez de
    // perderse. Con un `SharedFlow` sin réplica se perdería; con réplica, se repetiría al
    // girar y se navegaría dos veces.
    private val _effects = Channel<ScaffoldEffect>(Channel.BUFFERED)
    val effects: Flow<ScaffoldEffect> = _effects.receiveAsFlow()

    init {
        // Lo que viene del dominio se observa: cuando Room cambia, la pantalla cambia sin
        // recargar. Se vuelca en el estado con `update { copy(...) }`, que solo toca sus
        // campos y no pisa lo que la persona esté escribiendo en ese momento.
        observeSummary()
            .onEach { summary ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        activeGymName = summary.gym?.name,
                        pendingToGenerate = summary.blockers.size,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * La única puerta de entrada desde la pantalla. El `when` es exhaustivo y cada rama es
     * una línea: o cambia el estado, o llama a una función privada.
     */
    fun onEvent(event: ScaffoldEvent) {
        when (event) {
            // Al corregir el campo se borra su error: ya no describe lo que hay escrito.
            is ScaffoldEvent.GymNameChanged -> _state.update { it.copy(gymName = event.value, gymNameIssue = null) }

            ScaffoldEvent.CreateGymClicked -> submitGym()
        }
    }

    private fun submitGym() {
        // Sin esto, dos toques seguidos crean dos gimnasios. La pantalla además desactiva el
        // botón con `isSaving`, pero la garantía está aquí.
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            // El caso de uso valida y devuelve un resultado sellado: no hay `try/catch` ni
            // validación repetida en el ViewModel. Recibe el texto tal cual; recortarlo es
            // regla del dominio.
            when (val result = createGym(_state.value.gymName)) {
                is CreateGymResult.Created -> {
                    _state.update { it.copy(gymName = "", isSaving = false) }
                    _effects.send(ScaffoldEffect.GymCreated(result.gym.id))
                }

                // Un error esperable es estado: se queda en el campo.
                is CreateGymResult.InvalidName -> _state.update {
                    it.copy(isSaving = false, gymNameIssue = result.issue)
                }
            }
        }
    }
}
