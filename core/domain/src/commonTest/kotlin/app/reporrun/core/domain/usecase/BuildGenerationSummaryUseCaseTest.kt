package app.reporrun.core.domain.usecase

import app.reporrun.core.model.GymId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildGenerationSummaryUseCaseTest {
    private val profiles = FakeProfileRepository()
    private val gyms = FakeGymRepository()
    private val preferences = FakePreferencesRepository()
    private val settings = FakeSettingsRepository()
    private val buildSummary = BuildGenerationSummaryUseCase(profiles, gyms, preferences, settings)

    private suspend fun completeEverything(machines: Int = InventoryRules.MIN_EQUIPMENT_TO_GENERATE) {
        profiles.saveProfile(profile())
        gyms.saveGym(gym(isActive = true))
        gyms.saveEquipment(inventory(machines))
        preferences.savePreferences(preferences())
        settings.acceptConsent(NOW)
    }

    @Test
    fun allowsGeneratingWithEverythingComplete() = runTest {
        completeEverything()

        val summary = buildSummary().first()

        assertTrue(summary.canGenerate)
        assertEquals(profile(), summary.profile)
        assertEquals(GYM_ID, summary.gym?.id)
        assertEquals(5, summary.equipment.size)
        assertEquals(preferences(), summary.preferences)
        assertTrue(summary.consentAccepted)
    }

    @Test
    fun blocksWithFewerThanFiveMachines() = runTest {
        completeEverything(machines = 4)

        val summary = buildSummary().first()

        assertEquals(
            listOf<GenerationBlocker>(GenerationBlocker.NotEnoughEquipment(current = 4, required = 5)),
            summary.blockers,
        )
        assertEquals(false, summary.canGenerate)
    }

    @Test
    fun blocksWithoutConsent() = runTest {
        completeEverything()
        settings.revokeConsent()

        val summary = buildSummary().first()

        assertEquals(listOf<GenerationBlocker>(GenerationBlocker.MissingConsent), summary.blockers)
    }

    @Test
    fun blocksWithoutPreferences() = runTest {
        profiles.saveProfile(profile())
        gyms.saveGym(gym(isActive = true))
        gyms.saveEquipment(inventory(5))
        settings.acceptConsent(NOW)

        val summary = buildSummary().first()

        assertEquals(listOf<GenerationBlocker>(GenerationBlocker.MissingPreferences), summary.blockers)
    }

    @Test
    fun listsEverythingThatIsMissingOnAFreshInstall() = runTest {
        val summary = buildSummary().first()

        assertEquals(
            listOf(
                GenerationBlocker.MissingProfile,
                GenerationBlocker.MissingActiveGym,
                GenerationBlocker.MissingPreferences,
                GenerationBlocker.MissingConsent,
            ),
            summary.blockers,
        )
        assertTrue(summary.equipment.isEmpty())
    }

    @Test
    fun countsOnlyTheInventoryOfTheActiveGym() = runTest {
        completeEverything(machines = 2)
        gyms.saveGym(gym(GymId("gym_norte")))
        gyms.saveEquipment((1..10).map { equipment("eq_norte_$it", GymId("gym_norte")) })

        val summary = buildSummary().first()

        assertEquals(
            listOf<GenerationBlocker>(GenerationBlocker.NotEnoughEquipment(current = 2, required = 5)),
            summary.blockers,
        )
    }

    @Test
    fun blocksWhenNoGymIsActiveEvenIfOneHasMachines() = runTest {
        completeEverything()
        gyms.saveGym(gym(isActive = false))

        val summary = buildSummary().first()

        assertEquals(listOf<GenerationBlocker>(GenerationBlocker.MissingActiveGym), summary.blockers)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun followsTheInventoryAsItChanges() = runTest {
        completeEverything(machines = 4)
        val emitted = mutableListOf<GenerationSummary>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { buildSummary().toList(emitted) }

        gyms.saveEquipment(listOf(equipment("eq_quinto")))

        assertEquals(listOf(false, true), emitted.map { it.canGenerate })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun followsTheActiveGymWhenItChanges() = runTest {
        completeEverything()
        gyms.saveGym(gym(GymId("gym_norte")))
        val emitted = mutableListOf<GenerationSummary>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { buildSummary().toList(emitted) }

        SetActiveGymUseCase(gyms)(GymId("gym_norte"))

        assertEquals(GymId("gym_norte"), emitted.last().gym?.id)
        assertEquals(
            listOf<GenerationBlocker>(GenerationBlocker.NotEnoughEquipment(current = 0, required = 5)),
            emitted.last().blockers,
        )
    }
}
