// Estado y evento de una pantalla van juntos en `XxxContract.kt`: son su contrato con el
// ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.plan

import app.reporrun.core.domain.usecase.PlanDetails
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanStatus

/**
 * La vista del plan.
 *
 * [details] es lo que da el caso de uso, sin traducir. Mientras [isLoading] no se pinta nada,
 * para que la pantalla no diga «no hay plan» durante un instante; después, `null` es que no lo hay.
 *
 * @property today el día de la semana de hoy, para abrir por él y marcarlo.
 * @property selectedDayId el día que eligió la persona; `null` hasta que elige uno.
 */
data class PlanUiState(
    val today: DayOfWeek,
    val isLoading: Boolean = true,
    val details: PlanDetails? = null,
    val selectedDayId: PlanDayId? = null,
) {
    val plan: Plan?
        get() = details?.plan

    /** Los días con sesión, en el orden del bloque. */
    val days: List<PlanDay>
        get() = plan?.days.orEmpty().sortedBy { it.dayIndex }

    /** El día que se enseña: el elegido; si no se ha elegido, hoy si tiene sesión, y si no, el primero. */
    val selectedDay: PlanDay?
        get() = days.firstOrNull { it.id == selectedDayId }
            ?: days.firstOrNull { it.dayOfWeek == today }
            ?: days.firstOrNull()

    /** Lo que ofrece el pie, o `null` si no hay nada que hacer con este plan. */
    val action: PlanAction?
        get() {
            val plan = plan ?: return null
            return when {
                plan.status == PlanStatus.ACTIVE -> null

                plan.status == PlanStatus.DRAFT && selectedDay?.status == DayStatus.PROPOSED -> PlanAction.ApproveDay

                else -> PlanAction.ActivatePlan(
                    pendingDays = days.filter { it.status != DayStatus.APPROVED }.map { it.dayOfWeek },
                )
            }
        }
}

/** La acción del pie. Solo hay una cada vez. */
sealed interface PlanAction {
    /** Confirmar el día elegido, que está propuesto. */
    data object ApproveDay : PlanAction

    /**
     * Activar el plan. Solo se puede con todos los días aprobados: lo exige el caso de uso.
     *
     * @property pendingDays los días que faltan por aprobar, para decirlo.
     */
    data class ActivatePlan(val pendingDays: List<DayOfWeek>) : PlanAction {
        val enabled: Boolean
            get() = pendingDays.isEmpty()
    }
}

sealed interface PlanEvent {
    data class DaySelected(val dayId: PlanDayId) : PlanEvent

    data object ApproveDayClicked : PlanEvent

    data object ActivatePlanClicked : PlanEvent
}
