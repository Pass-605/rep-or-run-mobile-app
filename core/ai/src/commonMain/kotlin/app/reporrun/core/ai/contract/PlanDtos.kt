package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable

/*
 * `plan.schema.json`: lo que devuelve el modelo. El schema es estricto, así que todos los
 * campos son obligatorios, también los anulables. Nada de lo que llega aquí es de fiar hasta
 * pasar por `PlanValidator`.
 */

/** El plan de un bloque. */
@Serializable
data class PlanDto(val blockDays: Int, val focus: List<String>, val notes: List<String>, val days: List<PlanDayDto>)

/** `PlanDay`: un día con sesión. */
@Serializable
data class PlanDayDto(
    val dayOfWeek: DayOfWeekDto,
    val dayIndex: Int,
    val session: String,
    val priority: String,
    val cardioMin: Int?,
    val cardioMax: Int?,
    val cardioMode: String?,
    val rpe: String?,
    val speed: String?,
    val incline: String?,
    val warmup: String,
    val mobility: String,
    val cooldown: String,
    val exercises: List<PlanExerciseDto>,
)

/** `PlanExercise`. [trial] es la carga de ensayo como texto; no hay kilos prescritos. */
@Serializable
data class PlanExerciseDto(
    val code: String,
    val order: Int,
    val name: String,
    val equipmentId: String,
    val sets: Int,
    val low: Int,
    val high: Int,
    val rirLow: Int?,
    val rirHigh: Int?,
    val rest: String,
    val trial: String,
    val target: String,
    val unilateral: Boolean,
    val metric: MetricDto,
    val alternatives: List<AlternativeDto>,
)

/** `Alternative`: una opción equivalente. No tiene series propias. */
@Serializable
data class AlternativeDto(val name: String, val equipmentId: String, val note: String?)
