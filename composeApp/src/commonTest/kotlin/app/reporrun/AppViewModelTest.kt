package app.reporrun

import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel(settings: SettingsRepository) = AppViewModel(ObserveConsentUseCase(settings))

    @Test
    fun startsInOnboardingWithoutConsent() = runTest {
        val viewModel = viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = null)))

        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    @Test
    fun skipsOnboardingWhenConsentWasAlreadyGiven() = runTest {
        val viewModel = viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)))

        assertEquals(StartDestination.Main, viewModel.state.value.start)
    }

    @Test
    fun staysLoadingUntilStorageAnswers() = runTest {
        val settings = FakeSettingsRepository(stored = null)
        val viewModel = viewModel(settings)
        assertEquals(StartDestination.Loading, viewModel.state.value.start)

        settings.stored.value = Loaded(consentAcceptedAt = null)

        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    @Test
    fun doesNotChangeTheStartWhenConsentIsGivenLater() = runTest {
        val settings = FakeSettingsRepository(Loaded(consentAcceptedAt = null))
        val viewModel = viewModel(settings)

        settings.acceptConsent(NOW)

        // A dónde se va al aceptar lo decide la navegación, no el destino inicial.
        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-05T10:00:00Z")
    }
}

/** Lo que hay guardado, una vez leído. Distingue «sin consentimiento» de «aún no se ha leído». */
private class Loaded(val consentAcceptedAt: Instant?)

/** [stored] en `null` imita a un DataStore que todavía no ha contestado. */
private class FakeSettingsRepository(stored: Loaded?) : SettingsRepository {
    val stored = MutableStateFlow(stored)

    override fun observeConsentAcceptedAt(): Flow<Instant?> = stored.filterNotNull().map { it.consentAcceptedAt }

    override suspend fun acceptConsent(acceptedAt: Instant) {
        stored.value = Loaded(acceptedAt)
    }

    override suspend fun revokeConsent() {
        stored.value = Loaded(null)
    }

    override suspend fun installId(): String = "install-test"
}
