package app.reporrun.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Ajustes de la instalación que no son datos de entrenamiento: consentimiento e identificador
 * de instalación. El gimnasio activo no vive aquí: es [GymRepository.observeActiveGym].
 */
interface SettingsRepository {
    /**
     * Cuándo aceptó la persona el aviso y el consentimiento de envío al LLM, o `null` si no lo
     * ha aceptado. Sin consentimiento no se genera nada.
     */
    fun observeConsentAcceptedAt(): Flow<Instant?>

    /** Registra la aceptación en [acceptedAt]. */
    suspend fun acceptConsent(acceptedAt: Instant)

    /** Retira el consentimiento. */
    suspend fun revokeConsent()

    /**
     * Identificador anónimo de esta instalación. Se genera la primera vez que se pide y no
     * cambia después. No sale de ningún dato de la persona.
     */
    suspend fun installId(): String
}
