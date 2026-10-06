package app.reporrun.feature.onboarding

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.AcceptConsentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Test de referencia de un ViewModel: se construye a mano (sin Koin ni Compose), con los casos
 * de uso reales sobre repositorios fake, y se le habla solo por `onEvent`.
 *
 * `viewModelScope` corre en `Dispatchers.Main`, que en un test no existe: se sustituye por un
 * `UnconfinedTestDispatcher`, que ejecuta cada corrutina en el acto, así que tras un `onEvent`
 * el estado ya está al día.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val settings = FakeSettingsRepository()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel() = OnboardingViewModel(
        acceptConsent = AcceptConsentUseCase(settings, FixedClock(NOW)),
    )

    @Test
    fun startsWithoutSavingAnything() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.state.value.isSaving)
        assertNull(settings.consentAcceptedAt)
    }

    @Test
    fun storesTheConsentWithItsDateWhenAccepting() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingEvent.AcceptClicked)

        assertEquals(NOW, settings.consentAcceptedAt)
    }

    @Test
    fun emitsConsentAcceptedOnceAfterSaving() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.AcceptClicked)

            assertEquals(OnboardingEffect.ConsentAccepted, awaitItem())
            // Cuando el efecto llega, el consentimiento ya está guardado.
            assertEquals(NOW, settings.consentAcceptedAt)
            expectNoEvents()
        }
    }

    @Test
    fun ignoresASecondTapOnAccept() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.AcceptClicked)
            viewModel.onEvent(OnboardingEvent.AcceptClicked)

            assertEquals(OnboardingEffect.ConsentAccepted, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, settings.acceptCalls)
        // El botón sigue desactivado: la bienvenida se va y no se vuelve a aceptar.
        assertTrue(viewModel.state.value.isSaving)
    }

    @Test
    fun asksForTheNoticeWithoutAccepting() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.ReadNoticeClicked)

            assertEquals(OnboardingEffect.NoticeRequested, awaitItem())
            expectNoEvents()
        }
        assertNull(settings.consentAcceptedAt)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun keepsAnEffectUntilSomeoneCollectsIt() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingEvent.AcceptClicked)

        // Nadie escuchaba cuando se envió (la app estaba en segundo plano): no se pierde.
        viewModel.effects.test {
            assertEquals(OnboardingEffect.ConsentAccepted, awaitItem())
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-05T10:00:00Z")
    }
}
