package app.reporrun.core.data.mapper

import app.reporrun.core.database.entity.CardioColumns
import app.reporrun.core.database.entity.DoseColumns
import app.reporrun.core.database.entity.PlanDayWithExercises
import app.reporrun.core.database.entity.PlanExerciseWithAlternatives
import app.reporrun.core.database.entity.PlanWithDays
import app.reporrun.core.model.Alternative
import app.reporrun.core.model.CardioPrescription
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.GymId
import app.reporrun.core.model.Metric
import app.reporrun.core.model.MetricRange
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.RirRange
import app.reporrun.core.model.TrialLoad

/*
 * Filas de Room → Plan. Es el único sitio donde se construye un Plan desde datos que el
 * modelo no ha validado (traspaso de B-02): comprueba antes lo que haría lanzar a los
 * constructores y devuelve todos los errores juntos ([Mapped.Invalid]) en lugar de morir en el
 * primero.
 *
 * Patrón en cada nivel: se construyen las piezas por separado con [ModelErrors.build]; si
 * alguna falló, el nivel devuelve `null` sin construirse (el error ya está anotado); si no,
 * se construye con `requireNotNull`, que en ese punto no puede fallar.
 */

/**
 * Reconstruye el plan. Ordena días, ejercicios y alternativas (Room no garantiza el orden de
 * un `@Relation`) y reconstruye el `ExerciseDose` sellado desde `metric`.
 */
internal fun PlanWithDays.toPlan(): Mapped<Plan> {
    val errors = ModelErrors()
    val id = errors.build("plan.id") { PlanId(plan.id) }
    val gymId = errors.build("plan.gymId") { GymId(plan.gymId) }
    val mappedDays = days.sortedBy { it.day.dayIndex }
        .mapIndexed { index, day -> day.toPlanDay("days[$index]", errors) }
    if (errors.size > 0) return Mapped.Invalid(errors.errors)

    val built = errors.build("plan") {
        Plan(
            id = requireNotNull(id),
            gymId = requireNotNull(gymId),
            createdAt = plan.createdAt,
            status = plan.status,
            blockDays = plan.blockDays,
            startDate = plan.startDate,
            reviewDate = plan.reviewDate,
            focus = plan.focus,
            notes = plan.notes,
            days = mappedDays.filterNotNull(),
            inputSnapshotJson = plan.inputSnapshotJson,
            model = plan.model,
            promptVersion = plan.promptVersion,
            schemaVersion = plan.schemaVersion,
        )
    }
    return if (built == null) Mapped.Invalid(errors.errors) else Mapped.Valid(built)
}

private fun PlanDayWithExercises.toPlanDay(path: String, errors: ModelErrors): PlanDay? {
    val before = errors.size
    val id = errors.build("$path.id") { PlanDayId(day.id) }
    val cardio = day.cardio?.let { errors.build("$path.cardio") { it.toPrescription() } }
    val mappedExercises = exercises.sortedBy { it.exercise.order }
        .mapIndexed { index, exercise -> exercise.toPlanExercise("$path.exercises[$index]", errors) }
    if (errors.size > before) return null

    return errors.build(path) {
        PlanDay(
            id = requireNotNull(id),
            dayOfWeek = day.dayOfWeek,
            dayIndex = day.dayIndex,
            session = day.session,
            priority = day.priority,
            cardio = cardio,
            warmup = day.warmup,
            mobility = day.mobility,
            cooldown = day.cooldown,
            exercises = mappedExercises.filterNotNull(),
            status = day.status,
        )
    }
}

private fun CardioColumns.toPrescription() = CardioPrescription(minMinutes, maxMinutes, mode, rpe, speed, incline)

private fun PlanExerciseWithAlternatives.toPlanExercise(path: String, errors: ModelErrors): PlanExercise? {
    val before = errors.size
    val row = exercise
    val id = errors.build("$path.id") { PlanExerciseId(row.id) }
    val code = errors.build("$path.code") { ExerciseCode(row.code) }
    val equipmentId = errors.build("$path.equipmentId") { EquipmentId(row.equipmentId) }
    val trial = errors.build("$path.trial") { TrialLoad(row.trial) }
    if (row.sets < 1) errors.report("$path.sets", "series debe ser >= 1, es ${row.sets}")
    val dose = row.dose.toDose("$path.dose", errors)
    val mappedAlternatives = alternatives.sortedBy { it.position }.mapIndexed { index, alternative ->
        errors.build("$path.alternatives[$index]") {
            Alternative(alternative.name, EquipmentId(alternative.equipmentId), alternative.note)
        }
    }
    if (errors.size > before) return null

    return errors.build(path) {
        PlanExercise(
            id = requireNotNull(id),
            code = requireNotNull(code),
            order = row.order,
            name = row.name,
            equipmentId = requireNotNull(equipmentId),
            sets = row.sets,
            dose = requireNotNull(dose),
            rest = row.rest,
            trial = requireNotNull(trial),
            target = row.target,
            unilateral = row.unilateral,
            alternatives = mappedAlternatives.filterNotNull(),
        )
    }
}

/**
 * Reconstruye el sellado desde `metric` (ver `DoseColumns`). Comprueba el rango y el RIR con
 * [MetricRange.isValid] y [RirRange.isValid] antes de construir, así un rango y un RIR
 * inválidos en el mismo ejercicio salen como dos errores.
 */
internal fun DoseColumns.toDose(path: String, errors: ModelErrors): ExerciseDose? {
    val before = errors.size
    if (!MetricRange.isValid(low, high)) errors.report(path, "rango inválido: $low-$high (1 <= low <= high)")
    val rir = rirOrReport(path, errors)
    if (errors.size > before) return null

    val range = MetricRange(low, high)
    return when (metric) {
        Metric.REPS -> ExerciseDose.Reps(range, rir)
        Metric.METERS -> ExerciseDose.Meters(range)
        Metric.SECONDS -> ExerciseDose.Seconds(range)
    }
}

/**
 * El RIR de estas columnas, o `null` si no hay. Solo con repeticiones, y con los dos extremos
 * o ninguno; cualquier otra combinación se anota como error.
 */
private fun DoseColumns.rirOrReport(path: String, errors: ModelErrors): RirRange? {
    val low = rirLow
    val high = rirHigh
    return when {
        low == null && high == null -> null

        metric != Metric.REPS -> null.also { errors.report(path, "el RIR no aplica a $metric") }

        low == null || high == null ->
            null.also { errors.report(path, "RIR incompleto: rirLow y rirHigh van los dos o ninguno") }

        !RirRange.isValid(low, high) ->
            null.also { errors.report(path, "RIR inválido: $low-$high (${RirRange.BOUNDS} y low <= high)") }

        else -> RirRange(low, high)
    }
}
