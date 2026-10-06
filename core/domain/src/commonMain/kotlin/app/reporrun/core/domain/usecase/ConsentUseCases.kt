package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Cuándo aceptó la persona el aviso y el envío de sus datos al LLM, o `null` si no lo ha hecho.
 * Vuelve a emitir cuando el consentimiento se da o se retira.
 */
class ObserveConsentUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<Instant?> = repository.observeConsentAcceptedAt()
}

/**
 * Registra que la persona acepta el aviso y el envío de sus datos al LLM, con la hora del
 * [clock]. Devuelve el instante que queda guardado.
 *
 * Si ya había una aceptación, la conserva: aceptar dos veces no reescribe cuándo se consintió.
 */
class AcceptConsentUseCase(private val repository: SettingsRepository, private val clock: Clock) {
    suspend operator fun invoke(): Instant {
        val alreadyAcceptedAt = repository.observeConsentAcceptedAt().first()
        if (alreadyAcceptedAt != null) return alreadyAcceptedAt
        return clock.now().also { repository.acceptConsent(it) }
    }
}
