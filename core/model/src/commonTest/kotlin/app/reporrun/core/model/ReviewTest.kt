package app.reporrun.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewTest {
    @Test
    fun insufficientDataCarriesNoProposals() {
        assertFailsWith<IllegalArgumentException> {
            ReviewResult(
                dataQuality = DataQuality.INSUFFICIENT,
                summary = "No hay con qué comparar.",
                observations = emptyList(),
                proposals = listOf(proposed()),
                questions = emptyList(),
            )
        }
    }

    @Test
    fun insufficientDataWithQuestionsIsValid() {
        val result = ReviewResult(
            dataQuality = DataQuality.INSUFFICIENT,
            summary = "No hay con qué comparar.",
            observations = emptyList(),
            proposals = emptyList(),
            questions = listOf("¿Registraste las cargas?"),
        )
        assertTrue(result.proposals.isEmpty())
    }

    @Test
    fun exerciseProposalNeedsTargetExercise() {
        assertFailsWith<IllegalArgumentException> { proposed(type = ProposalType.INCREASE_LOAD, code = null) }
    }

    @Test
    fun cardioProposalMayTargetOnlyADay() {
        val cardio = proposed(type = ProposalType.ADJUST_CARDIO, code = null, day = DayOfWeek.WEDNESDAY)
        assertEquals(DayOfWeek.WEDNESDAY, cardio.targetDayOfWeek)
    }

    @Test
    fun dayOnlyAppliesToDayProposals() {
        assertFailsWith<IllegalArgumentException> { proposed(day = DayOfWeek.MONDAY) }
    }

    @Test
    fun replacementOnlyAppliesToSwap() {
        assertFailsWith<IllegalArgumentException> { proposed(replacement = "eq_smith") }
        assertEquals(
            "eq_smith",
            proposed(type = ProposalType.SWAP_EXERCISE, replacement = "eq_smith").replacementEquipmentId?.value,
        )
    }

    @Test
    fun proposalStartsPendingWithoutDecisionDate() {
        val proposal = ChangeProposal(ChangeProposalId("p_1"), ReviewId("rev_1"), proposed())
        assertEquals(Decision.PENDING, proposal.decision)
        assertFailsWith<IllegalArgumentException> { proposal.copy(decision = Decision.APPROVED) }
        assertFailsWith<IllegalArgumentException> { proposal.copy(decidedAt = NOW) }
    }

    @Test
    fun feedbackWithFewSessionsIsNotComparable() {
        assertFalse(feedback(sessionsWithData = 1).isComparable)
        assertFalse(feedback(sessionsWithData = 3, equipmentChanged = true).isComparable)
        assertTrue(feedback(sessionsWithData = 2).isComparable)
    }

    @Test
    fun painIsRecordedWithDate() {
        assertFailsWith<IllegalArgumentException> { checkIn(painNotes = "Molestia en el hombro", painDate = null) }
        assertEquals(
            LocalDate(2026, 9, 30),
            checkIn(painNotes = "Molestia en el hombro", painDate = LocalDate(2026, 9, 30)).painDate,
        )
    }

    @Test
    fun rejectsEnergyOutsideScale() {
        assertFailsWith<IllegalArgumentException> { checkIn().copy(energy = 6) }
    }

    @Test
    fun customFrequencyNeedsCustomDays() {
        assertFailsWith<IllegalArgumentException> { schedule(ReviewFrequency.CUSTOM, customDays = null) }
        assertFailsWith<IllegalArgumentException> { schedule(ReviewFrequency.EVERY_14, customDays = 10) }
        assertEquals(10, schedule(ReviewFrequency.CUSTOM, customDays = 10).intervalDays)
        assertEquals(14, schedule(ReviewFrequency.EVERY_14, customDays = null).intervalDays)
    }

    @Test
    fun completedAtOnlyOnFinalReviews() {
        val pending =
            Review(
                ReviewId("rev_1"),
                PlanId("ROR-2026-09-21-v1"),
                ReviewScheduleId("s_1"),
                NOW,
                null,
                ReviewStatus.PENDING,
                null,
            )
        assertFailsWith<IllegalArgumentException> { pending.copy(completedAt = NOW) }
        assertFailsWith<IllegalArgumentException> { pending.copy(status = ReviewStatus.CLOSED) }
        assertFailsWith<IllegalArgumentException> { pending.copy(status = ReviewStatus.PROPOSED) }
    }

    private fun feedback(sessionsWithData: Int, equipmentChanged: Boolean = false) = ExerciseFeedback(
        id = ExerciseFeedbackId("fb_1"),
        checkInId = CheckInId("ci_1"),
        planExerciseId = PlanExerciseId("ex_L01"),
        usualLoad = 32.5,
        loadUnitNote = "número del selector",
        repsReached = listOf(10, 10, 10),
        rirPerceived = 2,
        rating = FeedbackRating.OK,
        techniqueOk = true,
        equipmentChanged = equipmentChanged,
        sessionsWithData = sessionsWithData,
        notes = "",
    )

    private fun checkIn(painNotes: String = "", painDate: LocalDate? = null) = CheckIn(
        id = CheckInId("ci_1"),
        reviewId = ReviewId("rev_1"),
        weightKg = null,
        waistCm = null,
        hipCm = null,
        sessionsPlanned = 6,
        sessionsDone = 6,
        avgSleepH = 7.0,
        avgSteps = null,
        energy = 4,
        fatigue = 2,
        painNotes = painNotes,
        painDate = painDate,
        notes = "",
    )

    private fun schedule(frequency: ReviewFrequency, customDays: Int?) = ReviewSchedule(
        id = ReviewScheduleId("s_1"),
        planId = PlanId("ROR-2026-09-21-v1"),
        frequency = frequency,
        customDays = customDays,
        anchorDate = LocalDate(2026, 9, 21),
        weekday = DayOfWeek.MONDAY,
        time = LocalTime(8, 0),
        metrics = setOf(ReviewMetric.WEIGHT, ReviewMetric.SLEEP),
        remindersEnabled = true,
        nextDueAt = NOW,
    )
}
