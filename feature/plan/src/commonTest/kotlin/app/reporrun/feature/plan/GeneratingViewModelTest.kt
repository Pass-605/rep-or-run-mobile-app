package app.reporrun.feature.plan

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.GeneratePlanUseCase
import app.reporrun.core.domain.usecase.GenerationUnavailable
import app.reporrun.core.domain.validation.PlanValidator
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import kotlin.time.Duration.Companion.seconds

/**
 * El caso de uso real sobre repositorios fake y un generador que tarda, como el de `:core:ai`.
 * El tiempo es virtual: `runTest` usa el planificador del dispatcher puesto como `Main`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeneratingViewModelTest {
    private val generator = ScriptedPlanGenerator(latency = 3.seconds)
    private val plans = FakePlanRepository()
    private var settings = FakeSettingsRepository(NOW)

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()): GeneratingViewModel {
        val clock = FixedClock(NOW)
        val summary = BuildGenerationSummaryUseCase(
            FakeProfileRepository(profile()),
            FakeGymRepository(inventory(5)),
            FakePreferencesRepository(preferences()),
            settings,
            clock,
            TimeZone.UTC,
        )
        return GeneratingViewModel(
            GeneratePlanUseCase(summary, generator, PlanValidator(), plans, clock, TimeZone.UTC),
            VirtualClock { dispatcher.scheduler.currentTime },
            savedState,
        )
    }

    // ------------------------------------------------------------ espera y éxito

    @Test
    fun asksForThePlanOnceOnEntering() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.state.value.isGenerating)
        assertEquals(1, generator.calls)
    }

    @Test
    fun keepsWaitingWhileTheServiceHasNotAnswered() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            advanceTimeBy(2.seconds)

            assertTrue(viewModel.state.value.isGenerating)
            expectNoEvents()
        }
    }

    @Test
    fun opensThePlanOnceItIsSaved() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            advanceUntilIdle()

            assertEquals(GeneratingEffect.PlanGenerated(PlanId("ROR-2026-09-21-v1")), awaitItem())
            expectNoEvents()
        }
        assertEquals(PlanStatus.DRAFT, plans.saved.single().status)
    }

    // ------------------------------------------------------------ cancelar

    @Test
    fun cancellingStopsTheGenerationAndSavesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            advanceTimeBy(1.seconds)
            viewModel.onEvent(GeneratingEvent.CloseClicked)

            assertEquals(GeneratingEffect.BackToSummary, awaitItem())
            // Pasa de sobra el tiempo en que habría llegado el plan.
            advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun cancellingTwiceLeavesOnce() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(GeneratingEvent.CloseClicked)
            viewModel.onEvent(GeneratingEvent.CloseClicked)

            assertEquals(GeneratingEffect.BackToSummary, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun closingAfterThePlanIsSavedDoesNotGoBackToTheSummary() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            advanceUntilIdle()
            viewModel.onEvent(GeneratingEvent.CloseClicked)

            assertIs<GeneratingEffect.PlanGenerated>(awaitItem())
            expectNoEvents()
        }
    }

    // ------------------------------------------------------------ reintentable

    @Test
    fun aNetworkErrorCanBeRetriedAndRetryingAsksAgain() = runTest {
        generator.thenFails(AiError.Network)
        generator.thenSucceeds()
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(
            GenerationFailure.Unavailable(GenerationUnavailable.Network),
            viewModel.state.value.failure,
        )

        viewModel.effects.test {
            viewModel.onEvent(GeneratingEvent.RetryClicked)
            assertTrue(viewModel.state.value.isGenerating)
            advanceUntilIdle()

            assertIs<GeneratingEffect.PlanGenerated>(awaitItem())
        }
        assertEquals(2, generator.calls)
    }

    // ------------------------------------------------------------ límite de peticiones

    @Test
    fun aRateLimitKeepsTheSecondsAndStartsCountingThem() = runTest {
        generator.thenFails(AiError.RateLimited(retryAfterSeconds = 30))
        val viewModel = viewModel()

        // Lo que tarda el servicio en contestar, y ni un instante más.
        advanceTimeBy(3.seconds)
        runCurrent()

        val state = viewModel.state.value
        assertEquals(GenerationFailure.Unavailable(GenerationUnavailable.RateLimited(30)), state.failure)
        assertEquals(30, state.secondsUntilRetry)
        assertFalse(state.canRetryNow)
    }

    @Test
    fun theCountdownGoesDownWithTheClock() = runTest {
        generator.thenFails(AiError.RateLimited(retryAfterSeconds = 30))
        val viewModel = viewModel()
        advanceTimeBy(3.seconds)
        runCurrent()

        advanceTimeBy(12.seconds)
        runCurrent()

        assertEquals(18, viewModel.state.value.secondsUntilRetry)
    }

    @Test
    fun retryingBeforeTheWaitIsOverAsksNothing() = runTest {
        generator.thenFails(AiError.RateLimited(retryAfterSeconds = 30))
        val viewModel = viewModel()
        advanceTimeBy(3.seconds + 29.seconds)
        runCurrent()
        assertEquals(1, viewModel.state.value.secondsUntilRetry)

        viewModel.onEvent(GeneratingEvent.RetryClicked)

        assertEquals(1, generator.calls)
        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun retryIsAllowedOnceTheWaitIsOver() = runTest {
        generator.thenFails(AiError.RateLimited(retryAfterSeconds = 30))
        generator.thenSucceeds()
        val viewModel = viewModel()
        advanceTimeBy(3.seconds + 30.seconds)
        runCurrent()
        assertTrue(viewModel.state.value.canRetryNow)

        viewModel.effects.test {
            viewModel.onEvent(GeneratingEvent.RetryClicked)
            advanceUntilIdle()

            assertIs<GeneratingEffect.PlanGenerated>(awaitItem())
        }
        assertEquals(2, generator.calls)
    }

    @Test
    fun aRateLimitWithoutSecondsCanBeRetriedAtOnce() = runTest {
        generator.thenFails(AiError.RateLimited(retryAfterSeconds = null))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canRetryNow)

        viewModel.onEvent(GeneratingEvent.RetryClicked)

        assertEquals(2, generator.calls)
    }

    @Test
    fun aNewWaitDoesNotInheritTheCountdownOfTheLastOne() = runTest {
        generator.thenFails(AiError.RateLimited(retryAfterSeconds = 5))
        generator.thenFails(AiError.Network)
        val viewModel = viewModel()
        advanceTimeBy(3.seconds + 5.seconds)
        runCurrent()

        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        assertEquals(GenerationFailure.Unavailable(GenerationUnavailable.Network), viewModel.state.value.failure)
        assertTrue(viewModel.state.value.canRetryNow)
    }

    @Test
    fun retryingWhileWaitingDoesNotAskTwice() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(GeneratingEvent.RetryClicked)

        assertEquals(1, generator.calls)
    }

    // ------------------------------------------------------------ plan inválido

    @Test
    fun anInvalidPlanOffersGeneratingAgainAndSavesNothing() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        val viewModel = viewModel()

        advanceUntilIdle()

        val failure = assertIs<GenerationFailure.InvalidPlan>(viewModel.state.value.failure)
        assertTrue(failure.canGenerateAgain)
        assertTrue(failure.problems.single().contains("eq_ajeno"))
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun aPlanTheServiceRejectsIsAnInvalidPlanToo() = runTest {
        // Lo que enseña `FakeAiConfig(failure = AiError.InvalidResponse(…))` en el dispositivo.
        generator.thenFails(AiError.InvalidResponse(listOf("plan.days[0]: regla 6")))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(
            GenerationFailure.InvalidPlan(listOf("plan.days[0]: regla 6"), canGenerateAgain = true),
            viewModel.state.value.failure,
        )
    }

    @Test
    fun twoInvalidPlansInARowStopOfferingToGenerate() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        generator.thenFails(AiError.InvalidResponse(listOf("plan.days[0]: regla 6")))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        assertEquals(
            GenerationFailure.InvalidPlan(listOf("plan.days[0]: regla 6"), canGenerateAgain = false),
            viewModel.state.value.failure,
        )
        assertEquals(MAX_INVALID_PLANS_IN_A_ROW, generator.calls)
    }

    @Test
    fun afterTheLimitNothingAsksForAnotherPlan() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        assertEquals(2, generator.calls)
        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun afterTheLimitTheOnlyWayOutIsTheSummary() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(GeneratingEvent.CloseClicked)

            assertEquals(GeneratingEffect.BackToSummary, awaitItem())
        }
    }

    @Test
    fun aFailureThatIsNotAnInvalidPlanRestartsTheCount() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        generator.thenFails(AiError.Network)
        generator.thenReturnsAPlanWithForeignEquipment()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        val failure = assertIs<GenerationFailure.InvalidPlan>(viewModel.state.value.failure)
        assertTrue(failure.canGenerateAgain)
    }

    @Test
    fun enteringAgainStartsTheCountFromZero() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        val first = viewModel()
        advanceUntilIdle()
        first.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        // Salir y volver a entrar es un ViewModel nuevo.
        val second = viewModel()
        advanceUntilIdle()

        val failure = assertIs<GenerationFailure.InvalidPlan>(second.state.value.failure)
        assertTrue(failure.canGenerateAgain)
    }

    // ------------------------------------------------------------ proceso muerto

    @Test
    fun restoringAfterTheProcessDiedDoesNotAskForAPlan() = runTest {
        // Lo que queda de la primera vida del proceso: la marca, y nada más.
        val savedState = SavedStateHandle()
        viewModel(savedState)
        generator.thenSucceeds()

        val restored = viewModel(SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) }))
        advanceUntilIdle()

        assertEquals(GenerationFailure.Interrupted, restored.state.value.failure)
        // Una sola llamada: la de la primera vida. El restaurado no ha pedido nada.
        assertEquals(1, generator.calls)
    }

    @Test
    fun anInterruptedGenerationCanBeAskedAgainByThePerson() = runTest {
        val restored = viewModel(SavedStateHandle(mapOf("generation_started" to true)))
        assertEquals(0, generator.calls)

        restored.effects.test {
            restored.onEvent(GeneratingEvent.RetryClicked)
            assertTrue(restored.state.value.isGenerating)
            advanceUntilIdle()

            assertIs<GeneratingEffect.PlanGenerated>(awaitItem())
        }
        assertEquals(1, generator.calls)
    }

    @Test
    fun anInterruptionDoesNotCountTowardsTheLimit() = runTest {
        generator.thenReturnsAPlanWithForeignEquipment()
        val restored = viewModel(SavedStateHandle(mapOf("generation_started" to true)))

        restored.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        // Es el primer plan inválido, no el segundo: la interrupción no era uno.
        val failure = assertIs<GenerationFailure.InvalidPlan>(restored.state.value.failure)
        assertTrue(failure.canGenerateAgain)
    }

    @Test
    fun enteringOnPurposeGeneratesEvenAfterAnInterruption() = runTest {
        viewModel(SavedStateHandle(mapOf("generation_started" to true)))

        // Salir y volver a entrar es otra entrada de la pila, con su estado guardado vacío.
        viewModel()

        assertEquals(1, generator.calls)
    }

    // ------------------------------------------------------------ datos que no sirven

    @Test
    fun inputTheServiceRefusesCannotBeAskedAgain() = runTest {
        generator.thenFails(AiError.InvalidInput(listOf("gym.equipment: sin aparatos de fuerza")))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(
            GenerationFailure.InvalidInput(listOf("gym.equipment: sin aparatos de fuerza")),
            viewModel.state.value.failure,
        )

        viewModel.onEvent(GeneratingEvent.RetryClicked)
        advanceUntilIdle()

        assertEquals(1, generator.calls)
    }

    @Test
    fun goesBackToTheSummaryWhenSomethingIsMissing() = runTest {
        settings = FakeSettingsRepository(consentAcceptedAt = null)
        val viewModel = viewModel()

        viewModel.effects.test {
            assertEquals(GeneratingEffect.BackToSummary, awaitItem())
            expectNoEvents()
        }
        assertEquals(0, generator.calls)
        assertNull(viewModel.state.value.failure)
    }
}
