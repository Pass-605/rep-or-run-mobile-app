package app.reporrun.core.database

import androidx.sqlite.SQLiteException
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Metric
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class PlanDaoTest {
    private lateinit var db: RepOrRunDatabase
    private val gymDao get() = db.gymDao()
    private val planDao get() = db.planDao()

    @BeforeTest
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        gymDao.upsertGym(gym("gym_1", isActive = true))
        db.equipmentDao().upsertEquipment(listOf(equipment("eq_press"), equipment("eq_db")))
    }

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun insert(rows: PlanRows) =
        planDao.insertPlan(rows.plan, rows.days, rows.exercises, rows.alternatives)

    @Test
    fun readsBackTheWholePlanTree() = runBlocking {
        val rows = planRows()
        insert(rows)

        val stored = assertNotNull(planDao.observePlan(rows.plan.id).first())

        assertEquals(rows.plan, stored.plan)
        assertEquals(rows.days.toSet(), stored.days.map { it.day }.toSet())
        assertEquals(rows.exercises.toSet(), stored.days.flatMap { it.exercises }.map { it.exercise }.toSet())
        assertEquals(rows.alternatives, stored.days.flatMap { it.exercises }.flatMap { it.alternatives })
    }

    @Test
    fun keepsCardioAbsentAsNullAndPresentAsColumns() = runBlocking {
        val rows = planRows()
        insert(rows)

        val days = assertNotNull(planDao.observePlan(rows.plan.id).first()).days.associateBy { it.day.dayOfWeek }

        assertEquals(rows.days[0].cardio, days.getValue(DayOfWeek.MONDAY).day.cardio)
        assertNull(days.getValue(DayOfWeek.THURSDAY).day.cardio)
    }

    @Test
    fun keepsDoseDiscriminatorAndRirColumns() = runBlocking {
        val rows = planRows()
        insert(rows)

        val exercises = assertNotNull(planDao.observePlan(rows.plan.id).first())
            .days.flatMap { it.exercises }.associate { it.exercise.code to it.exercise.dose }

        assertEquals(Metric.REPS, exercises.getValue("L01").metric)
        assertEquals(1 to 2, exercises.getValue("L01").let { it.rirLow to it.rirHigh })
        assertEquals(Metric.METERS, exercises.getValue("J01").metric)
        assertEquals(null to null, exercises.getValue("J01").let { it.rirLow to it.rirHigh })
    }

    @Test
    fun insertsNothingWhenAnyRowFails() = runBlocking {
        // El ejercicio apunta a un aparato que no está en el inventario: falla la clave foránea.
        val rows = planRows(mainEquipment = "eq_unknown")

        assertFailsWith<SQLiteException> { insert(rows) }

        assertEquals(0, db.count("plan"))
        assertEquals(0, db.count("plan_day"))
    }

    @Test
    fun rejectsRewritingAnExistingPlan() = runBlocking<Unit> {
        val rows = planRows()
        insert(rows)

        assertFailsWith<SQLiteException> { insert(rows) }
    }

    @Test
    fun observesTheActivePlanOfTheGym() = runBlocking {
        val draft = planRows()
        insert(draft)
        assertNull(planDao.observeActivePlan("gym_1").first())

        planDao.activate(draft.plan.id)

        assertEquals(draft.plan.id, planDao.observeActivePlan("gym_1").first()?.plan?.id)
    }

    @Test
    fun activatingANewVersionArchivesThePreviousOne() = runBlocking {
        val v1 = planRows(id = "ROR-2026-09-30-v1", status = PlanStatus.ACTIVE)
        val v2 = planRows(id = "ROR-2026-10-14-v2", createdAt = T0 + 14.days)
        insert(v1)
        insert(v2)

        planDao.activate(v2.plan.id)

        val statuses = db.planHistoryDao().observeHistory("gym_1").first().associate { it.id to it.status }
        assertEquals(mapOf(v1.plan.id to PlanStatus.ARCHIVED, v2.plan.id to PlanStatus.ACTIVE), statuses)
    }

    @Test
    fun activatingDoesNotTouchOtherGyms() = runBlocking {
        gymDao.upsertGym(gym("gym_2"))
        db.equipmentDao().upsertEquipment(listOf(equipment("eq_press2", "gym_2"), equipment("eq_db2", "gym_2")))
        val other = planRows(id = "ROR-2026-09-01-v1", gymId = "gym_2", status = PlanStatus.ACTIVE)
            .let { it.copy(exercises = it.exercises.map { e -> e.copy(equipmentId = "eq_press2") }) }
            .let { it.copy(alternatives = it.alternatives.map { a -> a.copy(equipmentId = "eq_db2") }) }
        insert(other)
        insert(planRows())

        planDao.activate(planRows().plan.id)

        assertEquals(PlanStatus.ACTIVE, db.planHistoryDao().observeHistory("gym_2").first().single().status)
    }

    @Test
    fun listsHistoryFromNewestToOldest() = runBlocking {
        insert(planRows(id = "ROR-2026-09-30-v1"))
        insert(planRows(id = "ROR-2026-10-14-v2", createdAt = T0 + 14.days))

        val history = db.planHistoryDao().observeHistory("gym_1").first().map { it.id }

        assertEquals(listOf("ROR-2026-10-14-v2", "ROR-2026-09-30-v1"), history)
    }

    @Test
    fun approvesADayWithoutRewritingThePlan() = runBlocking {
        val rows = planRows()
        insert(rows)

        planDao.updateDayStatus(rows.days[0].id, DayStatus.APPROVED)

        val days = assertNotNull(planDao.observePlan(rows.plan.id).first()).days
            .associate { it.day.id to it.day.status }
        assertEquals(mapOf(rows.days[0].id to DayStatus.APPROVED, rows.days[1].id to DayStatus.PROPOSED), days)
    }

    @Test
    fun deletingAPlanCascadesToAllItsLevels() = runBlocking {
        val rows = planRows()
        insert(rows)

        planDao.deletePlan(rows.plan.id)

        listOf("plan", "plan_day", "plan_exercise", "plan_exercise_alternative").forEach {
            assertEquals(0, db.count(it), it)
        }
    }

    @Test
    fun keepsAVersionThatAnotherOneHasAsPrevious() = runBlocking {
        val v1 = planRows(id = "ROR-2026-09-30-v1")
        val v2 = planRows(id = "ROR-2026-10-14-v2", createdAt = T0 + 14.days)
        insert(v1)
        insert(v2)
        db.planHistoryDao().insertChange(planChange(planId = v2.plan.id, previousPlanId = v1.plan.id))

        assertFailsWith<SQLiteException> { planDao.deletePlan(v1.plan.id) }
        assertEquals(2, db.count("plan"))
    }

    @Test
    fun listsChangesOfTheGym() = runBlocking {
        val v1 = planRows(id = "ROR-2026-09-30-v1")
        val v2 = planRows(id = "ROR-2026-10-14-v2", createdAt = T0 + 14.days)
        insert(v1)
        insert(v2)
        val first = planChange(id = "ch_1", planId = v1.plan.id, previousPlanId = null)
        val second = planChange(id = "ch_2", planId = v2.plan.id, previousPlanId = v1.plan.id)
            .copy(approvedAt = T0 + 14.days)
        db.planHistoryDao().insertChange(first)
        db.planHistoryDao().insertChange(second)

        assertEquals(listOf(second, first), db.planHistoryDao().observeChanges("gym_1").first())
    }
}
