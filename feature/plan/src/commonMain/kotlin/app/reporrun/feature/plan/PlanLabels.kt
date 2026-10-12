package app.reporrun.feature.plan

import androidx.compose.runtime.Composable
import app.reporrun.core.model.CardioPrescription
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.MetricRange
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanStatus
import app.reporrun.feature.plan.resources.Res
import app.reporrun.feature.plan.resources.plan_activate_pending
import app.reporrun.feature.plan.resources.plan_cardio_incline
import app.reporrun.feature.plan.resources.plan_cardio_minutes
import app.reporrun.feature.plan.resources.plan_cardio_minutes_range
import app.reporrun.feature.plan.resources.plan_cardio_none
import app.reporrun.feature.plan.resources.plan_cardio_rpe
import app.reporrun.feature.plan.resources.plan_cardio_separator
import app.reporrun.feature.plan.resources.plan_day_counts
import app.reporrun.feature.plan.resources.plan_day_description
import app.reporrun.feature.plan.resources.plan_day_exercises
import app.reporrun.feature.plan.resources.plan_day_friday
import app.reporrun.feature.plan.resources.plan_day_friday_name
import app.reporrun.feature.plan.resources.plan_day_monday
import app.reporrun.feature.plan.resources.plan_day_monday_name
import app.reporrun.feature.plan.resources.plan_day_saturday
import app.reporrun.feature.plan.resources.plan_day_saturday_name
import app.reporrun.feature.plan.resources.plan_day_sets
import app.reporrun.feature.plan.resources.plan_day_status_approved
import app.reporrun.feature.plan.resources.plan_day_status_proposed
import app.reporrun.feature.plan.resources.plan_day_sunday
import app.reporrun.feature.plan.resources.plan_day_sunday_name
import app.reporrun.feature.plan.resources.plan_day_thursday
import app.reporrun.feature.plan.resources.plan_day_thursday_name
import app.reporrun.feature.plan.resources.plan_day_today
import app.reporrun.feature.plan.resources.plan_day_tuesday
import app.reporrun.feature.plan.resources.plan_day_tuesday_name
import app.reporrun.feature.plan.resources.plan_day_wednesday
import app.reporrun.feature.plan.resources.plan_day_wednesday_name
import app.reporrun.feature.plan.resources.plan_exercise_alternatives
import app.reporrun.feature.plan.resources.plan_exercise_meters
import app.reporrun.feature.plan.resources.plan_exercise_range
import app.reporrun.feature.plan.resources.plan_exercise_reps
import app.reporrun.feature.plan.resources.plan_exercise_rest
import app.reporrun.feature.plan.resources.plan_exercise_rir
import app.reporrun.feature.plan.resources.plan_exercise_seconds
import app.reporrun.feature.plan.resources.plan_exercise_separator
import app.reporrun.feature.plan.resources.plan_focus_separator
import app.reporrun.feature.plan.resources.plan_pending_separator
import app.reporrun.feature.plan.resources.plan_status_active
import app.reporrun.feature.plan.resources.plan_status_archived
import app.reporrun.feature.plan.resources.plan_status_draft
import app.reporrun.feature.plan.resources.plan_version
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Del plan a sus textos. Los `when` son exhaustivos sin `else`: un valor nuevo en un enum no
 * compila hasta que tiene su texto.
 *
 * Aquí no se calcula ni se convierte nada: la carga de ensayo, el descanso, el RPE, la velocidad
 * y la inclinación van tal como los escribe el plan.
 */

/** La abreviatura del chip de un día: «Mié». */
internal val DayOfWeek.shortLabel: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.plan_day_monday
        DayOfWeek.TUESDAY -> Res.string.plan_day_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.plan_day_wednesday
        DayOfWeek.THURSDAY -> Res.string.plan_day_thursday
        DayOfWeek.FRIDAY -> Res.string.plan_day_friday
        DayOfWeek.SATURDAY -> Res.string.plan_day_saturday
        DayOfWeek.SUNDAY -> Res.string.plan_day_sunday
    }

/** El nombre entero, en minúscula. */
internal val DayOfWeek.fullName: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.plan_day_monday_name
        DayOfWeek.TUESDAY -> Res.string.plan_day_tuesday_name
        DayOfWeek.WEDNESDAY -> Res.string.plan_day_wednesday_name
        DayOfWeek.THURSDAY -> Res.string.plan_day_thursday_name
        DayOfWeek.FRIDAY -> Res.string.plan_day_friday_name
        DayOfWeek.SATURDAY -> Res.string.plan_day_saturday_name
        DayOfWeek.SUNDAY -> Res.string.plan_day_sunday_name
    }

internal val PlanStatus.label: StringResource
    get() = when (this) {
        PlanStatus.DRAFT -> Res.string.plan_status_draft
        PlanStatus.ACTIVE -> Res.string.plan_status_active
        PlanStatus.ARCHIVED -> Res.string.plan_status_archived
    }

