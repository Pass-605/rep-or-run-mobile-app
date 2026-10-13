package app.reporrun.feature.plan

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.reporrun.core.domain.usecase.ActivatePlanUseCase
import app.reporrun.core.domain.usecase.ApprovePlanDayUseCase
import app.reporrun.core.domain.usecase.ObservePlanUseCase
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Los casos de uso reales sobre repositorios fake: así se prueba también que el ViewModel los entiende. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {
    private val plans = FakePlanRepository()
    private val gyms = FakeGymRepository(inventory(2))

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    /** @param planId el de la ruta; `null` es abrir la vista como destino inicial. */
    private fun viewModel(planId: String? = V1, now: Instant = NOW) = PlanViewModel(
        observePlan = ObservePlanUseCase(plans, gyms),
        approvePlanDay = ApprovePlanDayUseCase(plans),
        activatePlan = ActivatePlanUseCase(plans),
        clock = FixedClock(now),
        timeZone = TimeZone.UTC,
        savedState = SavedStateHandle(if (planId == null) emptyMap() else mapOf(PLAN_ID_ARG to planId)),
    )

    private fun day(dayOfWeek: DayOfWeek, index: Int, planId: String = V1, status: DayStatus = DayStatus.PROPOSED) =
        planDay(planExercise("L01", 1), planExercise("L02", 2, "eq_2")).copy(
            id = PlanDayId("$planId-${dayOfWeek.name}"),
            dayOfWeek = dayOfWeek,
            dayIndex = index,
            status = status,
        )

    /** Un plan de lunes y miércoles. */
    private fun plan(
        id: String = V1,
        status: PlanStatus = PlanStatus.DRAFT,
        days: List<PlanDay> = listOf(day(DayOfWeek.MONDAY, 0, id), day(DayOfWeek.WEDNESDAY, 1, id)),
    ) = Plan(
        id = PlanId(id),
        gymId = CENTRAL,
        createdAt = NOW,
        status = status,
        blockDays = BlockPreferences.BLOCK_DAYS_V1,
        startDate = null,
        reviewDate = null,
        focus = listOf("Fuerza básica"),
        notes = listOf("Las alternativas son «elige una»: no suman series."),
        days = days,
        inputSnapshotJson = "{}",
        model = "modelo-de-prueba",
        promptVersion = "generate-v1",
        schemaVersion = "1.0",
    )

    private fun approvedPlan(id: String = V1, status: PlanStatus = PlanStatus.DRAFT) = plan(
        id = id,
        status = status,
        days = listOf(
            day(DayOfWeek.MONDAY, 0, id, DayStatus.APPROVED),
            day(DayOfWeek.WEDNESDAY, 1, id, DayStatus.APPROVED),
        ),
    )

    // ------------------------------------------------------------ qué plan se enseña

    @Test
    fun showsThePlanOfTheRouteWithTheMachinesOfItsGym() = runTest {
        plans.savePlan(plan())

        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals(PlanId(V1), state.plan?.id)
        assertEquals("Aparato 2", state.details?.equipmentName(EquipmentId("eq_2")))
    }

    @Test
    fun showsADraftWhenItIsTheOneOfTheRoute() = runTest {
        plans.savePlan(approvedPlan(V1, PlanStatus.ACTIVE))
        plans.savePlan(plan(V2))

        assertEquals(PlanId(V2), viewModel(planId = V2).state.value.plan?.id)
    }

    @Test
    fun withoutARouteIdShowsTheActivePlanOfTheActiveGym() = runTest {
        plans.savePlan(plan(V1))
        plans.savePlan(approvedPlan(V2, PlanStatus.ACTIVE))

        assertEquals(PlanId(V2), viewModel(planId = null).state.value.plan?.id)
    }

    @Test
    fun withoutARouteIdAndWithOnlyDraftsThereIsNoPlanAndNothingToDo() = runTest {
        plans.savePlan(plan())

        val state = viewModel(planId = null).state.value

        assertFalse(state.isLoading)
        assertNull(state.plan)
        assertNull(state.selectedDay)
        assertNull(state.action)
    }

    @Test
    fun followsThePlanThatBecomesActiveWhileTheEmptyStateIsShown() = runTest {
        plans.savePlan(approvedPlan())
        val viewModel = viewModel(planId = null)

        viewModel.state.test {
            assertNull(awaitItem().plan)

            plans.activatePlan(PlanId(V1))

            assertEquals(PlanStatus.ACTIVE, awaitItem().plan?.status)
        }
    }

    // ------------------------------------------------------------ el día elegido

    @Test
    fun opensOnTodayWhenTodayHasASession() = runTest {
        plans.savePlan(plan())

        val wednesday = viewModel(now = NOW + 2.days).state.value

        assertEquals(DayOfWeek.WEDNESDAY, wednesday.today)
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.selectedDay?.dayOfWeek)
    }

    @Test
    fun opensOnTheFirstDayWhenTodayHasNoSession() = runTest {
        plans.savePlan(plan())

        val tuesday = viewModel(now = NOW + 1.days).state.value

        assertEquals(DayOfWeek.MONDAY, tuesday.selectedDay?.dayOfWeek)
    }

    @Test
    fun listsTheDaysInBlockOrderWhateverTheStoredOrder() = runTest {
        plans.savePlan(plan(days = listOf(day(DayOfWeek.WEDNESDAY, 1), day(DayOfWeek.MONDAY, 0))))

        val days = viewModel().state.value.days

        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), days.map { it.dayOfWeek })
    }

    @Test
    fun showsTheDayThatIsSelected() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.DaySelected(PlanDayId("$V1-WEDNESDAY")))

        assertEquals(DayOfWeek.WEDNESDAY, viewModel.state.value.selectedDay?.dayOfWeek)
    }

    // ------------------------------------------------------------ aprobar

    @Test
    fun offersApprovingAProposedDayOfADraft() = runTest {
        plans.savePlan(plan())

        assertEquals(PlanAction.ApproveDay, viewModel().state.value.action)
    }

    @Test
    fun approvesOnlyTheSelectedDay() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ApproveDayClicked)

        assertEquals(DayStatus.APPROVED, viewModel.state.value.selectedDay?.status)
        assertEquals(listOf(DayStatus.APPROVED, DayStatus.PROPOSED), plans.saved.single().days.map { it.status })
        assertEquals(PlanStatus.DRAFT, plans.saved.single().status)
    }

    @Test
    fun afterApprovingADayItSaysWhichOnesAreStillPending() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ApproveDayClicked)

        val action = assertIs<PlanAction.ActivatePlan>(viewModel.state.value.action)
        assertFalse(action.enabled)
        assertEquals(listOf(DayOfWeek.WEDNESDAY), action.pendingDays)
    }

    @Test
    fun selectingAProposedDayOffersApprovingAgain() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()
        viewModel.onEvent(PlanEvent.ApproveDayClicked)

        viewModel.onEvent(PlanEvent.DaySelected(PlanDayId("$V1-WEDNESDAY")))

        assertEquals(PlanAction.ApproveDay, viewModel.state.value.action)
    }

    // ------------------------------------------------------------ activar

    @Test
    fun doesNotActivateWhileADayIsPendingEvenIfAsked() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()
        viewModel.onEvent(PlanEvent.ApproveDayClicked)

        viewModel.onEvent(PlanEvent.ActivatePlanClicked)

        assertEquals(PlanStatus.DRAFT, plans.saved.single().status)
    }

    @Test
    fun doesNotActivateFromADayThatOnlyOffersApproving() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ActivatePlanClicked)

        assertEquals(PlanStatus.DRAFT, plans.saved.single().status)
    }

    @Test
    fun offersActivatingOnceEveryDayIsApproved() = runTest {
        plans.savePlan(plan())
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ApproveDayClicked)
        viewModel.onEvent(PlanEvent.DaySelected(PlanDayId("$V1-WEDNESDAY")))
        viewModel.onEvent(PlanEvent.ApproveDayClicked)

        assertTrue(assertIs<PlanAction.ActivatePlan>(viewModel.state.value.action).enabled)
    }

    @Test
    fun activatesThePlanAndThenOffersNothing() = runTest {
        plans.savePlan(approvedPlan())
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ActivatePlanClicked)

        assertEquals(PlanStatus.ACTIVE, viewModel.state.value.plan?.status)
        assertNull(viewModel.state.value.action)
    }

    @Test
    fun activatingArchivesThePlanThatWasActive() = runTest {
        plans.savePlan(approvedPlan(V1, PlanStatus.ACTIVE))
        plans.savePlan(approvedPlan(V2))
        val viewModel = viewModel(planId = V2)

        viewModel.onEvent(PlanEvent.ActivatePlanClicked)

        assertEquals(listOf(PlanStatus.ARCHIVED, PlanStatus.ACTIVE), plans.saved.map { it.status })
    }

    @Test
    fun anActivePlanOffersNeitherApprovingNorActivating() = runTest {
        plans.savePlan(approvedPlan(status = PlanStatus.ACTIVE))
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ApproveDayClicked)
        viewModel.onEvent(PlanEvent.ActivatePlanClicked)

        assertNull(viewModel.state.value.action)
        assertEquals(PlanStatus.ACTIVE, plans.saved.single().status)
    }

    @Test
    fun anArchivedVersionCanBeActivatedAgain() = runTest {
        plans.savePlan(approvedPlan(status = PlanStatus.ARCHIVED))
        val viewModel = viewModel()

        viewModel.onEvent(PlanEvent.ActivatePlanClicked)

        assertEquals(PlanStatus.ACTIVE, plans.saved.single().status)
    }

    private companion object {
        const val V1 = "ROR-2026-09-21-v1"
        const val V2 = "ROR-2026-09-21-v2"
    }
}
