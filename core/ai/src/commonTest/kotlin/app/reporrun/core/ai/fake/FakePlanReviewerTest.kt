package app.reporrun.core.ai.fake

import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.GeneratedReview
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanReviewRequest
import app.reporrun.core.domain.port.PlanReviewResult
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.CheckIn
import app.reporrun.core.model.CheckInId
import app.reporrun.core.model.CheckInRecord
import app.reporrun.core.model.DataQuality
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.ExerciseFeedback
import app.reporrun.core.model.ExerciseFeedbackId
import app.reporrun.core.model.FeedbackRating
import app.reporrun.core.model.Plan
import app.reporrun.core.model.ProposalType
import app.reporrun.core.model.ReviewId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FakePlanReviewerTest {
    @Test
    fun saysDataIsInsufficientAndProposesNothingWithoutComparableFeedback() = runTest {
        val plan = plan()

        val review = review(reviewRequest(plan, feedback = listOf(plan.feedbackFor("L01", sessionsWithData = 1))))

        assertEquals(DataQuality.INSUFFICIENT, review.result.dataQuality)
        assertEquals(emptyList(), review.result.proposals)
    }

    @Test
    fun doesNotCountFeedbackFromAnotherMachineAsComparable() = runTest {
        val plan = plan()
        val changed = plan.feedbackFor("L01", sessionsWithData = 3).copy(equipmentChanged = true)

        val review = review(reviewRequest(plan, feedback = listOf(changed)))

        assertEquals(DataQuality.INSUFFICIENT, review.result.dataQuality)
    }

    @Test
    fun proposesMoreLoadOnlyWhereThereIsComparableFeedback() = runTest {
        val plan = plan()

        val review = review(reviewRequest(plan, feedback = listOf(plan.feedbackFor("L01", sessionsWithData = 3))))

        assertEquals(DataQuality.SUFFICIENT, review.result.dataQuality)
        assertEquals(
            listOf("L01"),
            review.result.proposals.filter {
                it.type == ProposalType.INCREASE_LOAD
            }.map { it.targetExerciseCode?.value },
        )
        // Las demás propuestas del ejemplo no suben carga y sí caben en este plan.
        assertEquals(
            listOf(ProposalType.INCREASE_LOAD, ProposalType.ADJUST_CARDIO, ProposalType.KEEP),
            review.result.proposals.map { it.type },
        )
    }

    @Test
    fun onlyProposesOnExercisesAndDaysOfThePlanUnderReview() = runTest {
        val plan = plan(preferences(availableDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)))
        val codes = plan.days.flatMap { it.exercises }.map { it.code }
        val days = plan.days.map { it.dayOfWeek }

        val review = review(reviewRequest(plan, feedback = listOf(plan.feedbackFor("M01", sessionsWithData = 3))))

        assertEquals(DataQuality.SUFFICIENT, review.result.dataQuality)
        assertTrue(review.result.proposals.all { it.targetExerciseCode == null || it.targetExerciseCode in codes })
        assertTrue(review.result.proposals.all { it.targetDayOfWeek == null || it.targetDayOfWeek in days })
    }

    @Test
    fun signsTheReviewWithTheClock() = runTest {
        val review = review(reviewRequest(plan(), feedback = emptyList()))

        assertEquals(NOW, review.generatedAt)
        assertEquals(FAKE_MODEL, review.model)
    }

    @Test
    fun waitsTheConfiguredLatency() = runTest {
        val request = reviewRequest(plan(), feedback = emptyList())
        val start = currentTime

        reviewer(FakeAiConfig(latency = 4.seconds)).review(request)

        assertEquals(4.seconds.inWholeMilliseconds, currentTime - start)
    }

    @Test
    fun returnsTheConfiguredError() = runTest {
        val request = reviewRequest(plan(), feedback = emptyList())

        val result = reviewer(FakeAiConfig(failure = AiError.Network)).review(request)

        assertEquals(PlanReviewResult.Failure(AiError.Network), result)
    }

    private fun TestScope.dispatchers() = AppDispatchers(StandardTestDispatcher(testScheduler))

    private fun TestScope.reviewer(config: FakeAiConfig = FakeAiConfig()) =
        FakePlanReviewer(config = config, clock = FixedClock(), dispatchers = dispatchers())

    private suspend fun TestScope.review(request: PlanReviewRequest): GeneratedReview =
        assertIs<PlanReviewResult.Success>(reviewer().review(request)).review

    /** El plan que el fake de generación da para el gimnasio de ejemplo. */
    private suspend fun TestScope.plan(preferences: BlockPreferences = preferences()): Plan {
        val generator = FakePlanGenerator(FakeAiConfig(), SequentialIds(), FixedClock(), dispatchers())
        val result = generator.generate(generationRequest(preferences = preferences))
        return assertIs<PlanGenerationResult.Success>(result).plan.toPlan()
    }

    private fun reviewRequest(plan: Plan, feedback: List<ExerciseFeedback>) = PlanReviewRequest(
        plan = plan,
        gym = gym(),
        equipment = exampleInventory(),
        profile = profile(),
        preferences = preferences(),
        checkIn = CheckInRecord(checkIn(), feedback),
        daysElapsed = 14,
        previousReviews = emptyList(),
    )

    private fun checkIn() = CheckIn(
        id = CHECK_IN_ID,
        reviewId = ReviewId("review"),
        weightKg = null,
        waistCm = null,
        hipCm = null,
        sessionsPlanned = 6,
        sessionsDone = 6,
        avgSleepH = null,
        avgSteps = null,
        energy = null,
        fatigue = null,
        painNotes = "",
        painDate = null,
        notes = "",
    )

    private fun Plan.feedbackFor(code: String, sessionsWithData: Int) = ExerciseFeedback(
        id = ExerciseFeedbackId("feedback_$code"),
        checkInId = CHECK_IN_ID,
        planExerciseId = days.flatMap { it.exercises }.first { it.code.value == code }.id,
        usualLoad = null,
        loadUnitNote = "",
        repsReached = null,
        rirPerceived = null,
        rating = FeedbackRating.OK,
        techniqueOk = null,
        equipmentChanged = false,
        sessionsWithData = sessionsWithData,
        notes = "",
    )

    private companion object {
        val CHECK_IN_ID = CheckInId("check_in")
    }
}
