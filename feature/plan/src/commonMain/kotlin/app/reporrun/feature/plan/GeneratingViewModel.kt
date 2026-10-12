package app.reporrun.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.GeneratePlanResult
import app.reporrun.core.domain.usecase.GeneratePlanUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Pide el plan al entrar y traduce el resultado a lo que la pantalla puede ofrecer.
 *
 * **No decide qué se puede reintentar:** lo dice el tipo de [GeneratePlanResult]. Lo único suyo
 * es el tope de planes inválidos seguidos ([MAX_INVALID_PLANS_IN_A_ROW]), que vive en memoria:
 * no se guarda y empieza de cero cada vez que se entra a la pantalla.
 *
 * La generación corre en `viewModelScope`: sobrevive a un giro de pantalla y **se cancela** al
 * cancelar o al salir, que es lo que evita que quede un plan que nadie esperaba.
 */
class GeneratingViewModel(private val generatePlan: GeneratePlanUseCase) : ViewModel() {
    private val _state = MutableStateFlow(GeneratingUiState())
    val state: StateFlow<GeneratingUiState> = _state.asStateFlow()

    private val _effects = Channel<GeneratingEffect>(Channel.BUFFERED)
    val effects: Flow<GeneratingEffect> = _effects.receiveAsFlow()

    private var generation: Job? = null
    private var invalidPlansInARow = 0

    /** Ya hay plan guardado o ya se ha pedido salir: lo que llegue después no cuenta. */
    private var finished = false

    init {
        generate()
    }

    fun onEvent(event: GeneratingEvent) {
        when (event) {
            // El pie solo ofrece esto cuando se puede; la comprobación es por si el estado cambia
            // entre el toque y el evento, y para que el tope no dependa de la pantalla.
            GeneratingEvent.RetryClicked -> if (_state.value.failure.canAskAgain()) generate()

            GeneratingEvent.CloseClicked -> if (!finished) {
                finished = true
                generation?.cancel()
                _effects.trySend(GeneratingEffect.BackToSummary)
            }
        }
    }

    private fun generate() {
        if (finished || generation?.isActive == true) return
        _state.value = GeneratingUiState()
        generation = viewModelScope.launch { onResult(generatePlan()) }
    }

    private fun onResult(result: GeneratePlanResult) {
        // Un resultado que llega con la salida ya pedida no navega a ningún sitio.
        if (finished) return
        if (result !is GeneratePlanResult.InvalidPlan) invalidPlansInARow = 0
        when (result) {
            is GeneratePlanResult.Saved -> {
                finished = true
                _effects.trySend(GeneratingEffect.PlanGenerated(result.plan.id))
            }

            // El resumen no deja llegar aquí con algo pendiente. Si pasa, es él quien dice qué falta.
            is GeneratePlanResult.Blocked -> {
                finished = true
                _effects.trySend(GeneratingEffect.BackToSummary)
            }

            is GeneratePlanResult.Unavailable -> fail(GenerationFailure.Unavailable(result.reason))

            is GeneratePlanResult.InvalidPlan -> {
                invalidPlansInARow++
                fail(
                    GenerationFailure.InvalidPlan(
                        problems = result.problems,
                        canGenerateAgain = invalidPlansInARow < MAX_INVALID_PLANS_IN_A_ROW,
                    ),
                )
            }

            is GeneratePlanResult.InvalidInput -> fail(GenerationFailure.InvalidInput(result.problems))
        }
    }

    private fun fail(failure: GenerationFailure) {
        _state.value = GeneratingUiState(failure)
    }
}

/** Si desde este error se puede pedir un plan de nuevo, sea repitiendo o generando otro. */
private fun GenerationFailure?.canAskAgain(): Boolean = when (this) {
    is GenerationFailure.Unavailable -> true
    is GenerationFailure.InvalidPlan -> canGenerateAgain
    is GenerationFailure.InvalidInput, null -> false
}
