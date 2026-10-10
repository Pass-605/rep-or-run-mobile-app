package app.reporrun.feature.plansetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El resumen previo a generar: enseña lo que se va a enviar y deja generar solo si no falta nada.
 *
 * **No decide qué falta.** Observa [BuildGenerationSummaryUseCase] y guarda lo que emite tal
 * cual: los bloqueos y `canGenerate` son suyos. Aquí no hay ningún mínimo de máquinas ni de días.
 *
 * A diferencia de los formularios, **sí se observa**: no hay nada a medio escribir que pisar, y
 * al volver de editar el perfil, el gimnasio o las preferencias el resumen ya está al día.
 */
class SummaryViewModel(buildGenerationSummary: BuildGenerationSummaryUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SummaryUiState())
    val state: StateFlow<SummaryUiState> = _state.asStateFlow()

    private val _effects = Channel<SummaryEffect>(Channel.BUFFERED)
    val effects: Flow<SummaryEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            buildGenerationSummary().collect { summary -> _state.update { it.copy(summary = summary) } }
        }
    }

    fun onEvent(event: SummaryEvent) {
        when (event) {
            // El botón ya está deshabilitado si falta algo; esto es por si el estado cambia
            // entre el toque y el evento.
            SummaryEvent.GenerateClicked -> if (_state.value.canGenerate) {
                _effects.trySend(SummaryEffect.GenerationRequested)
            }
        }
    }
}
