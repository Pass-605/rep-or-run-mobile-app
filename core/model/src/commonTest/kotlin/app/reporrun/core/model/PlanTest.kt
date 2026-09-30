package app.reporrun.core.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanTest {
    @Test
    fun rejectsRangeWithLowAboveHigh() {
        assertFailsWith<IllegalArgumentException> { MetricRange(12, 8) }
    }

    @Test
    fun acceptsRangeWithLowEqualToHigh() {
        assertEquals(10, MetricRange(10, 10).high)
    }

    @Test
    fun rejectsRangeStartingAtZero() {
        assertFalse(MetricRange.isValid(0, 5))
    }

    @Test
    fun rejectsRirOutsideZeroToFive() {
        assertFailsWith<IllegalArgumentException> { RirRange(2, 6) }
        assertFailsWith<IllegalArgumentException> { RirRange(-1, 2) }
    }

    @Test
    fun rejectsRirWithLowAboveHigh() {
        assertFalse(RirRange.isValid(3, 1))
    }

    @Test
    fun onlyRepsCarryRir() {
        // Metros y segundos no tienen campo de RIR: el tipo no deja expresarlo (plan §4.8).
        val carry = ExerciseDose.Meters(MetricRange(20, 30))
        assertEquals(Metric.METERS, carry.metric)
        assertEquals(Metric.SECONDS, ExerciseDose.Seconds(MetricRange(30, 45)).metric)
        assertEquals(Metric.REPS, ExerciseDose.Reps(MetricRange(8, 12), rir = null).metric)
    }

    @Test
    fun rejectsExerciseWithoutSets() {
        assertFailsWith<IllegalArgumentException> { exercise(sets = 0) }
    }

    @Test
    fun rejectsBlankTrialLoad() {
        assertFailsWith<IllegalArgumentException> { TrialLoad(" ") }
    }

    @Test
    fun unilateralSetsCountPerSideWithoutDoubling() {
        val carry = exercise(code = "L06", sets = 3, unilateral = true, dose = ExerciseDose.Meters(MetricRange(20, 30)))
        assertEquals(3, carry.effectiveSets)
    }

    @Test
    fun alternativesDoNotAddSets() {
        val withAlternatives = exercise(
            sets = 3,
            alternatives = listOf(
                Alternative("Press con mancuernas", EquipmentId("eq_dumbbells"), note = null),
                Alternative("Press en Smith", EquipmentId("eq_smith"), note = null),
            ),
        )
        assertEquals(3, withAlternatives.effectiveSets)
        assertEquals(3, plan(days = listOf(day(listOf(withAlternatives)))).weeklyEffectiveSets)
    }

    @Test
    fun dayStartsProposed() {
        assertEquals(DayStatus.PROPOSED, day(emptyList()).status)
    }

    @Test
    fun rejectsCardioWithMinAboveMax() {
        assertFailsWith<IllegalArgumentException> {
            CardioPrescription(
                minMinutes = 20,
                maxMinutes = 15,
                mode = "Caminadora",
                rpe = "5-6",
                speed = null,
                incline = null,
            )
        }
    }

    @Test
    fun versionComesFromPlanId() {
        assertEquals(2, plan(id = "ROR-2026-10-05-v2").version)
    }

    @Test
    fun rejectsMalformedIds() {
        assertFailsWith<IllegalArgumentException> { PlanId("ROR-2026-09-21") }
        assertFailsWith<IllegalArgumentException> { EquipmentId("chest_press") }
        assertFailsWith<IllegalArgumentException> { ExerciseCode("l01") }
        assertFailsWith<IllegalArgumentException> { CatalogKey("Chest Press") }
    }

    @Test
    fun rejectsBlockOtherThanFourteenDays() {
        assertFailsWith<IllegalArgumentException> { plan().copy(blockDays = 21) }
    }

    @Test
    fun planChangeKeepsPreviousVersion() {
        val change = PlanChange(
            id = PlanChangeId("chg_1"),
            planId = PlanId("ROR-2026-10-05-v2"),
            previousPlanId = PlanId("ROR-2026-09-21-v1"),
            reviewId = ReviewId("rev_1"),
            date = LocalDate(2026, 10, 5),
            summary = "Sube L01 y X01",
            reason = "Doble progresión cumplida",
            approvedAt = NOW,
        )
        assertTrue(checkNotNull(change.previousPlanId).version < change.planId.version)
    }

    @Test
    fun rejectsPlanChangeWithoutPreviousVersionAfterV1() {
        assertFailsWith<IllegalArgumentException> {
            PlanChange(
                id = PlanChangeId("chg_1"),
                planId = PlanId("ROR-2026-10-05-v2"),
                previousPlanId = null,
                reviewId = null,
                date = LocalDate(2026, 10, 5),
                summary = "Sube L01",
                reason = "Doble progresión cumplida",
                approvedAt = NOW,
            )
        }
    }

    @Test
    fun rejectsEquipmentWithMinAboveMax() {
        assertFailsWith<IllegalArgumentException> {
            Equipment(
                id = EquipmentId("eq_chest_press"),
                gymId = GymId("gym_central"),
                catalogKey = CatalogKey("chest_press"),
                name = "Chest Press",
                category = EquipmentCategory.MACHINE,
                loadType = LoadType.SELECTOR,
                minKg = 100.0,
                maxKg = 5.0,
                incrementKg = 5.0,
                notes = null,
                isCustom = false,
            )
        }
    }
}
