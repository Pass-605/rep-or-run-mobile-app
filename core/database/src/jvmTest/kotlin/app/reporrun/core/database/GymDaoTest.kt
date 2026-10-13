package app.reporrun.core.database

import androidx.sqlite.SQLiteException
import app.reporrun.core.database.entity.EquipmentCatalogEntity
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GymDaoTest {
    private lateinit var db: RepOrRunDatabase
    private val gymDao get() = db.gymDao()

    @BeforeTest
    fun setUp() {
        db = inMemoryDatabase()
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun hasNoActiveGymUntilOneIsSet() = runBlocking {
        gymDao.upsertGym(gym("gym_1"))

        assertNull(gymDao.observeActiveGym().first())
    }

    @Test
    fun keepsASingleActiveGym() = runBlocking {
        gymDao.upsertGym(gym("gym_1", isActive = true))
        gymDao.upsertGym(gym("gym_2"))

        gymDao.setActive("gym_2")

        assertEquals("gym_2", gymDao.observeActiveGym().first()?.id)
        assertEquals(listOf("gym_2"), gymDao.observeGyms().first().filter { it.isActive }.map { it.id })
    }

    @Test
    fun observesTheInventoryOfOneGymSortedByName() = runBlocking {
        gymDao.upsertGym(gym("gym_1"))
        gymDao.upsertGym(gym("gym_2"))
        db.equipmentDao().upsertEquipment(
            listOf(
                equipment("eq_b").copy(name = "Remo"),
                equipment("eq_a").copy(name = "Prensa"),
                equipment("eq_other", gymId = "gym_2"),
            ),
        )

        assertEquals(listOf("Prensa", "Remo"), db.equipmentDao().observeEquipment("gym_1").first().map { it.name })
    }

    @Test
    fun rejectsEquipmentOfAnUnknownGym() = runBlocking<Unit> {
        assertFailsWith<SQLiteException> {
            db.equipmentDao().upsertEquipment(listOf(equipment("eq_a", gymId = "gym_x")))
        }
    }

    @Test
    fun deletesUnusedEquipment() = runBlocking {
        gymDao.upsertGym(gym("gym_1"))
        db.equipmentDao().upsertEquipment(listOf(equipment("eq_a")))

        assertEquals(0, db.equipmentDao().countEquipmentUsages("eq_a"))
        db.equipmentDao().deleteEquipment("eq_a")

        assertEquals(emptyList(), db.equipmentDao().observeEquipment("gym_1").first())
    }

    @Test
    fun refusesToDeleteEquipmentUsedByAPlan() = runBlocking {
        gymDao.upsertGym(gym("gym_1"))
        db.equipmentDao().upsertEquipment(listOf(equipment("eq_press"), equipment("eq_db")))
        val rows = planRows()
        db.planDao().insertPlan(rows.plan, rows.days, rows.exercises, rows.alternatives)

        // eq_db lo usan el carry del jueves y la alternativa del lunes.
        assertEquals(2, db.equipmentDao().countEquipmentUsages("eq_db"))
        assertFailsWith<SQLiteException> { db.equipmentDao().deleteEquipment("eq_db") }
        assertEquals(2, db.count("equipment"))
    }

    @Test
    fun deletingAGymCascadesToInventoryPlansAndReviews() = runBlocking {
        gymDao.upsertGym(gym("gym_1"))
        db.equipmentDao().upsertEquipment(listOf(equipment("eq_press"), equipment("eq_db")))
        val v1 = planRows(id = "ROR-2026-09-30-v1")
        val v2 = planRows(id = "ROR-2026-10-14-v2")
        db.planDao().insertPlan(v1.plan, v1.days, v1.exercises, v1.alternatives)
        db.planDao().insertPlan(v2.plan, v2.days, v2.exercises, v2.alternatives)
        seedReviewData(db, planId = v1.plan.id, planExerciseId = v1.exercises[0].id)
        db.planHistoryDao().insertChange(
            planChange(planId = v2.plan.id, previousPlanId = v1.plan.id, reviewId = "rev_1"),
        )

        gymDao.deleteGym("gym_1")

        TABLES_UNDER_GYM.forEach { assertEquals(0, db.count(it), it) }
    }

    @Test
    fun loadsTheCatalogIdempotently() = runBlocking {
        db.equipmentDao().upsertCatalog(listOf(CHEST_PRESS))
        db.equipmentDao().upsertCatalog(listOf(CHEST_PRESS))

        assertEquals(listOf(CHEST_PRESS), db.equipmentDao().observeCatalog().first())
    }

    @Test
    fun reloadingTheCatalogUpdatesTheTextOfAKnownKey() = runBlocking {
        val renamed = CHEST_PRESS.copy(name = "Press de pecho", aliases = listOf("Chest Press"))

        db.equipmentDao().upsertCatalog(listOf(CHEST_PRESS))
        db.equipmentDao().upsertCatalog(listOf(renamed))

        assertEquals(listOf(renamed), db.equipmentDao().observeCatalog().first())
    }

    @Test
    fun syncingGivesCatalogEquipmentTheNameOfItsEntry() = runBlocking {
        gymDao.upsertGym(gym("gym_1"))
        db.equipmentDao().upsertCatalog(listOf(CHEST_PRESS.copy(name = "Press de pecho")))
        db.equipmentDao().upsertEquipment(
            listOf(
                equipment("eq_catalog").copy(catalogKey = "chest_press", name = "Chest Press", isCustom = false),
                equipment("eq_custom").copy(name = "Chest Press casera"),
                equipment("eq_gone").copy(catalogKey = "no_longer_there", name = "Vieja", isCustom = false),
            ),
        )

        db.equipmentDao().syncCatalogNames()

        assertEquals(
            mapOf("eq_catalog" to "Press de pecho", "eq_custom" to "Chest Press casera", "eq_gone" to "Vieja"),
            db.equipmentDao().observeEquipment("gym_1").first().associate { it.id to it.name },
        )
    }

    private companion object {
        val CHEST_PRESS = EquipmentCatalogEntity(
            key = "chest_press",
            name = "Chest Press",
            aliases = listOf("Press de pecho", "Prensa de pecho"),
            category = EquipmentCategory.MACHINE,
            loadType = LoadType.SELECTOR,
            primaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
        )

        val TABLES_UNDER_GYM = listOf(
            "gym",
            "equipment",
            "plan",
            "plan_day",
            "plan_exercise",
            "plan_exercise_alternative",
            "plan_change",
            "export_record",
            "review_schedule",
            "review",
            "check_in",
            "exercise_feedback",
            "change_proposal",
        )
    }
}
