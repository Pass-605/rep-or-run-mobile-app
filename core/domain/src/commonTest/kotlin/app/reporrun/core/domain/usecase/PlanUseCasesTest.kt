package app.reporrun.core.domain.usecase

import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanUseCasesTest {
    private val plans = FakePlanRepository()
    private val gyms = FakeGymRepository()
    private val observePlan = ObservePlanUseCase(plans, gyms)
    private val approvePlanDay = ApprovePlanDayUseCase(plans)
    private val activatePlan = ActivatePlanUseCase(plans)
    private val hasSavedPlans = HasSavedPlansUseCase(plans, gyms)

    /** Un borrador de dos días, lunes y miércoles, con ids de día propios de [id]. */
    private fun twoDayDraft(id: String = V1, gymId: GymId = GYM_ID): Plan = draft(id, gymId).copy(
        days = listOf(
            planDay(planExercise("L01", 1)).copy(id = PlanDayId("$id-monday")),
            planDay(planExercise("X01", 1)).copy(
                id = PlanDayId("$id-wednesday"),
                dayOfWeek = DayOfWeek.WEDNESDAY,
                dayIndex = 1,
            ),
        ),
    )

    private suspend fun stored(id: String = V1): Plan = checkNotNull(plans.observePlan(PlanId(id)).first())

    private suspend fun approveAllDays(id: String = V1) {
        stored(id).days.forEach { approvePlanDay(PlanId(id), it.id) }
    }

    @Test
    fun approvesAProposedDayAndLeavesTheOthersAlone() = runTest {
        plans.savePlan(twoDayDraft())

        val result = approvePlanDay(PlanId(V1), PlanDayId("$V1-monday"))

        assertEquals(ApprovePlanDayResult.Approved, result)
        assertEquals(listOf(DayStatus.APPROVED, DayStatus.PROPOSED), stored().days.map { it.status })
        assertEquals(PlanStatus.DRAFT, stored().status)
    }

    @Test
    fun approvingADayTwiceChangesNothing() = runTest {
        plans.savePlan(twoDayDraft())
        approvePlanDay(PlanId(V1), PlanDayId("$V1-monday"))

        val result = approvePlanDay(PlanId(V1), PlanDayId("$V1-monday"))

        assertEquals(ApprovePlanDayResult.AlreadyApproved, result)
    }

    @Test
    fun doesNotApproveADayOfAnotherPlan() = runTest {
        plans.savePlan(twoDayDraft(V1))
        plans.savePlan(twoDayDraft(V2))

        val result = approvePlanDay(PlanId(V1), PlanDayId("$V2-monday"))

        assertEquals(ApprovePlanDayResult.DayNotFound, result)
        assertTrue(stored(V2).days.all { it.status == DayStatus.PROPOSED })
    }

    @Test
    fun doesNotApproveADayOfAPlanThatDoesNotExist() = runTest {
        assertEquals(ApprovePlanDayResult.DayNotFound, approvePlanDay(PlanId(V1), PlanDayId("$V1-monday")))
    }

    @Test
    fun doesNotApproveADayOfAnArchivedPlan() = runTest {
        plans.savePlan(twoDayDraft().copy(status = PlanStatus.ARCHIVED))

        val result = approvePlanDay(PlanId(V1), PlanDayId("$V1-monday"))

        assertEquals(ApprovePlanDayResult.PlanNotDraft, result)
        assertEquals(DayStatus.PROPOSED, stored().days.first().status)
    }

    @Test
    fun doesNotActivateWhileADayIsStillProposed() = runTest {
        plans.savePlan(twoDayDraft())
        approvePlanDay(PlanId(V1), PlanDayId("$V1-monday"))

        val result = activatePlan(PlanId(V1))

        assertEquals(ActivatePlanResult.DaysPending(pending = 1), result)
        assertEquals(PlanStatus.DRAFT, stored().status)
    }

    @Test
    fun activatesADraftWithEveryDayApproved() = runTest {
        plans.savePlan(twoDayDraft())
        approveAllDays()

        val result = activatePlan(PlanId(V1))

        assertEquals(ActivatePlanResult.Activated, result)
        assertEquals(PlanStatus.ACTIVE, stored().status)
    }

    @Test
    fun activatingArchivesThePreviousActivePlanOfTheSameGymOnly() = runTest {
        plans.savePlan(twoDayDraft(V1))
        plans.savePlan(twoDayDraft(V2))
        plans.savePlan(twoDayDraft(V3, NORTH))
        listOf(V1, V2, V3).forEach { approveAllDays(it) }
        activatePlan(PlanId(V1))
        activatePlan(PlanId(V3))

        activatePlan(PlanId(V2))

        assertEquals(PlanStatus.ARCHIVED, stored(V1).status)
        assertEquals(PlanStatus.ACTIVE, stored(V2).status)
        assertEquals(PlanStatus.ACTIVE, stored(V3).status)
    }

    @Test
    fun reactivatesAnArchivedVersion() = runTest {
        plans.savePlan(twoDayDraft(V1))
        plans.savePlan(twoDayDraft(V2))
        listOf(V1, V2).forEach { approveAllDays(it) }
        activatePlan(PlanId(V1))
        activatePlan(PlanId(V2))

        val result = activatePlan(PlanId(V1))

        assertEquals(ActivatePlanResult.Activated, result)
        assertEquals(PlanStatus.ARCHIVED, stored(V2).status)
    }

    @Test
    fun activatingTheActivePlanChangesNothing() = runTest {
        plans.savePlan(twoDayDraft())
        approveAllDays()
        activatePlan(PlanId(V1))

        assertEquals(ActivatePlanResult.AlreadyActive, activatePlan(PlanId(V1)))
    }

    @Test
    fun doesNotActivateAPlanThatDoesNotExist() = runTest {
        assertEquals(ActivatePlanResult.PlanNotFound, activatePlan(PlanId(V1)))
    }

    @Test
    fun observesAPlanWithTheEquipmentOfItsGym() = runTest {
        gyms.saveEquipment(listOf(equipment("eq_1", name = "Chest Press"), equipment("eq_9", NORTH, name = "Ajeno")))
        plans.savePlan(twoDayDraft())

        val details = checkNotNull(observePlan(PlanId(V1)).first())

        assertEquals(PlanId(V1), details.plan.id)
        assertEquals("Chest Press", details.equipmentName(EquipmentId("eq_1")))
        assertNull(details.equipmentName(EquipmentId("eq_9")))
    }

    @Test
    fun observesNothingForAPlanThatDoesNotExist() = runTest {
        assertNull(observePlan(PlanId(V1)).first())
    }

    @Test
    fun observesTheActivePlanOfTheActiveGym() = runTest {
        gyms.saveGym(gym(isActive = true))
        plans.savePlan(twoDayDraft(V1))
        plans.savePlan(twoDayDraft(V2))
        approveAllDays(V2)
        activatePlan(PlanId(V2))

        assertEquals(PlanId(V2), observePlan.active().first()?.plan?.id)
    }

    @Test
    fun aDraftIsNotTheActivePlan() = runTest {
        gyms.saveGym(gym(isActive = true))
        plans.savePlan(twoDayDraft())

        assertNull(observePlan.active().first())
    }

    @Test
    fun thereIsNoActivePlanWithoutAnActiveGym() = runTest {
        assertNull(observePlan.active().first())
    }

    @Test
    fun theActiveGymHasSavedPlansEvenIfTheyAreOnlyDrafts() = runTest {
        gyms.saveGym(gym(isActive = true))
        plans.savePlan(twoDayDraft())

        assertTrue(hasSavedPlans())
    }

    @Test
    fun plansOfAnotherGymDoNotCountAsSaved() = runTest {
        gyms.saveGym(gym(isActive = true))
        plans.savePlan(twoDayDraft(V1, NORTH))

        assertFalse(hasSavedPlans())
    }

    @Test
    fun thereAreNoSavedPlansWithoutAnActiveGym() = runTest {
        plans.savePlan(twoDayDraft())

        assertFalse(hasSavedPlans())
    }

    private companion object {
        const val V1 = "ROR-2026-09-21-v1"
        const val V2 = "ROR-2026-09-21-v2"
        const val V3 = "ROR-2026-09-21-v3"
        val NORTH = GymId("gym_norte")
    }
}
