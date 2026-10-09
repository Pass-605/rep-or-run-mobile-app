package app.reporrun.feature.plansetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.ObserveCardioOptionsUseCase
import app.reporrun.core.domain.usecase.ObservePreferencesUseCase
import app.reporrun.core.domain.usecase.PreferencesField
import app.reporrun.core.domain.usecase.PreferencesInput
import app.reporrun.core.domain.usecase.SavePreferencesResult
import app.reporrun.core.domain.usecase.SavePreferencesUseCase
import app.reporrun.core.model.BlockPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Las preferencias del bloque: días, minutos, fecha de inicio, cardio y ejercicios a evitar.
 *
 * **No decide qué es válido.** Convierte lo que hay en el formulario en un [PreferencesInput] y
 * se lo da a [SavePreferencesUseCase], que es quien exige al menos un día disponible y el resto
 * de límites del contrato. Aquí no hay ningún número de esos.
 *
 * Las preferencias guardadas se leen **una vez**, como el perfil. Las opciones de cardio, en
 * cambio, **sí se observan**: salen del inventario del gimnasio activo
 * ([ObserveCardioOptionsUseCase]) y un modo elegido que deja de estar en él se desmarca.
 *
 * El patrón (estado en el sitio, efectos por `Channel`, `onEvent` como única entrada) es el de
 * `OnboardingViewModel`, que explica el porqué de cada pieza.
 */
class PreferencesViewModel(
    observePreferences: ObservePreferencesUseCase,
    observeCardioOptions: ObserveCardioOptionsUseCase,
    private val savePreferences: SavePreferencesUseCase,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {
    private val _state = MutableStateFlow(PreferencesUiState())
    val state: StateFlow<PreferencesUiState> = _state.asStateFlow()

    private val _effects = Channel<PreferencesEffect>(Channel.BUFFERED)
    val effects: Flow<PreferencesEffect> = _effects.receiveAsFlow()

    init {
        val today = clock.now().toLocalDateTime(timeZone).date
        viewModelScope.launch {
            // Una vez y sin observar: si se siguiera recogiendo, guardar pisaría lo que la
            // persona tuviera a medio escribir.
            val saved = observePreferences().first()
            _state.update { (saved?.toUiState(today) ?: it).copy(startDateOptions = startDateOptions(today)) }

            observeCardioOptions().collect { options ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        cardioOptions = options,
                        // Sin aparato en el inventario no hay modo que preferir.
                        cardioModes = it.cardioModes.filterTo(mutableSetOf()) { mode -> mode in options },
                    )
                }
            }
        }
    }

    fun onEvent(event: PreferencesEvent) {
        when (event) {
            // El error de un campo se va en cuanto se toca: vuelve, si sigue mal, al guardar.
            is PreferencesEvent.DayToggled -> _state.update {
                it.copy(
                    availableDays = it.availableDays.toggle(event.day),
                    errors = it.errors - PreferencesField.AVAILABLE_DAYS,
                )
            }

            is PreferencesEvent.SessionMinutesChanged -> _state.update {
                it.copy(
                    sessionMinutes = event.value.asMinutesInput(),
                    errors = it.errors - PreferencesField.SESSION_MINUTES,
                )
            }

            is PreferencesEvent.StartDateSelected -> _state.update { it.copy(startDate = event.date) }

            is PreferencesEvent.CardioModeToggled -> _state.update {
                it.copy(
                    cardioModes = it.cardioModes.toggle(event.mode),
                    errors = it.errors - PreferencesField.CARDIO_MODES,
                )
            }

            is PreferencesEvent.AvoidExercisesChanged -> _state.update {
                it.copy(avoidExercises = event.value, errors = it.errors - PreferencesField.AVOID_EXERCISES)
            }

            PreferencesEvent.SaveClicked -> save()
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            when (val result = savePreferences(current.toInput())) {
                // `isSaving` no vuelve a `false`: la pantalla se va.
                is SavePreferencesResult.Saved -> _effects.send(PreferencesEffect.PreferencesSaved)

                is SavePreferencesResult.Invalid -> _state.update {
                    it.copy(isSaving = false, errors = result.errors.associate { error -> error.field to error.issue })
                }
            }
        }
    }
}

/** Cuántas fechas de inicio se ofrecen, contando hoy. No es un límite del contrato. */
private const val START_DATE_OPTIONS = 14

private const val MAX_MINUTES_DIGITS = 3

private fun startDateOptions(today: LocalDate): List<LocalDate> =
    List(START_DATE_OPTIONS) { today.plus(it, DateTimeUnit.DAY) }

/** Deja solo dígitos. No valida: el rango es del caso de uso. */
private fun String.asMinutesInput(): String = filter(Char::isDigit).take(MAX_MINUTES_DIGITS)

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

/**
 * Una fecha de inicio guardada que ya pasó no se precarga: el bloque que se va a pedir no puede
 * empezar ayer.
 */
private fun BlockPreferences.toUiState(today: LocalDate) = PreferencesUiState(
    availableDays = availableDays,
    sessionMinutes = sessionMinutes.toString(),
    blockDays = blockDays,
    startDate = startDate?.takeIf { it >= today },
    cardioModes = cardioModes.toSet(),
    avoidExercises = avoidExercises,
)

/** Lo que hay en el formulario, como lo pide el caso de uso. Un campo vacío es «sin dato». */
private fun PreferencesUiState.toInput() = PreferencesInput(
    availableDays = availableDays,
    sessionMinutes = sessionMinutes.toIntOrNull(),
    startDate = startDate,
    // En el orden en que se ofrecen, no en el que se tocaron.
    cardioModes = cardioOptions.filter { it in cardioModes },
    avoidExercises = avoidExercises,
    blockDays = blockDays,
)
