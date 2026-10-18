package app.reporrun.feature.settings

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.LoadSampleGymUseCase
import app.reporrun.core.domain.usecase.ObserveAiBackendUseCase
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.domain.usecase.ObserveThemeModeUseCase
import app.reporrun.core.domain.usecase.SetAiBackendUseCase
import app.reporrun.core.domain.usecase.SetThemeModeUseCase
import app.reporrun.core.model.AiBackend
import app.reporrun.core.model.Experience
import app.reporrun.core.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private var settings = FakeSettingsRepository(consentAcceptedAt = ACCEPTED_AT)
    private var profiles = FakeProfileRepository(profile())
    private val developerSettings = FakeDeveloperSettingsRepository()
    private val gyms = FakeGymRepository()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel(timeZone: TimeZone = TimeZone.UTC) = SettingsViewModel(
        observeThemeMode = ObserveThemeModeUseCase(settings),
        observeProfile = ObserveProfileUseCase(profiles),
        observeConsent = ObserveConsentUseCase(settings),
        observeAiBackend = ObserveAiBackendUseCase(developerSettings),
        setThemeMode = SetThemeModeUseCase(settings),
        setAiBackend = SetAiBackendUseCase(developerSettings),
        loadSampleGym = LoadSampleGymUseCase(SampleGymFixture, gyms, FakePreferencesRepository(), FixedClock),
        timeZone = timeZone,
    )

    @Test
    fun followsTheSystemThemeWhenNothingWasChosen() = runTest {
        assertEquals(ThemeMode.SYSTEM, viewModel().state.value.themeMode)
    }

    @Test
    fun storesTheChosenThemeAndMarksIt() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
        // Y es lo guardado: otra pantalla que se abra después lo ve.
        assertEquals(ThemeMode.DARK, viewModel().state.value.themeMode)
    }

    @Test
    fun doesNotWriteTheThemeThatIsAlreadyChosen() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.LIGHT))
        viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.LIGHT))

        assertEquals(1, settings.themeWrites)
    }

    @Test
    fun choosingAThemeDoesNotEmitAnyEffect() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.DARK))

            expectNoEvents()
        }
    }

    @Test
    fun showsTheAgeAndExperienceOfTheSavedProfile() = runTest {
        profiles = FakeProfileRepository(profile(age = 41, experience = Experience.ADVANCED))

        assertEquals(ProfileSummary(41, Experience.ADVANCED), viewModel().state.value.profile)
    }

    @Test
    fun updatesTheProfileSummaryWhenTheProfileIsEdited() = runTest {
        val viewModel = viewModel()

        profiles.saveProfile(profile(age = 31))

        assertEquals(31, viewModel.state.value.profile?.age)
    }

    @Test
    fun hasNoProfileSummaryWithoutAProfile() = runTest {
        profiles = FakeProfileRepository(profile = null)

        assertNull(viewModel().state.value.profile)
    }

    @Test
    fun showsTheLocalDayTheNoticeWasAccepted() = runTest {
        // 23:30 UTC del día 19 ya es día 20 en Madrid.
        val viewModel = viewModel(TimeZone.of("Europe/Madrid"))

        assertEquals(LocalDate(2026, 9, 20), viewModel.state.value.consentAcceptedOn)
    }

    @Test
    fun hasNoAcceptanceDateWithoutConsent() = runTest {
        settings = FakeSettingsRepository(consentAcceptedAt = null)

        assertNull(viewModel().state.value.consentAcceptedOn)
    }

    @Test
    fun asksForTheProfile() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ProfileClicked)

            assertEquals(SettingsEffect.ProfileRequested, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun asksForTheNoticeWithoutChangingTheConsent() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.NoticeClicked)

            assertEquals(SettingsEffect.NoticeRequested, awaitItem())
            expectNoEvents()
        }
        assertEquals(ACCEPTED_AT, settings.consentAcceptedAt)
    }

    @Test
    fun asksForTheLicenses() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.LicensesClicked)

            assertEquals(SettingsEffect.LicensesRequested, awaitItem())
            expectNoEvents()
        }
    }

    // ------------------------------------------------------------ depuración

    @Test
    fun generatesWithTheExamplesUntilTheProxyIsChosen() = runTest {
        assertEquals(AiBackend.FAKE, viewModel().state.value.aiBackend)
    }

    @Test
    fun turningTheProxyOnStoresItAndShowsIt() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SettingsEvent.ProxyToggled(useProxy = true))

        assertEquals(AiBackend.PROXY, developerSettings.stored)
        assertEquals(AiBackend.PROXY, viewModel.state.value.aiBackend)

        viewModel.onEvent(SettingsEvent.ProxyToggled(useProxy = false))

        assertEquals(AiBackend.FAKE, viewModel.state.value.aiBackend)
    }

    @Test
    fun loadsTheSampleGymAsTheActiveOne() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SettingsEvent.SampleGymClicked)

        assertEquals(SampleGymLoad.LOADED, viewModel.state.value.sampleGym)
        assertEquals(LoadSampleGymUseCase.SAMPLE_GYM_ID, gyms.observeActiveGym().first()?.id)
    }

    private object FixedClock : Clock {
        override fun now(): Instant = ACCEPTED_AT
    }

    private companion object {
        val ACCEPTED_AT: Instant = Instant.parse("2026-09-19T23:30:00Z")
    }
}
