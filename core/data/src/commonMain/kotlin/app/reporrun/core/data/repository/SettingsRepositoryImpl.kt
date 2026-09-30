package app.reporrun.core.data.repository

import app.reporrun.core.datastore.SettingsDataSource
import app.reporrun.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Ajustes sobre DataStore.
 *
 * @param newInstallId genera el identificador de instalación la primera vez; por defecto un
 *   UUID v4 aleatorio. Se inyecta para poder fijarlo en los tests.
 */
class SettingsRepositoryImpl(
    private val dataSource: SettingsDataSource,
    private val newInstallId: () -> String = { Uuid.random().toString() },
) : SettingsRepository {
    override fun observeConsentAcceptedAt(): Flow<Instant?> = dataSource.consentAcceptedAt

    override suspend fun acceptConsent(acceptedAt: Instant) = dataSource.setConsentAcceptedAt(acceptedAt)

    override suspend fun revokeConsent() = dataSource.setConsentAcceptedAt(null)

    override suspend fun installId(): String = dataSource.installId(newInstallId)
}
