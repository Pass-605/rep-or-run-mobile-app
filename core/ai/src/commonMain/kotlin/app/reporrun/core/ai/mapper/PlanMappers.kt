package app.reporrun.core.ai.mapper

import app.reporrun.core.ai.contract.AlternativeDto
import app.reporrun.core.ai.contract.GeneratePlanResponse
import app.reporrun.core.ai.contract.MetricDto
import app.reporrun.core.ai.contract.PlanDayDto
import app.reporrun.core.ai.contract.PlanExerciseDto
import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.domain.validation.Mapped
import app.reporrun.core.domain.validation.ModelErrors
import app.reporrun.core.model.Alternative
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.CardioPrescription
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.MetricRange
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.RirRange
import app.reporrun.core.model.TrialLoad
import kotlin.time.Instant

/*
 * Respuesta de generación → dominio. Es el único sitio de `:core:ai` donde se construye un
 * plan desde lo que devolvió el servicio (traspaso de B-02): comprueba antes lo que haría
 * lanzar a los constructores y devuelve todos los errores juntos ([Mapped.Invalid]).
 *
 * Aquí solo se mira cada pieza por separado. Lo que cruza datos —aparatos del inventario, días
 * disponibles, códigos únicos, volumen— es de `PlanValidator`.
 *
 * Patrón en cada nivel, el mismo que `PlanReading.kt` de `:core:data`: se construyen las
 * piezas con [ModelErrors.build]; si alguna falló, el nivel devuelve `null` sin construirse;
 * si no, se construye con `requireNotNull`, que en ese punto no puede fallar.
 */

/**
 * El plan de la respuesta, con identificadores nuevos de [ids] para sus días y ejercicios.
 *
 * @param inputSnapshotJson el cuerpo exacto de la petición que produjo esta respuesta.
 */
internal fun GeneratePlanResponse.toGeneratedPlan(ids: IdGenerator, inputSnapshotJson: String): Mapped<GeneratedPlan> {
    val errors = ModelErrors()
    if (plan.blockDays != BlockPreferences.BLOCK_DAYS_V1) {
        errors.report("plan.blockDays", "v1 solo admite bloques de ${BlockPreferences.BLOCK_DAYS_V1} días")
    }
    val generated = errors.build("generatedAt") { Instant.parse(generatedAt) }
    val days = plan.days.mapIndexed { index, day -> day.toPlanDay("plan.days[$index]", ids, errors) }

    return if (errors.size > 0) {
        Mapped.Invalid(errors.errors)
    } else {
        Mapped.Valid(
            GeneratedPlan(
                blockDays = plan.blockDays,
                focus = plan.focus,
                notes = plan.notes,
                days = days.filterNotNull(),
                model = model,
                promptVersion = promptVersion,
                schemaVersion = schemaVersion,
                generatedAt = requireNotNull(generated),
                inputSnapshotJson = inputSnapshotJson,
            ),
        )
    }
}

private fun PlanDayDto.toPlanDay(path: String, ids: IdGenerator, errors: ModelErrors): PlanDay? {
    val before = errors.size
    val cardio = toCardio("$path.cardio", errors)
    val mapped = exercises.mapIndexed { index, exercise ->
        exercise.toPlanExercise("$path.exercises[$index]", ids, errors)
    }
    if (errors.size > before) return null

    return errors.build(path) {
        PlanDay(
            id = PlanDayId(ids.newId()),
            dayOfWeek = DayOfWeek.valueOf(dayOfWeek.name),
            dayIndex = dayIndex,
            session = session,
            priority = priority,
            cardio = cardio,
            warmup = warmup,
            mobility = mobility,
            cooldown = cooldown,
            exercises = mapped.filterNotNull(),
        )
    }
}

/** Sin minutos no hay cardio ese día; con uno solo de los dos extremos, la respuesta está mal. */
private fun PlanDayDto.toCardio(path: String, errors: ModelErrors): CardioPrescription? = when {
    cardioMin == null && cardioMax == null -> null

    cardioMin == null || cardioMax == null -> {
        errors.report(path, "cardioMin y cardioMax van los dos o ninguno")
        null
    }

    else -> errors.build(path) { CardioPrescription(cardioMin, cardioMax, cardioMode, rpe, speed, incline) }
}

private fun PlanExerciseDto.toPlanExercise(path: String, ids: IdGenerator, errors: ModelErrors): PlanExercise? {
    val before = errors.size
    val exerciseCode = errors.build("$path.code") { ExerciseCode(code) }
    val equipment = errors.build("$path.equipmentId") { EquipmentId(equipmentId) }
    val trialLoad = errors.build("$path.trial") { TrialLoad(trial) }
    if (sets < 1) errors.report("$path.sets", "series debe ser >= 1, es $sets")
    val dose = toDose("$path.dose", errors)
    val mappedAlternatives = alternatives.mapIndexed { index, alternative ->
        errors.build("$path.alternatives[$index]") { alternative.toAlternative() }
    }
    if (errors.size > before) return null

    return errors.build(path) {
        PlanExercise(
            id = PlanExerciseId(ids.newId()),
            code = requireNotNull(exerciseCode),
            order = order,
            name = name,
            equipmentId = requireNotNull(equipment),
            sets = sets,
            dose = requireNotNull(dose),
            rest = rest,
            trial = requireNotNull(trialLoad),
            target = target,
            unilateral = unilateral,
            alternatives = mappedAlternatives.filterNotNull(),
        )
    }
}

private fun AlternativeDto.toAlternative() = Alternative(name, EquipmentId(equipmentId), note)

/**
 * Reconstruye el sellado desde `metric`. Comprueba el rango y el RIR con [MetricRange.isValid]
 * y [RirRange.isValid] antes de construir, así un rango y un RIR inválidos en el mismo
 * ejercicio salen como dos errores.
 */
private fun PlanExerciseDto.toDose(path: String, errors: ModelErrors): ExerciseDose? {
    val before = errors.size
    if (!MetricRange.isValid(low, high)) errors.report(path, "rango inválido: $low-$high (1 <= low <= high)")
    val rir = toRir(path, errors)

    return if (errors.size > before) {
        null
    } else {
        val range = MetricRange(low, high)
        when (metric) {
            MetricDto.REPS -> ExerciseDose.Reps(range, rir)
            MetricDto.METERS -> ExerciseDose.Meters(range)
            MetricDto.SECONDS -> ExerciseDose.Seconds(range)
        }
    }
}

/** El RIR solo existe con repeticiones (plan §4.8): en metros o segundos es un error, no se descarta. */
private fun PlanExerciseDto.toRir(path: String, errors: ModelErrors): RirRange? = when {
    rirLow == null && rirHigh == null -> null

    metric != MetricDto.REPS -> {
        errors.report(path, "el RIR solo aplica a repeticiones, y la métrica es $metric")
        null
    }

    rirLow == null || rirHigh == null -> {
        errors.report(path, "rirLow y rirHigh van los dos o ninguno")
        null
    }

    !RirRange.isValid(rirLow, rirHigh) -> {
        errors.report(path, "RIR inválido: $rirLow-$rirHigh (${RirRange.BOUNDS}, low <= high)")
        null
    }

    else -> RirRange(rirLow, rirHigh)
}
