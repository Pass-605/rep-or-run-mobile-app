package app.reporrun.core.database

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import app.reporrun.core.model.Decision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReviewDaoTest {
    private lateinit var db: RepOrRunDatabase
    private val reviewDao get() = db.reviewDao()
    private val rows = planRows()

    @BeforeTest
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        db.gymDao().upsertGym(gym("gym_1"))
        db.equipmentDao().upsertEquipment(listOf(equipment("eq_press"), equipment("eq_db")))
        db.planDao().insertPlan(rows.plan, rows.days, rows.exercises, rows.alternatives)
        seedReviewData(db, planId = rows.plan.id, planExerciseId = rows.exercises[0].id)
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun readsBackScheduleWithTimeAndMetrics() = runBlocking {
        assertEquals(schedule(rows.plan.id), reviewDao.observeSchedule(rows.plan.id).first())
    }

    @Test
    fun readsBackCheckInWithItsFeedback() = runBlocking {
        val stored = assertNotNull(db.checkInDao().observeCheckIn("rev_1").first())

        assertEquals(checkIn(), stored.checkIn)
        assertEquals(listOf(feedback("fb_1", rows.exercises[0].id)), stored.feedback)
    }

    @Test
    fun savingTheCheckInAgainReplacesItsFeedback() = runBlocking {
        val updated = checkIn().copy(sessionsDone = 4)
        val newFeedback = feedback("fb_2", rows.exercises[1].id).copy(repsReached = null)

        db.checkInDao().saveCheckIn(updated, listOf(newFeedback))

        val stored = assertNotNull(db.checkInDao().observeCheckIn("rev_1").first())
        assertEquals(updated, stored.checkIn)
        assertEquals(listOf(newFeedback), stored.feedback)
    }

    @Test
    fun recordsTheDecisionOnAProposal() = runBlocking {
        reviewDao.decide("prop_1", Decision.APPROVED, T0)

        val stored = reviewDao.observeProposals("rev_1").first().single()
        assertEquals(Decision.APPROVED to T0, stored.decision to stored.decidedAt)
    }

    @Test
    fun deletingThePlanCascadesToReviewsAndExports() = runBlocking {
        db.planDao().deletePlan(rows.plan.id)

        listOf("review_schedule", "review", "check_in", "exercise_feedback", "change_proposal", "export_record")
            .forEach { assertEquals(0, db.count(it), it) }
    }

    @Test
    fun losingTheReviewKeepsTheApprovedChange() = runBlocking {
        val v2 = planRows(id = "ROR-2026-10-14-v2")
        db.planDao().insertPlan(v2.plan, v2.days, v2.exercises, v2.alternatives)
        db.planHistoryDao().insertChange(
            planChange(planId = v2.plan.id, previousPlanId = rows.plan.id, reviewId = "rev_1"),
        )

        // Borrar la revisión (aquí, vía su calendario) deja el cambio sin revisión, no lo borra.
        db.useWriterConnection { it.immediateTransaction { execSQL("DELETE FROM review_schedule") } }

        assertNull(db.planHistoryDao().observeChanges("gym_1").first().single().reviewId)
    }
}
