package app.reporrun.core.data.repository

import app.reporrun.core.data.T0
import app.reporrun.core.data.plan
import app.reporrun.core.data.preferences
import app.reporrun.core.data.profile
import app.reporrun.core.model.ChangeProposal
import app.reporrun.core.model.ChangeProposalId
import app.reporrun.core.model.CheckIn
import app.reporrun.core.model.CheckInId
import app.reporrun.core.model.CheckInRecord
import app.reporrun.core.model.Confidence
import app.reporrun.core.model.DataQuality
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Decision
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ExerciseFeedback
import app.reporrun.core.model.ExerciseFeedbackId
import app.reporrun.core.model.FeedbackRating
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.ProposalType
import app.reporrun.core.model.ProposedChange
import app.reporrun.core.model.Review
import app.reporrun.core.model.ReviewFrequency
import app.reporrun.core.model.ReviewId
import app.reporrun.core.model.ReviewMetric
import app.reporrun.core.model.ReviewSchedule
import app.reporrun.core.model.ReviewScheduleId
import app.reporrun.core.model.ReviewStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class ProfileAndReviewRepositoriesTest {
    private val db = inMemoryDatabase()
    private val profiles = ProfileRepositoryImpl(db.profileDao())
    private val preferences = PreferencesRepositoryImpl(db.profileDao())
    private val reviews = ReviewRepositoryImpl(db.reviewDao(), db.checkInDao())

    @AfterTest
    fun close() = db.close()

    // ---------------------------------------------------------------- perfil y preferencias

    @Test
    fun profileIsNullUntilSavedAndThenReplaced() = runTest {
        assertNull(profiles.observeProfile().first())

        profiles.saveProfile(profile())
        val updated = profile().copy(age = 36, priorityMuscles = setOf(MuscleGroup.CHEST))
        profiles.saveProfile(updated)

        assertEquals(updated, profiles.observeProfile().first())
    }

    @Test
    fun savesAndReadsBackBlockPreferences() = runTest {
        preferences.savePreferences(preferences())

        assertEquals(preferences(), preferences.observePreferences().first())
    }

    // ---------------------------------------------------------------- revisiones

    @Test
    fun savesScheduleAndReviews() = runTest {
        val planId = savePlan()
        val schedule = schedule(planId)
        val older = review("rev_1", dueAt = 10)
        val newer = review("rev_2", dueAt = 20)

        reviews.saveSchedule(schedule)
        reviews.saveReview(older)
        reviews.saveReview(newer)

        assertEquals(schedule, reviews.observeSchedule(planId).first())
        assertEquals(listOf(newer, older), reviews.observeReviews(planId).first())
    }

    @Test
    fun rewritingACheckInReplacesItsFeedback() = runTest {
        val planId = savePlan()
        reviews.saveSchedule(schedule(planId))
        reviews.saveReview(review("rev_1", dueAt = 10))
        val press = plan().days[0].exercises[0].id
        val carry = plan().days[1].exercises[0].id

        reviews.saveCheckIn(CheckInRecord(checkIn(), listOf(feedback("fb_1", press), feedback("fb_2", carry))))
        val rewritten = CheckInRecord(checkIn().copy(sessionsDone = 4), listOf(feedback("fb_3", press)))
        reviews.saveCheckIn(rewritten)

        assertEquals(rewritten, reviews.observeCheckIn(ReviewId("rev_1")).first())
    }

    @Test
    fun recordsTheDecisionOnAProposal() = runTest {
        val planId = savePlan()
        reviews.saveSchedule(schedule(planId))
        reviews.saveReview(review("rev_1", dueAt = 10))
        val proposal = proposal()
        reviews.saveProposals(listOf(proposal))

        reviews.decide(proposal.id, Decision.APPROVED, T0)

        assertEquals(
            listOf(proposal.copy(decision = Decision.APPROVED, decidedAt = T0)),
            reviews.observeProposals(ReviewId("rev_1")).first(),
        )
    }

    @Test
    fun rejectsADecisionWithoutDate() = runTest {
        assertFailsWith<IllegalArgumentException> {
            reviews.decide(ChangeProposalId("prop_1"), Decision.REJECTED, decidedAt = null)
        }
    }

    // ---------------------------------------------------------------- ayudas

    private suspend fun savePlan() = plan().also {
        db.seedGymWithPlanEquipment()
        PlanRepositoryImpl(db.planDao(), db.planHistoryDao()).savePlan(it)
    }.id

    private fun schedule(planId: PlanId) = ReviewSchedule(
        id = ReviewScheduleId("sched_1"),
        planId = planId,
        frequency = ReviewFrequency.CUSTOM,
        customDays = 10,
        anchorDate = LocalDate(2026, 10, 5),
        weekday = DayOfWeek.SUNDAY,
        time = LocalTime(7, 30),
        metrics = setOf(ReviewMetric.SLEEP, ReviewMetric.WEIGHT),
        remindersEnabled = true,
        nextDueAt = T0 + 10.days,
    )

    private fun review(id: String, dueAt: Int) = Review(
        id = ReviewId(id),
        planId = plan().id,
        scheduleId = ReviewScheduleId("sched_1"),
        dueAt = T0 + dueAt.days,
        completedAt = null,
        status = ReviewStatus.PROPOSED,
        dataQuality = DataQuality.LIMITED,
    )

    private fun checkIn() = CheckIn(
        id = CheckInId("ci_1"),
        reviewId = ReviewId("rev_1"),
        weightKg = null,
        waistCm = null,
        hipCm = null,
        sessionsPlanned = 4,
        sessionsDone = 3,
        avgSleepH = 7.5,
        avgSteps = 8000,
        energy = 4,
        fatigue = 2,
        painNotes = "",
        painDate = null,
        notes = "",
    )

    private fun feedback(id: String, planExerciseId: PlanExerciseId) = ExerciseFeedback(
        id = ExerciseFeedbackId(id),
        checkInId = CheckInId("ci_1"),
        planExerciseId = planExerciseId,
        usualLoad = 25.0,
        loadUnitNote = "número del selector",
        repsReached = listOf(12, 10, 9),
        rirPerceived = 2,
        rating = FeedbackRating.OK,
        techniqueOk = true,
        equipmentChanged = false,
        sessionsWithData = 3,
        notes = "",
    )

    private fun proposal() = ChangeProposal(
        id = ChangeProposalId("prop_1"),
        reviewId = ReviewId("rev_1"),
        change = ProposedChange(
            type = ProposalType.SWAP_EXERCISE,
            targetExerciseCode = ExerciseCode("L01"),
            targetDayOfWeek = null,
            replacementEquipmentId = EquipmentId("eq_db"),
            description = "Cambiar a mancuernas",
            rationale = "La máquina estuvo ocupada",
            confidence = Confidence.MEDIUM,
        ),
    )
}
