package app.reporrun.core.domain.repository

import app.reporrun.core.model.AiBackend
import kotlinx.coroutines.flow.Flow

/**
 * Ajustes que solo existen en una build de depuración. Están aparte de [SettingsRepository]
 * para que nada de la app normal dependa de ellos.
 */
interface DeveloperSettingsRepository {
    /** De dónde sale el plan. Sin elección guardada, [AiBackend.FAKE]. */
    fun observeAiBackend(): Flow<AiBackend>

    /** Guarda de dónde sale el plan. */
    suspend fun setAiBackend(backend: AiBackend)
}
