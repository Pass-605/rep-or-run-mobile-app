package app.reporrun.core.data.mapper

import app.reporrun.core.data.plan
import app.reporrun.core.database.entity.CardioColumns
import app.reporrun.core.database.entity.DoseColumns
import app.reporrun.core.database.entity.PlanDayWithExercises
import app.reporrun.core.database.entity.PlanExerciseWithAlternatives
import app.reporrun.core.database.entity.PlanWithDays
import app.reporrun.core.domain.validation.Mapped
import app.reporrun.core.domain.validation.ModelErrors
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.Metric
import app.reporrun.core.model.MetricRange
import app.reporrun.core.model.RirRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlanMappersTest {
    // ---------------------------------------------------------------- ida y vuelta

    @Test
    fun roundTripsPlanThroughRows() {
        val plan = plan()

        assertEquals(Mapped.Valid(plan), plan.toRows().toPlanWithDays().toPlan())
    }

    @Test
    fun sortsDaysExercisesAndAlternativesWhateverTheRowOrder() {
        val plan = plan()

        val shuffled = plan.toRows().toPlanWithDays(reversed = true)

        assertEquals(Mapped.Valid(plan), shuffled.toPlan())
    }

    @Test
    fun writesRirOnlyForRepsWithRir() {
        val exercises = plan().toRows().exercises.associate { it.code to it.dose }

        assertEquals(DoseColumns(Metric.REPS, 8, 12, 1, 2), exercises["L01"])
        assertEquals(DoseColumns(Metric.REPS, 10, 12, null, null), exercises["L02"])
        assertEquals(DoseColumns(Metric.METERS, 20, 30, null, null), exercises["J01"])
        assertEquals(DoseColumns(Metric.SECONDS, 30, 45, null, null), exercises["J02"])
    }

    @Test
    fun writesCardioWholeOrNotAtAll() {
        val days = plan().toRows().days

        assertEquals(CardioColumns(10, 15, "Caminadora", "5-6", "5 km/h", null), days[0].cardio)
        assertNull(days[1].cardio)
    }

    @Test
    fun keepsAlternativePositions() {
        val alternatives = plan().toRows().alternatives

        assertEquals(listOf(0, 1), alternatives.map { it.position })
        assertEquals(listOf("eq_db", "eq_floor"), alternatives.map { it.equipmentId })
    }

    // ---------------------------------------------------------------- dosis

    @Test
    fun rebuildsEachDoseFromItsMetric() {
        val errors = ModelErrors()

        assertEquals(
            ExerciseDose.Reps(MetricRange(8, 12), RirRange(0, 1)),
            DoseColumns(Metric.REPS, 8, 12, 0, 1).toDose("dose", errors),
        )
        assertEquals(
            ExerciseDose.Reps(MetricRange(8, 12), null),
            DoseColumns(Metric.REPS, 8, 12, null, null).toDose("dose", errors),
        )
        assertEquals(
            ExerciseDose.Meters(MetricRange(20, 30)),
            DoseColumns(Metric.METERS, 20, 30, null, null).toDose("dose", errors),
        )
        assertEquals(
            ExerciseDose.Seconds(MetricRange(30, 45)),
            DoseColumns(Metric.SECONDS, 30, 45, null, null).toDose("dose", errors),
        )
        assertEquals(emptyList(), errors.errors)
    }

    @Test
    fun reportsRangeAndRirOfTheSameExerciseSeparately() {
        val errors = ModelErrors()

        assertNull(DoseColumns(Metric.REPS, low = 12, high = 8, rirLow = 4, rirHigh = 7).toDose("dose", errors))

        assertEquals(2, errors.size)
        assertEquals("dose: rango inválido: 12-8 (1 <= low <= high)", errors.errors[0])
        assertEquals("dose: RIR inválido: 4-7 (0..5 y low <= high)", errors.errors[1])
    }

    @Test
    fun rejectsRirWithOnlyOneEnd() {
        val errors = ModelErrors()

        assertNull(DoseColumns(Metric.REPS, 8, 12, rirLow = 1, rirHigh = null).toDose("dose", errors))

        assertEquals(listOf("dose: RIR incompleto: rirLow y rirHigh van los dos o ninguno"), errors.errors)
    }

    @Test
    fun rejectsRirOnMetersAndSeconds() {
        val errors = ModelErrors()

        assertNull(DoseColumns(Metric.METERS, 20, 30, 1, 2).toDose("a", errors))
        assertNull(DoseColumns(Metric.SECONDS, 30, 45, null, 2).toDose("b", errors))

        assertEquals(listOf("a: el RIR no aplica a METERS", "b: el RIR no aplica a SECONDS"), errors.errors)
    }

    // ---------------------------------------------------------------- lista completa de errores

    @Test
    fun reportsEveryBrokenRuleOfThePlanInsteadOfTheFirst() {
        val rows = plan().toRows()
        val broken = rows.copy(
            exercises = rows.exercises.map { exercise ->
                when (exercise.code) {
                    "L01" -> exercise.copy(dose = exercise.dose.copy(low = 12, high = 8))
                    "L02" -> exercise.copy(sets = 0)
                    "J01" -> exercise.copy(dose = exercise.dose.copy(rirLow = 1, rirHigh = 2))
                    "J02" -> exercise.copy(equipmentId = "floor")
                    else -> exercise
                }
            },
            days = rows.days.map { if (it.dayIndex == 0) it.copy(cardio = it.cardio?.copy(minMinutes = 20)) else it },
        )

        val result = assertIs<Mapped.Invalid>(broken.toPlanWithDays().toPlan())

        assertEquals(
            listOf(
                "days[0].cardio: minMinutes > maxMinutes",
                "days[0].exercises[0].dose: rango inválido: 12-8 (1 <= low <= high)",
                "days[0].exercises[1].sets: series debe ser >= 1, es 0",
                "days[1].exercises[0].dose: el RIR no aplica a METERS",
                "days[1].exercises[1].equipmentId: EquipmentId fuera de formato: floor",
            ),
            result.errors,
        )
    }

    @Test
    fun reportsBrokenAlternativesAndPlanFieldsWithTheirPath() {
        val rows = plan().toRows()
        val broken = rows.copy(
            plan = rows.plan.copy(gymId = " "),
            alternatives = rows.alternatives.map { if (it.position == 1) it.copy(name = "") else it },
        )

        val result = assertIs<Mapped.Invalid>(broken.toPlanWithDays().toPlan())

        assertEquals(
            listOf("plan.gymId: GymId vacío", "days[0].exercises[0].alternatives[1]: name vacío"),
            result.errors,
        )
    }

    @Test
    fun reportsPlanLevelInvariantsOnceTheTreeIsValid() {
        val rows = plan().toRows()
        val broken = rows.copy(plan = rows.plan.copy(blockDays = 7))

        val result = assertIs<Mapped.Invalid>(broken.toPlanWithDays().toPlan())

        assertEquals(listOf("plan: v1 solo admite bloques de 14 días"), result.errors)
    }
}

/** Arma lo que devolvería Room para estas filas; con [reversed], en el orden contrario. */
private fun PlanRows.toPlanWithDays(reversed: Boolean = false): PlanWithDays {
    fun <T> List<T>.ordered() = if (reversed) asReversed() else this
    val dayTrees = days.ordered().map { day ->
        val exerciseTrees = exercises.filter { it.planDayId == day.id }.ordered().map { exercise ->
            PlanExerciseWithAlternatives(exercise, alternatives.filter { it.planExerciseId == exercise.id }.ordered())
        }
        PlanDayWithExercises(day, exerciseTrees)
    }
    return PlanWithDays(plan, dayTrees)
}
