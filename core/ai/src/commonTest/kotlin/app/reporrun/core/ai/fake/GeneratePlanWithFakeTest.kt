package app.reporrun.core.ai.fake

import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.GeneratePlanResult
import app.reporrun.core.domain.usecase.GeneratePlanUseCase
import app.reporrun.core.domain.usecase.GenerationUnavailable
import app.reporrun.core.domain.validation.PlanValidator
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import app.reporrun.core.model.PlanSummary
import app.reporrun.core.model.ThemeMode
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * `GeneratePlanUseCase` sobre el fake de verdad: lo que la app hace hoy de punta a punta. El
 * fake es determinista, así que el camino de error se prueba con su modo error
 * ([FakeAiConfig.failure]), no llamando dos veces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeneratePlanWithFakeTest {
    private val plans = SavedPlans()

    @Test
    fun theFakePlanPassesTheValidatorAndIsSavedAsDraft() = runTest {
        val result = useCase(FakeAiConfig())()

        val saved = assertIs<GeneratePlanResult.Saved>(result).plan
        assertEquals(listOf(saved), plans.saved)
        assertEquals(PlanId("ROR-2026-10-05-v1"), saved.id)
        assertEquals(PlanStatus.DRAFT, saved.status)
        assertEquals(FAKE_MODEL, saved.model)
        assertTrue(saved.inputSnapshotJson.isNotBlank())
    }

    @Test
    fun errorModeIsReportedAndSavesNothing() = runTest {
        val result = useCase(FakeAiConfig(failure = AiError.Network))()

        assertEquals(GeneratePlanResult.Unavailable(GenerationUnavailable.Network), result)
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun rejectedPlanModeIsDefinitiveAndSavesNothing() = runTest {
        val result = useCase(FakeAiConfig(failure = AiError.InvalidResponse(listOf("plan.days[0]: regla 6"))))()

        assertEquals(GeneratePlanResult.InvalidPlan(listOf("plan.days[0]: regla 6")), result)
        assertTrue(plans.saved.isEmpty())
    }

    @Test
    fun cancellingDuringTheWaitSavesNothing() = runTest {
        val generate = useCase(FakeAiConfig(latency = 3.seconds))

        val job = launch { generate() }
        advanceTimeBy(1.seconds)
        job.cancel()
        job.join()

        assertTrue(plans.saved.isEmpty())
    }

    private fun TestScope.useCase(config: FakeAiConfig): GeneratePlanUseCase {
        val clock = FixedClock()
        val summary = BuildGenerationSummaryUseCase(
            StoredProfile,
            StoredGym,
            StoredPreferences,
            ConsentGiven,
            clock,
            TimeZone.UTC,
        )
        val generator =
            FakePlanGenerator(config, SequentialIds(), clock, AppDispatchers(StandardTestDispatcher(testScheduler)))
        return GeneratePlanUseCase(summary, generator, PlanValidator(), plans, clock, TimeZone.UTC)
    }
}

// Repositorios de solo lectura con los datos del ejemplo del contrato: aquí nada escribe salvo
// el guardado del plan.

private const val READ_ONLY = "repositorio de solo lectura"

private object StoredProfile : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = flowOf(profile())

    override suspend fun saveProfile(profile: UserProfile) = error(READ_ONLY)
}

private object StoredPreferences : PreferencesRepository {
    override fun observePreferences(): Flow<BlockPreferences?> = flowOf(preferences())

    override suspend fun savePreferences(preferences: BlockPreferences) = error(READ_ONLY)
}

private object ConsentGiven : SettingsRepository {
    override fun observeConsentAcceptedAt(): Flow<Instant?> = flowOf(NOW)

    override suspend fun acceptConsent(acceptedAt: Instant) = error(READ_ONLY)

    override suspend fun revokeConsent() = error(READ_ONLY)

    override suspend fun installId(): String = "install-test"

    override fun observeThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = error(READ_ONLY)
}

private object StoredGym : GymRepository {
    override fun observeGyms(): Flow<List<Gym>> = flowOf(listOf(gym()))

    override fun observeActiveGym(): Flow<Gym?> = flowOf(gym())

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> = flowOf(exampleInventory())

    override suspend fun saveGym(gym: Gym) = error(READ_ONLY)

    override suspend fun setActiveGym(gymId: GymId) = error(READ_ONLY)

    override suspend fun deleteGym(gymId: GymId) = error(READ_ONLY)

    override suspend fun saveEquipment(equipment: List<Equipment>) = error(READ_ONLY)

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion = error(READ_ONLY)
}

/** Solo lo que usa la generación: el contador de versión y el guardado. */
private class SavedPlans : PlanRepository {
    val saved = mutableListOf<Plan>()

    override suspend fun planIdsOn(date: LocalDate): List<PlanId> =
        saved.map { it.id }.filter { it.value.startsWith(PlanId.prefixOf(date)) }

    override suspend fun savePlan(plan: Plan) {
        saved += plan
    }

    override fun observePlan(planId: PlanId): Flow<Plan?> = error(READ_ONLY)

    override fun observeActivePlan(gymId: GymId): Flow<Plan?> = error(READ_ONLY)

    override fun observeHistory(gymId: GymId): Flow<List<PlanSummary>> = error(READ_ONLY)

    override suspend fun activatePlan(planId: PlanId) = error(READ_ONLY)

    override suspend fun setDayStatus(dayId: PlanDayId, status: DayStatus) = error(READ_ONLY)

    override fun observeChanges(gymId: GymId): Flow<List<PlanChange>> = error(READ_ONLY)

    override suspend fun saveChange(change: PlanChange) = error(READ_ONLY)

    override fun observeExports(planId: PlanId): Flow<List<ExportRecord>> = error(READ_ONLY)

    override suspend fun saveExport(record: ExportRecord) = error(READ_ONLY)
}
