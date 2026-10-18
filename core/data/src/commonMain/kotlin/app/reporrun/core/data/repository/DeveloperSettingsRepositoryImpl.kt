package app.reporrun.core.data.repository

import app.reporrun.core.datastore.SettingsDataSource
import app.reporrun.core.domain.repository.DeveloperSettingsRepository
import app.reporrun.core.model.AiBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ajustes de depuración sobre el mismo DataStore que [SettingsRepositoryImpl]. */
class DeveloperSettingsRepositoryImpl(private val dataSource: SettingsDataSource) : DeveloperSettingsRepository {
    // Sin valor, o con uno que esta versión no conoce, los ejemplos sin red.
    override fun observeAiBackend(): Flow<AiBackend> = dataSource.aiBackend.map { stored ->
        AiBackend.entries.firstOrNull { it.name == stored } ?: AiBackend.FAKE
    }

    override suspend fun setAiBackend(backend: AiBackend) = dataSource.setAiBackend(backend.name)
}
