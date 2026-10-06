package app.reporrun.core.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ConsentUseCasesTest {
    private val settings = FakeSettingsRepository()
    private val observeConsent = ObserveConsentUseCase(settings)

    @Test
    fun reportsNoConsentBeforeAccepting() = runTest {
        assertNull(observeConsent().first())
    }

    @Test
    fun storesTheClockInstantWhenAccepting() = runTest {
        val accepted = AcceptConsentUseCase(settings, FixedClock(NOW))()

        assertEquals(NOW, accepted)
        assertEquals(NOW, observeConsent().first())
    }

    @Test
    fun keepsTheFirstDateWhenAcceptingAgain() = runTest {
        AcceptConsentUseCase(settings, FixedClock(NOW))()

        val acceptedAgain = AcceptConsentUseCase(settings, FixedClock(NOW + 3.days))()

        assertEquals(NOW, acceptedAgain)
        assertEquals(NOW, observeConsent().first())
    }

    @Test
    fun emitsWhenConsentIsGivenAndWithdrawn() = runTest {
        observeConsent().test {
            assertNull(awaitItem())

            AcceptConsentUseCase(settings, FixedClock(NOW))()
            assertEquals(NOW, awaitItem())

            settings.revokeConsent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun acceptingRemovesTheConsentBlocker() = runTest {
        val buildSummary = BuildGenerationSummaryUseCase(
            FakeProfileRepository(),
            FakeGymRepository(),
            FakePreferencesRepository(),
            settings,
        )
        assertTrue(GenerationBlocker.MissingConsent in buildSummary().first().blockers)

        AcceptConsentUseCase(settings, FixedClock(NOW))()

        assertFalse(GenerationBlocker.MissingConsent in buildSummary().first().blockers)
    }
}
