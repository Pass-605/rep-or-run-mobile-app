package app.reporrun.feature.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.ActivatePlanUseCase
import app.reporrun.core.domain.usecase.ApprovePlanDayUseCase
import app.reporrun.core.domain.usecase.ObservePlanUseCase
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.PlanId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * La vista del plan: lo observa y deja aprobar sus días y activarlo.
 *
 * **Qué plan** lo dice la ruta: con id, ese (el que se acaba de generar); sin id, el activo del
 * gimnasio activo, y si no hay, el estado vacío.
 *
 * **No decide las transiciones.** Qué se puede aprobar y cuándo se puede activar lo dicen
 * [ApprovePlanDayUseCase] y [ActivatePlanUseCase]; el pie solo ofrece lo que el estado observado
 * deja hacer. Un resultado distinto del esperado es una carrera (dos toques, un plan que ya
 * cambió) y no hay nada que contar: el plan observado ya lo refleja.
 *
 * No tiene efectos: la pantalla no navega por nada que se decida aquí.
 *
 * @param clock con [timeZone], de ellos sale qué día de la semana es hoy. Se mira una vez, al abrir.
 */
class PlanViewModel(
    observePlan: ObservePlanUseCase,
    private val approvePlanDay: ApprovePlanDayUseCase,
    private val activatePlan: ActivatePlanUseCase,
    clock: Clock,
    timeZone: TimeZone,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(PlanUiState(today = clock.weekdayIn(timeZone)))
    val state: StateFlow<PlanUiState> = _state.asStateFlow()

    init {
        val planId = savedState.get<String>(PLAN_ID_ARG)?.let(::PlanId)
        val plan = if (planId != null) observePlan(planId) else observePlan.active()
        viewModelScope.launch {
            plan.collect { details -> _state.update { it.copy(isLoading = false, details = details) } }
        }
    }

    fun onEvent(event: PlanEvent) {
        when (event) {
            is PlanEvent.DaySelected -> _state.update { it.copy(selectedDayId = event.dayId) }
            PlanEvent.ApproveDayClicked -> approveSelectedDay()
            PlanEvent.ActivatePlanClicked -> activate()
        }
    }

    // El pie solo ofrece cada acción cuando se puede; las comprobaciones son por si el estado
    // cambia entre el toque y el evento.

    private fun approveSelectedDay() {
        val state = _state.value
        val plan = state.plan
        val day = state.selectedDay
        if (state.action != PlanAction.ApproveDay || plan == null || day == null) return
        viewModelScope.launch { approvePlanDay(plan.id, day.id) }
    }

    private fun activate() {
        val state = _state.value
        val plan = state.plan
        if ((state.action as? PlanAction.ActivatePlan)?.enabled != true || plan == null) return
        viewModelScope.launch { activatePlan(plan.id) }
    }
}

// Los dos enums empiezan en lunes: el de kotlinx-datetime y el del contrato.
private fun Clock.weekdayIn(timeZone: TimeZone): DayOfWeek =
    DayOfWeek.entries[now().toLocalDateTime(timeZone).dayOfWeek.ordinal]