internal val DayStatus.label: StringResource
    get() = when (this) {
        DayStatus.PROPOSED -> Res.string.plan_day_status_proposed
        DayStatus.APPROVED -> Res.string.plan_day_status_approved
    }

/** «Versión 2 · bloque de 14 días». */
@Composable
internal fun Plan.versionLine(): String = stringResource(Res.string.plan_version, version, blockDays)

/** «Pecho · Espalda · Cuádriceps», o `null` si el plan no trae enfoque. */
@Composable
internal fun Plan.focusLine(): String? =
    focus.takeIf { it.isNotEmpty() }?.joinToString(stringResource(Res.string.plan_focus_separator))

/** Lo que anuncia el chip de un día: «miércoles, propuesto». */
@Composable
internal fun PlanDay.chipDescription(): String = stringResource(
    Res.string.plan_day_description,
    stringResource(dayOfWeek.fullName),
    stringResource(status.label).lowercase(),
)

/** El rótulo del héroe: «miércoles», o «miércoles · hoy». La mayúscula la pone quien lo pinta. */
@Composable
internal fun PlanDay.heading(today: DayOfWeek): String {
    val name = stringResource(dayOfWeek.fullName)
    return if (dayOfWeek == today) stringResource(Res.string.plan_day_today, name) else name
}

/**
 * «6 ejercicios · 18 series». Las series son las efectivas del modelo: un unilateral no se
 * duplica y las alternativas no suman.
 */
@Composable
internal fun PlanDay.countsLine(): String = stringResource(
    Res.string.plan_day_counts,
    pluralStringResource(Res.plurals.plan_day_exercises, exercises.size, exercises.size),
    pluralStringResource(Res.plurals.plan_day_sets, effectiveSets, effectiveSets),
)

/** «15-20 min · Caminadora · RPE 5-6 · 4,8-5,5 km/h · inclinación 5-7%», o que no hay cardio. */
@Composable
internal fun CardioPrescription?.line(): String {
    if (this == null) return stringResource(Res.string.plan_cardio_none)
    val minutes = if (minMinutes == maxMinutes) {
        stringResource(Res.string.plan_cardio_minutes, minMinutes)
    } else {
        stringResource(Res.string.plan_cardio_minutes_range, minMinutes, maxMinutes)
    }
    return listOfNotNull(
        minutes,
        mode,
        rpe?.let { stringResource(Res.string.plan_cardio_rpe, it) },
        speed,
        incline?.let { stringResource(Res.string.plan_cardio_incline, it) },
    ).joinToString(stringResource(Res.string.plan_cardio_separator))
}

@Composable
private fun rangeText(low: Int, high: Int): String =
    if (low == high) low.toString() else stringResource(Res.string.plan_exercise_range, low, high)

@Composable
private fun MetricRange.text(): String = rangeText(low, high)

/**
 * «3 × 6-10 · RIR 2 · descanso 2,5-3 min». En metros o segundos el rango lleva su unidad y no
 * hay RIR: el tipo no deja expresarlo.
 */
@Composable
internal fun PlanExercise.doseLine(): String {
    val volume = when (val dose = dose) {
        is ExerciseDose.Reps -> stringResource(Res.string.plan_exercise_reps, sets, dose.range.text())
        is ExerciseDose.Meters -> stringResource(Res.string.plan_exercise_meters, sets, dose.range.text())
        is ExerciseDose.Seconds -> stringResource(Res.string.plan_exercise_seconds, sets, dose.range.text())
    }
    val rir = (dose as? ExerciseDose.Reps)?.rir?.let {
        stringResource(Res.string.plan_exercise_rir, rangeText(it.low, it.high))
    }
    return listOfNotNull(volume, rir, stringResource(Res.string.plan_exercise_rest, rest))
        .joinToString(stringResource(Res.string.plan_exercise_separator))
}

/** «O elige una: Press con mancuernas · Fondos», o `null` si el ejercicio no trae alternativas. */
@Composable
internal fun PlanExercise.alternativesLine(): String? {
    if (alternatives.isEmpty()) return null
    val names = alternatives.joinToString(stringResource(Res.string.plan_exercise_separator)) { it.name }
    return stringResource(Res.string.plan_exercise_alternatives, names)
}

/** «Para activarlo falta aprobar: miércoles, sábado.», o `null` si no falta ninguno. */
@Composable
internal fun PlanAction.ActivatePlan.pendingHint(): String? {
    if (enabled) return null
    val names = pendingDays.map { stringResource(it.fullName) }
    return stringResource(
        Res.string.plan_activate_pending,
        names.joinToString(stringResource(Res.string.plan_pending_separator)),
    )
}
