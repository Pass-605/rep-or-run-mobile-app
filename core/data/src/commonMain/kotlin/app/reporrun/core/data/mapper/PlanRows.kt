package app.reporrun.core.data.mapper

import app.reporrun.core.database.entity.CardioColumns
import app.reporrun.core.database.entity.DoseColumns
import app.reporrun.core.database.entity.PlanDayEntity
import app.reporrun.core.database.entity.PlanEntity
import app.reporrun.core.database.entity.PlanExerciseAlternativeEntity
import app.reporrun.core.database.entity.PlanExerciseEntity
import app.reporrun.core.model.Alternative
import app.reporrun.core.model.CardioPrescription
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.PlanId

/*
 * Plan → filas de Room. Es directo: un Plan del modelo ya cumple sus invariantes. La vuelta,
 * que no lo es, está en PlanReading.kt.
 */

/** Las filas de un plan, listas para `PlanDao.insertPlan`. */
internal data class PlanRows(
    val plan: PlanEntity,
    val days: List<PlanDayEntity>,
    val exercises: List<PlanExerciseEntity>,
    val alternatives: List<PlanExerciseAlternativeEntity>,
)

internal fun Plan.toRows(): PlanRows {
    val dayRows = days.map { it.toEntity(id) }
    val exerciseRows = days.flatMap { day -> day.exercises.map { it.toEntity(day.id) } }
    val alternativeRows = days.flatMap { day ->
        day.exercises.flatMap { exercise ->
            exercise.alternatives.mapIndexed { position, alternative -> alternative.toEntity(exercise.id, position) }
        }
    }
    return PlanRows(toEntity(), dayRows, exerciseRows, alternativeRows)
}

private fun Plan.toEntity() = PlanEntity(
    id = id.value,
    gymId = gymId.value,
    createdAt = createdAt,
    status = status,
    blockDays = blockDays,
    startDate = startDate,
    reviewDate = reviewDate,
    focus = focus,
    notes = notes,
    inputSnapshotJson = inputSnapshotJson,
    model = model,
    promptVersion = promptVersion,
    schemaVersion = schemaVersion,
)

private fun PlanDay.toEntity(planId: PlanId) = PlanDayEntity(
    id = id.value,
    planId = planId.value,
    dayOfWeek = dayOfWeek,
    dayIndex = dayIndex,
    session = session,
    priority = priority,
    cardio = cardio?.toColumns(),
    warmup = warmup,
    mobility = mobility,
    cooldown = cooldown,
    status = status,
)

/** Un cardio completo o ninguno: con cardio, los minutos nunca van a `NULL`. */
private fun CardioPrescription.toColumns() = CardioColumns(
    minMinutes = minMinutes,
    maxMinutes = maxMinutes,
    mode = mode,
    rpe = rpe,
    speed = speed,
    incline = incline,
)

private fun PlanExercise.toEntity(dayId: PlanDayId) = PlanExerciseEntity(
    id = id.value,
    planDayId = dayId.value,
    code = code.value,
    order = order,
    name = name,
    equipmentId = equipmentId.value,
    sets = sets,
    dose = dose.toColumns(),
    rest = rest,
    trial = trial.text,
    target = target,
    unilateral = unilateral,
)

/** RIR solo en repeticiones que lo prescriben; en metros y segundos, siempre `NULL`. */
private fun ExerciseDose.toColumns(): DoseColumns = when (this) {
    is ExerciseDose.Reps -> DoseColumns(metric, range.low, range.high, rir?.low, rir?.high)
    is ExerciseDose.Meters, is ExerciseDose.Seconds -> DoseColumns(metric, range.low, range.high, null, null)
}

private fun Alternative.toEntity(exerciseId: PlanExerciseId, position: Int) = PlanExerciseAlternativeEntity(
    planExerciseId = exerciseId.value,
    position = position,
    name = name,
    equipmentId = equipmentId.value,
    note = note,
)
