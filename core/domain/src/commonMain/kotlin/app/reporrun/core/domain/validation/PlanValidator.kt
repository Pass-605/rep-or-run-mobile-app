package app.reporrun.core.domain.validation

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanExercise

/**
 * Las reglas del contrato (`contract/v1`, README) que [PlanValidator] comprueba, con su número.
 *
 * Faltan la 3 (rangos, series y RIR) y la 5 (cardio) a propósito: son de un solo campo, los
 * constructores de `:core:model` no dejan expresar una infracción y el mapeo de `:core:ai` las
 * informa antes de construir.
 *
 * @property number el número de la regla en el README del contrato.
 */
enum class PlanRule(val number: Int) {
    /** Cada ejercicio y cada alternativa usa un aparato del inventario. */
    EQUIPMENT_IN_INVENTORY(number = 1),

    /** Los días con sesión están entre los disponibles y no se repiten. */
    AVAILABLE_DAYS(number = 2),

    /** El código es único en el plan y coherente con su día y con su orden. */
    EXERCISE_CODES(number = 4),

    /** El volumen de cada sesión cabe en los minutos de sesión. Ver [SessionVolume]. */
    SESSION_VOLUME(number = 6),
}

/**
 * Una regla incumplida, en un sitio concreto del plan.
 *
 * @property path ruta del campo, como las del mapeo (`plan.days[0].exercises[1].equipmentId`).
 * @property day día de la sesión donde está.
 * @property exerciseCode ejercicio donde está; `null` si la regla es del día entero.
 * @property detail qué falla, con identificadores y cifras del plan; nunca nombres ni notas.
 */
data class PlanViolation(
    val rule: PlanRule,
    val path: String,
    val day: DayOfWeek,
    val exerciseCode: ExerciseCode?,
    val detail: String,
) {
    /**
     * Todo en una línea, apta para un prompt de reparación o para
     * `AiError.InvalidResponse.problems`: ruta, regla, día, ejercicio y qué falla.
     */
    val message: String
        get() = listOfNotNull("regla ${rule.number}", day.name, exerciseCode?.value)
            .joinToString(prefix = "$path: ", postfix = ": $detail")
}

/** Resultado de [PlanValidator.validate]. */
sealed interface PlanValidation {
    /** El plan cumple las reglas y se puede guardar. */
    data object Valid : PlanValidation

    /**
     * El plan no se puede guardar.
     *
     * @property violations todas las infracciones, no solo la primera, por regla y por posición.
     */
    data class Invalid(val violations: List<PlanViolation>) : PlanValidation {
        /** [PlanViolation.message] de cada infracción. */
        val messages: List<String>
            get() = violations.map { it.message }
    }
}

/**
 * Segunda red antes de guardar un plan: comprueba lo que exige cruzar datos —inventario, días
 * disponibles, códigos y volumen— y devuelve todas las infracciones, como valor. Un plan que no
 * pasa nunca llega a Room.
 *
 * Lo que se ve mirando un campo solo (rangos, RIR, series, cardio) ya lo garantiza el modelo.
 */
class PlanValidator {
    /**
     * Valida [days] contra el inventario y las preferencias con los que se generaron.
     *
     * @param equipment el inventario del gimnasio del plan.
     * @param preferences de aquí salen los días disponibles y los minutos por sesión.
     */
    fun validate(days: List<PlanDay>, equipment: List<Equipment>, preferences: BlockPreferences): PlanValidation {
        val violations = equipmentViolations(days, equipment.map { it.id }.toSet()) +
            dayViolations(days, preferences.availableDays) +
            codeViolations(days) +
            volumeViolations(days, preferences.sessionMinutes)

        return if (violations.isEmpty()) PlanValidation.Valid else PlanValidation.Invalid(violations)
    }

    private fun equipmentViolations(days: List<PlanDay>, inventory: Set<EquipmentId>): List<PlanViolation> =
        days.eachExercise { day, exercise, path ->
            val own = PlanViolation(
                rule = PlanRule.EQUIPMENT_IN_INVENTORY,
                path = "$path.equipmentId",
                day = day.dayOfWeek,
                exerciseCode = exercise.code,
                detail = "el aparato ${exercise.equipmentId.value} no está en gym.equipment",
            ).takeIf { exercise.equipmentId !in inventory }

            val alternatives = exercise.alternatives.mapIndexedNotNull { index, alternative ->
                PlanViolation(
                    rule = PlanRule.EQUIPMENT_IN_INVENTORY,
                    path = "$path.alternatives[$index].equipmentId",
                    day = day.dayOfWeek,
                    exerciseCode = exercise.code,
                    detail = "la alternativa usa el aparato ${alternative.equipmentId.value}, " +
                        "que no está en gym.equipment",
                ).takeIf { alternative.equipmentId !in inventory }
            }

            listOfNotNull(own) + alternatives
        }

