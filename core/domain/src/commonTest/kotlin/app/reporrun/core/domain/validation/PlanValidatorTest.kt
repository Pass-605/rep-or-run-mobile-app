package app.reporrun.core.domain.validation

import app.reporrun.core.domain.usecase.inventory
import app.reporrun.core.domain.usecase.preferences
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlanValidatorTest {
    private val validator = PlanValidator()

    @Test
    fun acceptsAPlanThatFollowsEveryRule() {
        val days = listOf(
            day(DayOfWeek.MONDAY, exercise("L01", 1, alternatives = listOf("eq_2")), exercise("L02", 2, "eq_3")),
            day(DayOfWeek.THURSDAY, exercise("J01", 1, unilateral = true), cardio = cardio(10, 15)),
        )

        assertEquals(PlanValidation.Valid, validate(days))
    }

    // ------------------------------------------------------------ regla 1: inventario

    @Test
    fun rejectsExerciseWithUnknownEquipment() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1), exercise("L02", 2, "eq_ajeno")))

        val violation = violations(days).single()

        assertEquals(PlanRule.EQUIPMENT_IN_INVENTORY, violation.rule)
        assertEquals("plan.days[0].exercises[1].equipmentId", violation.path)
        assertEquals(ExerciseCode("L02"), violation.exerciseCode)
        assertEquals("el aparato eq_ajeno no está en gym.equipment", violation.detail)
    }

    @Test
    fun rejectsAlternativeWithUnknownEquipment() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1, alternatives = listOf("eq_2", "eq_ajeno"))))

        val violation = violations(days).single()

        assertEquals(PlanRule.EQUIPMENT_IN_INVENTORY, violation.rule)
        assertEquals("plan.days[0].exercises[0].alternatives[1].equipmentId", violation.path)
        assertEquals(ExerciseCode("L01"), violation.exerciseCode)
    }

    // ------------------------------------------------------------ regla 2: días

    @Test
    fun rejectsADayThatIsNotAvailable() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1)), day(DayOfWeek.FRIDAY, exercise("V01", 1)))

        val violation = violations(days).single()

        assertEquals(PlanRule.AVAILABLE_DAYS, violation.rule)
        assertEquals("plan.days[1].dayOfWeek", violation.path)
        assertEquals(DayOfWeek.FRIDAY, violation.day)
        assertNull(violation.exerciseCode)
    }

    @Test
    fun rejectsARepeatedDay() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1)), day(DayOfWeek.MONDAY, exercise("L02", 2)))

        val violation = violations(days).single()

        assertEquals(PlanRule.AVAILABLE_DAYS, violation.rule)
        assertEquals("plan.days[1].dayOfWeek", violation.path)
        assertEquals("día repetido: ya tiene sesión en plan.days[0]", violation.detail)
    }

    // ------------------------------------------------------------ regla 3 y 5: del modelo

    @Test
    fun rangesSetsAndRirCannotReachTheValidatorBroken() {
        assertFalse(MetricRange.isValid(low = 12, high = 6))
        assertFalse(RirRange.isValid(low = 2, high = 6))
        assertFailsWith<IllegalArgumentException> { MetricRange(low = 12, high = 6) }
        assertFailsWith<IllegalArgumentException> { RirRange(low = 3, high = 2) }
        assertFailsWith<IllegalArgumentException> { exercise("L01", 1, sets = 0) }
    }

    @Test
    fun cardioCannotReachTheValidatorBroken() {
        assertFailsWith<IllegalArgumentException> { cardio(min = 20, max = 15) }
    }

    // ------------------------------------------------------------ regla 4: códigos

    @Test
    fun rejectsARepeatedCode() {
        // Mismo código y mismo orden: lo único que falla es que se repite.
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1), exercise("L01", 1)))

        val violation = violations(days).single()

        assertEquals(PlanRule.EXERCISE_CODES, violation.rule)
        assertEquals("plan.days[0].exercises[1].code", violation.path)
        assertEquals("código repetido: ya lo usa plan.days[0].exercises[0]", violation.detail)
    }

    @Test
    fun rejectsACodeRepeatedOnAnotherDay() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1)), day(DayOfWeek.THURSDAY, exercise("L01", 1)))

        val details = violations(days).map { it.detail }

        assertEquals(
            listOf(
                "el código de un ejercicio de THURSDAY empieza por «J»",
                "código repetido: ya lo usa plan.days[0].exercises[0]",
            ),
            details,
        )
    }

    @Test
    fun rejectsACodeWithTheLetterOfAnotherDay() {
        // «M» es martes; el miércoles es «X».
        val days = listOf(day(DayOfWeek.WEDNESDAY, exercise("M01", 1)))

        val violation = violations(days, available = setOf(DayOfWeek.WEDNESDAY)).single()

        assertEquals(PlanRule.EXERCISE_CODES, violation.rule)
        assertEquals("el código de un ejercicio de WEDNESDAY empieza por «X»", violation.detail)
    }

    @Test
    fun rejectsACodeWithALetterTooMany() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("LA01", 1)))

        assertEquals(PlanRule.EXERCISE_CODES, violations(days).single().rule)
    }

    @Test
    fun rejectsACodeThatDoesNotMatchItsOrder() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1), exercise("L03", 2)))

        val violation = violations(days).single()

        assertEquals(PlanRule.EXERCISE_CODES, violation.rule)
        assertEquals("el código termina en 03 y order es 2", violation.detail)
    }

    @Test
    fun reportsLetterOrderAndRepetitionOfTheSameCodeSeparately() {
        val days = listOf(day(DayOfWeek.MONDAY, exercise("J02", 2), exercise("J02", 3)))

        val violations = violations(days)

        assertEquals(listOf(0, 1, 1, 1), violations.map { it.path.substringAfter("exercises[").first().digitToInt() })
        assertEquals(setOf(PlanRule.EXERCISE_CODES), violations.map { it.rule }.toSet())
    }

    @Test
    fun usesTheLetterOfTheContractForEveryDay() {
        assertEquals("LMXJVSD", DayOfWeek.entries.joinToString("") { it.codeLetter })
    }

    // ------------------------------------------------------------ regla 6: volumen

    @Test
    fun acceptsASessionRightAtTheLimit() {
        // 60 min + 10 % = 66 min; 26 series a 2,5 min son 65.
        val days = listOf(day(DayOfWeek.MONDAY, exercise("L01", 1, sets = 13), exercise("L02", 2, sets = 13)))

        assertEquals(PlanValidation.Valid, validate(days, minutes = 60))
    }

    @Test
    fun rejectsASessionLongerThanTheMinutes() {
        val days = listOf(
            day(DayOfWeek.MONDAY, exercise("L01", 1)),
            day(DayOfWeek.THURSDAY, exercise("J01", 1, sets = 14), exercise("J02", 2, sets = 13)),
        )

        val violation = violations(days, minutes = 60).single()

        assertEquals(PlanRule.SESSION_VOLUME, violation.rule)
        assertEquals("plan.days[1]", violation.path)
        assertEquals(DayOfWeek.THURSDAY, violation.day)
        assertNull(violation.exerciseCode)
        assertEquals(
            "la sesión se estima en 68 min (27 series y 0 min de cardio) y el tope son 66 min " +
                "(sessionMinutes 60 + 10 %): quita series o cardio",
            violation.detail,
        )
    }

    @Test
    fun countsTheMinimumOfTheCardioRange() {
        val twentySets = arrayOf(exercise("L01", 1, sets = 10), exercise("L02", 2, sets = 10))

        // 50 min de series: con 15-40 de cardio son 65, con 17-20 son 67.
        val shortCardio = listOf(day(DayOfWeek.MONDAY, *twentySets, cardio = cardio(15, 40)))
        val longCardio = listOf(day(DayOfWeek.MONDAY, *twentySets, cardio = cardio(17, 20)))

        assertEquals(PlanValidation.Valid, validate(shortCardio, minutes = 60))
        assertEquals(PlanRule.SESSION_VOLUME, violations(longCardio, minutes = 60).single().rule)
    }

    @Test
    fun alternativesDoNotCountSets() {
        val alternatives = listOf("eq_2", "eq_3", "eq_4")
        val days = listOf(
            day(
                DayOfWeek.MONDAY,
                exercise("L01", 1, sets = 13, alternatives = alternatives),
                exercise("L02", 2, sets = 13, alternatives = alternatives),
            ),
        )

        assertEquals(PlanValidation.Valid, validate(days, minutes = 60))
    }

    @Test
    fun unilateralSetsTakeTheWorkOfBothSides() {
        // 22 series: 55 min si son bilaterales, 66 min justos si son por lado.
        val bilateral = day(DayOfWeek.MONDAY, exercise("L01", 1, sets = 22))
        val unilateral = day(DayOfWeek.MONDAY, exercise("L01", 1, sets = 22, unilateral = true))

        assertEquals(55 * 60, SessionVolume.estimatedSeconds(bilateral))
        assertEquals(66 * 60, SessionVolume.estimatedSeconds(unilateral))
        assertEquals(PlanValidation.Valid, validate(listOf(unilateral), minutes = 60))
        assertIs<PlanValidation.Invalid>(validate(listOf(unilateral), minutes = 59))
    }

    @Test
    fun estimatesTheExampleDayOfTheContract() {
        // El lunes del ejemplo: 3+3+3+2+3 series y 15-20 min de cardio.
        val monday = day(
            DayOfWeek.MONDAY,
            *listOf(3, 3, 3, 2, 3).mapIndexed { index, sets -> exercise("L0${index + 1}", index + 1, sets = sets) }
                .toTypedArray(),
            cardio = cardio(15, 20),
        )

        assertEquals(50 * 60, SessionVolume.estimatedSeconds(monday))
        assertEquals(listOf(false, true, true), listOf(45, 60, 75).map { SessionVolume.fits(monday, it) })
    }

    // ------------------------------------------------------------ resultado

    @Test
    fun returnsEveryViolationOrderedByRule() {
        val days = listOf(
            day(DayOfWeek.MONDAY, exercise("L01", 1, "eq_ajeno"), exercise("L01", 2, sets = 30)),
            day(DayOfWeek.SUNDAY, exercise("D01", 1, alternatives = listOf("eq_otro"))),
        )

        val violations = violations(days, minutes = 60)

        assertEquals(
            listOf(
                PlanRule.EQUIPMENT_IN_INVENTORY,
                PlanRule.EQUIPMENT_IN_INVENTORY,
                PlanRule.AVAILABLE_DAYS,
                PlanRule.EXERCISE_CODES,
                PlanRule.EXERCISE_CODES,
                PlanRule.SESSION_VOLUME,
            ),
            violations.map { it.rule },
        )
    }

    @Test
    fun messagesNameTheRuleTheDayAndTheExercise() {
        val days = listOf(
            day(DayOfWeek.MONDAY, exercise("L01", 1)),
            day(DayOfWeek.FRIDAY, exercise("V01", 1, "eq_ajeno")),
        )

        val invalid = assertIs<PlanValidation.Invalid>(validate(days))

        assertEquals(
            listOf(
                "plan.days[1].exercises[0].equipmentId: regla 1, FRIDAY, V01: " +
                    "el aparato eq_ajeno no está en gym.equipment",
                "plan.days[1].dayOfWeek: regla 2, FRIDAY: el día no está en preferences.availableDays",
            ),
            invalid.messages,
        )
    }

    @Test
    fun messagesNeverCopyExerciseNames() {
        val days =
            listOf(day(DayOfWeek.FRIDAY, exercise("M07", 1, "eq_ajeno", sets = 40, alternatives = listOf("eq_no"))))

        val messages = assertIs<PlanValidation.Invalid>(validate(days)).messages

        assertEquals(6, messages.size)
        assertFalse(messages.any { EXERCISE_NAME in it || ALTERNATIVE_NAME in it })
    }

    @Test
    fun numbersTheRulesLikeTheContract() {
        assertEquals(listOf(1, 2, 4, 6), PlanRule.entries.map { it.number })
    }

    // ------------------------------------------------------------ ayuda

    private fun validate(
        days: List<PlanDay>,
        minutes: Int = 60,
        available: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    ): PlanValidation = validator.validate(
        days = days,
        equipment = inventory(count = 5),
        preferences = preferences().copy(availableDays = available, sessionMinutes = minutes),
    )

    private fun violations(
        days: List<PlanDay>,
        minutes: Int = BlockPreferences.SESSION_MINUTES_RANGE.last,
        available: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    ): List<PlanViolation> = assertIs<PlanValidation.Invalid>(validate(days, minutes, available)).violations

    private fun day(dayOfWeek: DayOfWeek, vararg exercises: PlanExercise, cardio: CardioPrescription? = null) = PlanDay(
        id = PlanDayId("day_${dayOfWeek.name}"),
        dayOfWeek = dayOfWeek,
        dayIndex = dayOfWeek.ordinal,
        session = "Sesión",
        priority = "Prioridad",
        cardio = cardio,
        warmup = "Calentamiento",
        mobility = "Movilidad",
        cooldown = "Vuelta a la calma",
        exercises = exercises.toList(),
    )

    private fun cardio(min: Int, max: Int) = CardioPrescription(
        minMinutes = min,
        maxMinutes = max,
        mode = "Caminadora",
        rpe = null,
        speed = null,
        incline = null,
    )

    private fun exercise(
        code: String,
        order: Int,
        equipmentId: String = "eq_1",
        sets: Int = 3,
        unilateral: Boolean = false,
        alternatives: List<String> = emptyList(),
    ) = PlanExercise(
        id = PlanExerciseId("exercise_${code}_$order"),
        code = ExerciseCode(code),
        order = order,
        name = EXERCISE_NAME,
        equipmentId = EquipmentId(equipmentId),
        sets = sets,
        dose = ExerciseDose.Reps(MetricRange(low = 8, high = 12), RirRange(low = 2, high = 2)),
        rest = "2 min",
        trial = TrialLoad("Carga de ensayo"),
        target = "Pecho",
        unilateral = unilateral,
        alternatives = alternatives.map { Alternative(ALTERNATIVE_NAME, EquipmentId(it), note = null) },
    )

    private companion object {
        const val EXERCISE_NAME = "Press sintético"
        const val ALTERNATIVE_NAME = "Variante sintética"
    }
}
