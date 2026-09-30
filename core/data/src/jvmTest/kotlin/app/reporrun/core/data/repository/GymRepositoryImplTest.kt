package app.reporrun.core.data.repository

import app.reporrun.core.data.GYM_ID
import app.reporrun.core.data.T0
import app.reporrun.core.data.equipment
import app.reporrun.core.data.gym
import app.reporrun.core.data.plan
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class GymRepositoryImplTest {
    private val db = inMemoryDatabase()
    private val repository = GymRepositoryImpl(db.gymDao(), db.equipmentDao())

    @AfterTest
    fun close() = db.close()

    @Test
    fun savesAndListsGymsOldestFirst() = runTest {
        val second = gym(GymId("gym_2"), createdAt = T0 + 1.days)
        repository.saveGym(second)
        repository.saveGym(gym())

        assertEquals(listOf(gym(), second), repository.observeGyms().first())
    }

    @Test
    fun keepsASingleActiveGym() = runTest {
        repository.saveGym(gym(isActive = true))
        repository.saveGym(gym(GymId("gym_2")))

        repository.setActiveGym(GymId("gym_2"))

        assertEquals(GymId("gym_2"), repository.observeActiveGym().first()?.id)
        assertEquals(1, repository.observeGyms().first().count { it.isActive })
    }

    @Test
    fun hasNoActiveGymUntilOneIsSet() = runTest {
        repository.saveGym(gym())

        assertNull(repository.observeActiveGym().first())
    }

    @Test
    fun savesAndReadsBackTheInventory() = runTest {
        repository.saveGym(gym())
        val inventory = listOf(equipment("eq_b"), equipment("eq_a"))

        repository.saveEquipment(inventory)

        assertEquals(inventory.sortedBy { it.name }, repository.observeEquipment(GYM_ID).first())
    }

    @Test
    fun deletesEquipmentThatNoPlanUses() = runTest {
        repository.saveGym(gym())
        repository.saveEquipment(listOf(equipment("eq_spare")))

        assertEquals(EquipmentDeletion.Deleted, repository.deleteEquipment(EquipmentId("eq_spare")))
        assertEquals(emptyList(), repository.observeEquipment(GYM_ID).first())
    }

    @Test
    fun refusesToDeleteEquipmentAPlanUsesInsteadOfThrowing() = runTest {
        db.seedGymWithPlanEquipment()
        PlanRepositoryImpl(db.planDao(), db.planHistoryDao()).savePlan(plan())

        // eq_db es el aparato del carry y alternativa del press: dos usos.
        assertEquals(EquipmentDeletion.InUse(usages = 2), repository.deleteEquipment(EquipmentId("eq_db")))
        assertEquals(4, repository.observeEquipment(GYM_ID).first().size)
    }

    @Test
    fun deletingAGymTakesItsInventoryAndPlans() = runTest {
        db.seedGymWithPlanEquipment()
        val plans = PlanRepositoryImpl(db.planDao(), db.planHistoryDao())
        plans.savePlan(plan())

        repository.deleteGym(GYM_ID)

        assertEquals(emptyList(), repository.observeEquipment(GYM_ID).first())
        assertEquals(emptyList(), plans.observeHistory(GYM_ID).first())
    }
}
