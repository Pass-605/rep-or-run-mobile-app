package app.reporrun.feature.onboarding

import app.reporrun.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Fakes escritos a mano, en memoria. El test de un ViewModel usa los casos de uso reales
 * sobre repositorios fake: así se prueba también que el ViewModel entiende sus resultados.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

internal class FakeSettingsRepository(consentAcceptedAt: Instant? = null) : SettingsRepository {
    private val consent = MutableStateFlow(consentAcceptedAt)

    /** Cuántas veces se ha escrito el consentimiento. */
    var acceptCalls = 0
        private set

    val consentAcceptedAt: Instant?
        get() = consent.value

    override fun observeConsentAcceptedAt(): Flow<Instant?> = consent

    override suspend fun acceptConsent(acceptedAt: Instant) {
        acceptCalls++
        consent.value = acceptedAt
    }

    override suspend fun revokeConsent() {
        consent.value = null
    }

    override suspend fun installId(): String = "install-test"
}
