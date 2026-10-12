package app.reporrun.feature.plansetup

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.GenerationBlocker
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
import kotlin.test.assertTrue

/** Como `PreferencesViewModelTest`: el caso de uso real sobre repositorios fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    private var profiles = FakeProfileRepository(profile())
    private var gyms = FakeGymRepository(machines(5))
    private var preferences = FakePreferencesRepository(savedPreferences())
    private var settings = FakeSettingsRepository(NOW)

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel() = SummaryViewModel(
        BuildGenerationSummaryUseCase(profiles, gyms, preferences, settings, FixedClock(NOW), TimeZone.UTC),
    )

    @Test
    fun staysLoadingUntilStorageAnswers() = runTest {
        preferences = FakePreferencesRepository(savedPreferences(), answered = false)
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.canGenerate)

        preferences.answer()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun showsWhatIsSavedAndAllowsGeneratingWhenNothingIsMissing() = runTest {
        val state = viewModel().state.value

        assertTrue(state.canGenerate)
        assertEquals(profile(), state.summary?.profile)
        assertEquals(CENTRAL, state.summary?.gym?.id)
        assertEquals(5, state.summary?.equipment?.size)
        assertEquals(75, state.summary?.preferences?.sessionMinutes)
    }

    @Test
    fun keepsEveryBlockerOfTheUseCaseNotOnlyTheFirst() = runTest {
        profiles = FakeProfileRepository()
        gyms = FakeGymRepository(machines(2))
        preferences = FakePreferencesRepository()
        settings = FakeSettingsRepository()

        val state = viewModel().state.value

        assertFalse(state.canGenerate)
        assertEquals(
            listOf(
                GenerationBlocker.MissingProfile,
                GenerationBlocker.NotEnoughEquipment(current = 2, required = 5),
                GenerationBlocker.MissingPreferences,
                GenerationBlocker.MissingConsent,
            ),
            state.summary?.blockers,
        )
    }

    @Test
    fun blocksWithoutAnActiveGym() = runTest {
        gyms = FakeGymRepository(hasGym = false)

        val blockers = viewModel().state.value.summary?.blockers

        assertEquals(listOf<GenerationBlocker>(GenerationBlocker.MissingActiveGym), blockers)
    }

    @Test
    fun stopsBlockingWhenWhatWasMissingIsSaved() = runTest {
        preferences = FakePreferencesRepository()
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.canGenerate)

        preferences.savePreferences(savedPreferences())

        assertTrue(viewModel.state.value.canGenerate)
    }

    @Test
    fun blocksAgainWhenTheInventoryDropsBelowTheMinimum() = runTest {
        val viewModel = viewModel()

        gyms.deleteEquipment(machines(5).first().id)

        assertEquals(
            listOf<GenerationBlocker>(GenerationBlocker.NotEnoughEquipment(current = 4, required = 5)),
            viewModel.state.value.summary?.blockers,
        )
    }

    @Test
    fun asksToGenerateOnceWhenNothingIsMissing() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SummaryEvent.GenerateClicked)

            assertEquals(SummaryEffect.GenerationRequested, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun doesNotAskToGenerateWhileSomethingIsMissing() = runTest {
        settings = FakeSettingsRepository()
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SummaryEvent.GenerateClicked)

            expectNoEvents()
        }
    }
}
