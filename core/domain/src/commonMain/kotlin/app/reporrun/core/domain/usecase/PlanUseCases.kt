package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Un plan con los aparatos de su gimnasio. Un ejercicio guarda el id de su aparato; el nombre
 * que se lee sale de [equipment].
 */
data class PlanDetails(val plan: Plan, val equipment: List<Equipment>) {
    /** El nombre del aparato [id], o `null` si ya no está en el inventario. */
    fun equipmentName(id: EquipmentId): String? = equipment.firstOrNull { it.id == id }?.name
}

/**
 * Un plan para leerlo, al día: cambia cuando se aprueba un día o se activa.
 *
 * Hay dos formas de pedirlo porque hay dos formas de llegar a él: con el id de un plan concreto
 * (el que se acaba de generar) o sin ninguno, y entonces es el activo.
 */
class ObservePlanUseCase(private val planRepository: PlanRepository, private val gymRepository: GymRepository) {
    /** El plan [planId]. Emite `null` si no existe. */
    operator fun invoke(planId: PlanId): Flow<PlanDetails?> = planRepository.observePlan(planId).withEquipment()

    /** El plan activo del gimnasio activo. Emite `null` si no hay gimnasio activo o no tiene plan activo. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun active(): Flow<PlanDetails?> = gymRepository.observeActiveGym().flatMapLatest { gym ->
        if (gym == null) flowOf(null) else planRepository.observeActivePlan(gym.id).withEquipment()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Plan?>.withEquipment(): Flow<PlanDetails?> = flatMapLatest { plan ->
        if (plan == null) {
            flowOf(null)
        } else {
            gymRepository.observeEquipment(plan.gymId).map { equipment -> PlanDetails(plan, equipment) }
        }
    }
}

/** Resultado de [ApprovePlanDayUseCase]. */
sealed interface ApprovePlanDayResult {
    /** El día pasó de propuesto a aprobado. */
    data object Approved : ApprovePlanDayResult

    /** El día ya estaba aprobado; no cambió nada. */
    data object AlreadyApproved : ApprovePlanDayResult

    /** El plan no es un borrador: sus días ya no se confirman. */
    data object PlanNotDraft : ApprovePlanDayResult

    /** Ese plan no existe o no tiene ese día. */
    data object DayNotFound : ApprovePlanDayResult
}

/**
 * La persona confirma un día de un borrador (plan §4.11): de [DayStatus.PROPOSED] a
 * [DayStatus.APPROVED]. Solo cambia el estado de ese día; el plan sigue siendo el mismo.
 *
 * **No hay vuelta atrás:** ningún caso de uso devuelve un día a propuesto.
 */
class ApprovePlanDayUseCase(private val repository: PlanRepository) {
    suspend operator fun invoke(planId: PlanId, dayId: PlanDayId): ApprovePlanDayResult {
        val plan = repository.observePlan(planId).first()
        val day = plan?.days?.firstOrNull { it.id == dayId }
        return when {
            plan == null || day == null -> ApprovePlanDayResult.DayNotFound

            day.status == DayStatus.APPROVED -> ApprovePlanDayResult.AlreadyApproved

            plan.status != PlanStatus.DRAFT -> ApprovePlanDayResult.PlanNotDraft

            else -> {
                repository.setDayStatus(dayId, DayStatus.APPROVED)
                ApprovePlanDayResult.Approved
            }
        }
    }
}

/** Resultado de [ActivatePlanUseCase]. */
sealed interface ActivatePlanResult {
    /** El plan es ahora el único activo de su gimnasio. */
    data object Activated : ActivatePlanResult

    /** Ya era el activo; no cambió nada. */
    data object AlreadyActive : ActivatePlanResult

    /** No se activó: quedan [pending] días sin aprobar. */
    data class DaysPending(val pending: Int) : ActivatePlanResult

    /** Ese plan no existe. */
    data object PlanNotFound : ActivatePlanResult
}

/**
 * Activa un plan cuando la persona ha aprobado **todos** sus días: activar es lo que cambia el
 * plan vigente, y nada lo cambia sin aprobación (plan §4.12).
 *
 * Vale para un borrador y para una versión archivada, que ya fue activa y por eso tiene todos
 * sus días aprobados.
 *
 * **Solo orquesta.** Que haya un único plan activo por gimnasio lo garantiza
 * [PlanRepository.activatePlan], que archiva el anterior en la misma transacción; aquí no se
 * archiva nada.
 */
class ActivatePlanUseCase(private val repository: PlanRepository) {
    suspend operator fun invoke(planId: PlanId): ActivatePlanResult {
        val plan = repository.observePlan(planId).first()
        val pending = plan?.days.orEmpty().count { it.status != DayStatus.APPROVED }
        return when {
            plan == null -> ActivatePlanResult.PlanNotFound

            plan.status == PlanStatus.ACTIVE -> ActivatePlanResult.AlreadyActive

            pending > 0 -> ActivatePlanResult.DaysPending(pending)

            else -> {
                repository.activatePlan(planId)
                ActivatePlanResult.Activated
            }
        }
    }
}

/**
 * Si el gimnasio activo tiene algún plan guardado, en cualquier estado. Es lo que decide si la
 * app se abre por la vista del plan.
 */
class HasSavedPlansUseCase(private val planRepository: PlanRepository, private val gymRepository: GymRepository) {
    suspend operator fun invoke(): Boolean {
        val gym = gymRepository.observeActiveGym().first() ?: return false
        return planRepository.observeHistory(gym.id).first().isNotEmpty()
    }
}
