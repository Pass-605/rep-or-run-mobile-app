package app.reporrun.feature.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.GeneratePlanResult
import app.reporrun.core.domain.usecase.GeneratePlanUseCase
import app.reporrun.core.domain.usecase.GenerationUnavailable
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Pide el plan al entrar y traduce el resultado a lo que la pantalla puede ofrecer.
 *
 * **No decide qué se puede reintentar:** lo dice el tipo de [GeneratePlanResult]. Lo único suyo
 * es el tope de planes inválidos seguidos ([MAX_INVALID_PLANS_IN_A_ROW]), que vive en memoria:
 * no se guarda y empieza de cero cada vez que se entra a la pantalla.
 *
 * La generación corre en `viewModelScope`: sobrevive a un giro de pantalla y **se cancela** al
 * cancelar o al salir, que es lo que evita que quede un plan que nadie esperaba.
 *
 * **Entrar genera una vez, restaurarse no.** Al arrancar la primera generación se deja una marca
 * en [savedState]. Esa marca sobrevive a la muerte del proceso y no a salir de la pantalla: si
 * al crearse el ViewModel ya está, es que el proceso murió con la espera delante, y pedir otro
 * plan guardaría uno que nadie ha pedido. Entonces no se arranca nada y la persona decide.
 *
 * @param clock con él se cuenta lo que el servicio pide esperar tras un límite de peticiones.
 */
class GeneratingViewModel(
    private val generatePlan: GeneratePlanUseCase,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(GeneratingUiState())
    val state: StateFlow<GeneratingUiState> = _state.asStateFlow()

    private val _effects = Channel<GeneratingEffect>(Channel.BUFFERED)
    val effects: Flow<GeneratingEffect> = _effects.receiveAsFlow()

    private var generation: Job? = null
    private var countdown: Job? = null
    private var invalidPlansInARow = 0

    /** Ya hay plan guardado o ya se ha pedido salir: lo que llegue después no cuenta. */
    private var finished = false

    init {
        if (savedState.get<Boolean>(STARTED_KEY) == true) {
            _state.value = GeneratingUiState(GenerationFailure.Interrupted)
        } else {
            // Antes de pedir nada: si el proceso muere en cuanto sale la petición, la marca ya está.
            savedState[STARTED_KEY] = true
            generate()
        }
    }

    fun onEvent(event: GeneratingEvent) {
        when (event) {
            // El pie solo ofrece esto cuando se puede; la comprobación es por si el estado cambia
            // entre el toque y el evento, y para que el tope no dependa de la pantalla.
            GeneratingEvent.RetryClicked -> if (_state.value.canAskAgain()) generate()

            GeneratingEvent.CloseClicked -> if (!finished) {
                finished = true
                generation?.cancel()
                _effects.trySend(GeneratingEffect.BackToSummary)
            }
        }
    }

    private fun generate() {
        if (finished || generation?.isActive == true) return
        countdown?.cancel()
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

            is GeneratePlanResult.Unavailable -> {
                fail(GenerationFailure.Unavailable(result.reason))
                (result.reason as? GenerationUnavailable.RateLimited)?.retryAfterSeconds?.let(::waitBeforeRetrying)
            }

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

    /**
     * Cuenta hacia atrás lo que pidió el servicio. Lo que falta se calcula cada vez contra la hora
     * a la que se podrá reintentar, no restando uno: un tic que llegue tarde no alarga la espera.
     */
    private fun waitBeforeRetrying(seconds: Int) {
        if (seconds <= 0) return
        val retryAt = clock.now() + seconds.seconds
        _state.update { it.copy(secondsUntilRetry = seconds) }
        countdown = viewModelScope.launch {
            while (true) {
                val left = retryAt - clock.now()
                _state.update { it.copy(secondsUntilRetry = left.wholeSecondsUp()) }
                if (left <= Duration.ZERO) break
                delay(minOf(left, 1.seconds))
            }
        }
    }

    private companion object {
        const val STARTED_KEY = "generation_started"
    }
}

/**
 * Si desde este estado se puede pedir un plan de nuevo, sea repitiendo o generando otro. Mientras
 * dure la espera que pidió el servicio, no.
 */
private fun GeneratingUiState.canAskAgain(): Boolean = canRetryNow &&
    when (failure) {
        is GenerationFailure.Unavailable, GenerationFailure.Interrupted -> true
        is GenerationFailure.InvalidPlan -> failure.canGenerateAgain
        is GenerationFailure.InvalidInput, null -> false
    }

/** Los segundos que quedan, hacia arriba: con medio segundo por delante todavía se enseña «1». */
private fun Duration.wholeSecondsUp(): Int = ((inWholeMilliseconds + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND)
    .toInt()
    .coerceAtLeast(0)

private const val MILLIS_PER_SECOND = 1000L
