package app.reporrun.core.domain.validation

import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanExercise

/**
 * Cuánto dura una sesión, estimado, y cuánto puede durar: la heurística de la regla 6 del
 * contrato («series × descanso estimado + cardio»).
 *
 * ```text
 * estimación = Σ ejercicios [ series × (descanso + trabajo × lados) ] + cardio mínimo
 * tope       = sessionMinutes × 1,10
 * ```
 *
 * Supuestos:
 * - Una serie son [REST_SECONDS_PER_SET] de descanso y [WORK_SECONDS_PER_SIDE] de trabajo. El
 *   descanso no se lee del texto del ejercicio: es libre («2,5-3 min», «90 s»).
 * - En un unilateral las series son por lado: el trabajo se hace dos veces, el descanso una.
 * - Las alternativas son «elige una» y no tienen series: no suman.
 * - El cardio cuenta por su mínimo: el rango es la holgura que da el propio plan.
 * - El calentamiento, la movilidad y la vuelta a la calma no cuentan. La estimación es un suelo
 *   y la regla solo rechaza lo que no cabe ni sin calentar.
 * - [TOLERANCE_PERCENT] de margen, elegido para sesiones de 60 minutos: deja pasar 20 series
 *   con 15 min de cardio (65 min) y rechaza 22 (70 min).
 */
object SessionVolume {
    /** Descanso estimado después de cada serie. */
    const val REST_SECONDS_PER_SET: Int = 120

    /** Lo que dura hacer una serie, por lado. */
    const val WORK_SECONDS_PER_SIDE: Int = 30

    /** Cuánto puede pasarse la estimación de los minutos de sesión. */
    const val TOLERANCE_PERCENT: Int = 10

    private const val SECONDS_PER_MINUTE = 60
    private const val PERCENT = 100
    private const val SIDES_UNILATERAL = 2

    /** Duración estimada de [day], en segundos. */
    fun estimatedSeconds(day: PlanDay): Int =
        day.exercises.sumOf { estimatedSeconds(it) } + (day.cardio?.minMinutes ?: 0) * SECONDS_PER_MINUTE

    /** Lo más que puede durar una sesión de [sessionMinutes], con el margen, en segundos. */
    fun limitSeconds(sessionMinutes: Int): Int =
        sessionMinutes * SECONDS_PER_MINUTE * (PERCENT + TOLERANCE_PERCENT) / PERCENT

    /** Si [day] cabe en una sesión de [sessionMinutes]. */
    fun fits(day: PlanDay, sessionMinutes: Int): Boolean = estimatedSeconds(day) <= limitSeconds(sessionMinutes)

    private fun estimatedSeconds(exercise: PlanExercise): Int {
        val sides = if (exercise.unilateral) SIDES_UNILATERAL else 1
        return exercise.effectiveSets * (REST_SECONDS_PER_SET + WORK_SECONDS_PER_SIDE * sides)
    }
}