    private fun dayViolations(days: List<PlanDay>, available: Set<DayOfWeek>): List<PlanViolation> =
        days.flatMapIndexed { index, day ->
            val path = "${dayPath(index)}.dayOfWeek"
            val repeatedAt = days.take(index).indexOfFirst { it.dayOfWeek == day.dayOfWeek }

            listOfNotNull(
                PlanViolation(
                    rule = PlanRule.AVAILABLE_DAYS,
                    path = path,
                    day = day.dayOfWeek,
                    exerciseCode = null,
                    detail = "el día no está en preferences.availableDays",
                ).takeIf { day.dayOfWeek !in available },
                PlanViolation(
                    rule = PlanRule.AVAILABLE_DAYS,
                    path = path,
                    day = day.dayOfWeek,
                    exerciseCode = null,
                    detail = "día repetido: ya tiene sesión en ${dayPath(repeatedAt)}",
                ).takeIf { repeatedAt >= 0 },
            )
        }

    /** Letra, orden y unicidad se comprueban por separado: un código puede fallar en las tres. */
    private fun codeViolations(days: List<PlanDay>): List<PlanViolation> {
        val firstUse = mutableMapOf<ExerciseCode, String>()

        return days.eachExercise { day, exercise, path ->
            val code = exercise.code.value
            val letter = code.dropLast(CODE_DIGITS)
            val number = code.takeLast(CODE_DIGITS).toInt()
            val usedAt = firstUse[exercise.code]
            if (usedAt == null) firstUse[exercise.code] = path

            fun violation(detail: String) =
                PlanViolation(PlanRule.EXERCISE_CODES, "$path.code", day.dayOfWeek, exercise.code, detail)

            listOfNotNull(
                violation(
                    "el código de un ejercicio de ${day.dayOfWeek.name} empieza por «${day.dayOfWeek.codeLetter}»",
                )
                    .takeIf { letter != day.dayOfWeek.codeLetter },
                violation("el código termina en ${code.takeLast(CODE_DIGITS)} y order es ${exercise.order}")
                    .takeIf { number != exercise.order },
                usedAt?.let { violation("código repetido: ya lo usa $it") },
            )
        }
    }

    private fun volumeViolations(days: List<PlanDay>, sessionMinutes: Int): List<PlanViolation> =
        days.mapIndexedNotNull { index, day ->
            PlanViolation(
                rule = PlanRule.SESSION_VOLUME,
                path = dayPath(index),
                day = day.dayOfWeek,
                exerciseCode = null,
                detail = "la sesión se estima en ${minutesUp(SessionVolume.estimatedSeconds(day))} min " +
                    "(${day.effectiveSets} series y ${day.cardio?.minMinutes ?: 0} min de cardio) y el tope son " +
                    "${SessionVolume.limitSeconds(sessionMinutes) / SECONDS_PER_MINUTE} min " +
                    "(sessionMinutes $sessionMinutes + ${SessionVolume.TOLERANCE_PERCENT} %): quita series o cardio",
            ).takeIf { !SessionVolume.fits(day, sessionMinutes) }
        }

    private inline fun List<PlanDay>.eachExercise(
        check: (day: PlanDay, exercise: PlanExercise, path: String) -> List<PlanViolation>,
    ): List<PlanViolation> = flatMapIndexed { dayIndex, day ->
        day.exercises.flatMapIndexed { index, exercise ->
            check(day, exercise, "${dayPath(dayIndex)}.exercises[$index]")
        }
    }

    private fun dayPath(index: Int) = "plan.days[$index]"

    /** Los minutos de la estimación se redondean hacia arriba: 52,5 min no caben en 52. */
    private fun minutesUp(seconds: Int) = (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE

    private companion object {
        /** `ExerciseCode` garantiza que el código acaba en dos dígitos. */
        const val CODE_DIGITS = 2
        const val SECONDS_PER_MINUTE = 60
    }
}
