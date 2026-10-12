package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.validation.PlanValidator
import app.reporrun.core.model.GymId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GeneratePlanUseCaseTest {
    private val profiles = FakeProfileRepository(profile())
    private val gyms = FakeGymRepository()
    private val preferences = FakePreferencesRepository(preferences())
    private val settings = FakeSettingsRepository(NOW)
    private val plans = FakePlanRepository()
    private val generator = RecordingPlanGenerator()

    private fun useCase(now: Instant = NOW, timeZone: TimeZone = TimeZone.UTC): GeneratePlanUseCase {
        val clock = FixedClock(now)
        return GeneratePlanUseCase(
            buildSummary = BuildGenerationSummaryUseCase(profiles, gyms, preferences, settings, clock, timeZone),
            generator = generator,
            validator = PlanValidator(),
            planRepository = plans,
            clock = clock,
            timeZone = timeZone,
        )
    }

    private suspend fun activeGymWithInventory() {
        gyms.saveGym(gym(isActive = true))
        gyms.saveEquipment(inventory(InventoryRules.MIN_EQUIPMENT_TO_GENERATE))
    }

    // ------------------------------------------------------------ éxito

    @Test
    fun savesAValidPlanAsDraftWithItsMetadata() = runTest {
        activeGymWithInventory()

        val saved = assertIs<GeneratePlanResult.Saved>(useCase()()).plan

        assertEquals(listOf(saved), plans.saved)
        assertEquals(PlanId("ROR-2026-09-21-v1"), saved.id)
        assertEquals(PlanStatus.DRAFT, saved.status)
        assertEquals(GYM_ID, saved.gymId)
        assertEquals(NOW, saved.createdAt)
        assertNull(saved.reviewDate)
        val generated = generatedPlan()
        assertEquals(generated.days, saved.days)
        assertEquals(generated.inputSnapshotJson, saved.inputSnapshotJson)
        assertEquals(generated.model, saved.model)
        assertEquals(generated.promptVersion, saved.promptVersion)
        assertEquals(generated.schemaVersion, saved.schemaVersion)
    }

    @Test
    fun sendsWhatTheSummaryShows() = runTest {
        activeGymWithInventory()

        useCase()()

        val request = generator.requests.single()
        assertEquals(profile(), request.profile)
        assertEquals(GYM_ID, request.gym.id)
        assertEquals(inventory(InventoryRules.MIN_EQUIPMENT_TO_GENERATE), request.equipment)
        assertEquals(preferences(), request.preferences)
    }

    // ------------------------------------------------------------ bloqueos

    @Test
    fun sendsNothingWithoutConsent() = runTest {
        activeGymWithInventory()
        settings.revokeConsent()

        val result = useCase()()

        assertEquals(GeneratePlanResult.Blocked(listOf(GenerationBlocker.MissingConsent)), result)
        assertTrue(generator.requests.isEmpty())
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun reportsEverythingThatIsMissing() = runTest {
        gyms.saveGym(gym(isActive = true))
        gyms.saveEquipment(inventory(2))
        settings.revokeConsent()

        val result = useCase()()

        assertEquals(
            GeneratePlanResult.Blocked(
                listOf(
                    GenerationBlocker.NotEnoughEquipment(current = 2, required = 5),
                    GenerationBlocker.MissingConsent,
                ),
            ),
            result,
        )
        assertTrue(generator.requests.isEmpty())
    }

    // ------------------------------------------------------------ versión

    @Test
    fun secondPlanOfTheDayIsV2AndKeepsTheFirst() = runTest {
        activeGymWithInventory()
        val generate = useCase()

        generate()
        val second = assertIs<GeneratePlanResult.Saved>(generate()).plan

        assertEquals(PlanId("ROR-2026-09-21-v2"), second.id)
        assertEquals(listOf(1, 2), plans.saved.map { it.version })
    }

    @Test
    fun numbersAfterTheHighestVersionOfTheDayInAnyGym() = runTest {
        activeGymWithInventory()
        plans.savePlan(draft("ROR-2026-09-21-v1", GymId("gym_norte")))
        plans.savePlan(draft("ROR-2026-09-21-v3", GymId("gym_norte")))

        val saved = assertIs<GeneratePlanResult.Saved>(useCase()()).plan

        assertEquals(PlanId("ROR-2026-09-21-v4"), saved.id)
    }

    @Test
    fun plansOfOtherDaysDoNotCount() = runTest {
        activeGymWithInventory()
        plans.savePlan(draft("ROR-2026-09-20-v5"))

        val saved = assertIs<GeneratePlanResult.Saved>(useCase()()).plan

        assertEquals(PlanId("ROR-2026-09-21-v1"), saved.id)
    }

    @Test
    fun datesTheIdInThePersonsTimeZone() = runTest {
        activeGymWithInventory()
        // Las 03:30 del 22 en UTC son las 22:30 del 21 cinco horas al oeste.
        val generate = useCase(Instant.parse("2026-09-22T03:30:00Z"), UtcOffset(hours = -5).asTimeZone())

        val saved = assertIs<GeneratePlanResult.Saved>(generate()).plan

        assertEquals(PlanId("ROR-2026-09-21-v1"), saved.id)
    }

    // ------------------------------------------------------------ fecha de inicio

    @Test
    fun dropsAStartDateThatAlreadyPassed() = runTest {
        activeGymWithInventory()
        preferences.savePreferences(preferences().copy(startDate = LocalDate(2026, 9, 20)))

        val saved = assertIs<GeneratePlanResult.Saved>(useCase()()).plan

        assertNull(generator.requests.single().preferences.startDate)
        assertNull(saved.startDate)
    }

    @Test
    fun keepsAStartDateFromTodayOn() = runTest {
        activeGymWithInventory()
        preferences.savePreferences(preferences().copy(startDate = LocalDate(2026, 9, 21)))

        val saved = assertIs<GeneratePlanResult.Saved>(useCase()()).plan

        assertEquals(LocalDate(2026, 9, 21), generator.requests.single().preferences.startDate)
        assertEquals(LocalDate(2026, 9, 21), saved.startDate)
    }

    // ------------------------------------------------------------ plan inválido

    @Test
    fun discardsAnInvalidPlanWithoutSavingOrAskingAgain() = runTest {
        activeGymWithInventory()
        val invalid = generatedPlan(days = listOf(planDay(planExercise("L01", 1, equipmentId = "eq_ajeno"))))
        generator.answer = { PlanGenerationResult.Success(invalid) }

        val result = assertIs<GeneratePlanResult.InvalidPlan>(useCase()())

        assertEquals(
            listOf(
                "plan.days[0].exercises[0].equipmentId: regla 1, MONDAY, L01: " +
                    "el aparato eq_ajeno no está en gym.equipment",
            ),
            result.problems,
        )
        assertEquals(1, generator.requests.size)
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun aRejectedPlanFromTheServiceIsNotAskedForAgain() = runTest {
        activeGymWithInventory()
        // Donde caerá el `422 PLAN_INVALID` del proxy (A-13).
        generator.fails(AiError.InvalidResponse(listOf("plan.days[0]: regla 6")))

        val result = useCase()()

        assertEquals(GeneratePlanResult.InvalidPlan(listOf("plan.days[0]: regla 6")), result)
        assertEquals(1, generator.requests.size)
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun inputTheServiceRefusesIsNotRetryable() = runTest {
        activeGymWithInventory()
        generator.fails(AiError.InvalidInput(listOf("gym.equipment: sin aparatos de fuerza")))

        val result = useCase()()

        assertEquals(GeneratePlanResult.InvalidInput(listOf("gym.equipment: sin aparatos de fuerza")), result)
        assertTrue(plans.saved.isEmpty())
    }

    // ------------------------------------------------------------ reintentable

    @Test
    fun aNetworkErrorIsRetryableAndSavesNothing() = runTest {
        activeGymWithInventory()
        generator.fails(AiError.Network)

        val result = useCase()()

        assertEquals(GeneratePlanResult.Unavailable(GenerationUnavailable.Network), result)
        assertEquals(1, generator.requests.size)
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun aTimeoutIsRetryable() = runTest {
        activeGymWithInventory()
        generator.fails(AiError.Timeout)

        assertEquals(GeneratePlanResult.Unavailable(GenerationUnavailable.Timeout), useCase()())
    }

    @Test
    fun aRateLimitIsRetryableAndSaysHowLongToWait() = runTest {
        activeGymWithInventory()
        generator.fails(AiError.RateLimited(retryAfterSeconds = 30))

        assertEquals(GeneratePlanResult.Unavailable(GenerationUnavailable.RateLimited(30)), useCase()())
    }

    @Test
    fun aServiceFailureIsRetryable() = runTest {
        activeGymWithInventory()
        generator.fails(AiError.Upstream)

        assertEquals(GeneratePlanResult.Unavailable(GenerationUnavailable.Upstream), useCase()())
    }

    // ------------------------------------------------------------ cancelación

    @Test
    fun cancellingWhileWaitingSavesNothing() = runTest {
        activeGymWithInventory()
        val waiting = CompletableDeferred<Unit>()
        generator.answer = {
            waiting.complete(Unit)
            awaitCancellation()
        }
        val generate = useCase()

        val job = launch { generate() }
        waiting.await()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun aPlanThatArrivesAfterCancellingIsNotSaved() = runTest {
        activeGymWithInventory()
        // Un generador que no coopera: devuelve su plan aunque ya nadie lo espere.
        generator.answer = {
            currentCoroutineContext().job.cancel()
            PlanGenerationResult.Success(generatedPlan())
        }
        val generate = useCase()

        val job = launch { generate() }
        job.join()

        assertTrue(job.isCancelled)
        assertTrue(plans.saved.isEmpty())
    }
}
