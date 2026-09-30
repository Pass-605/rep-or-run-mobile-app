package app.reporrun.core.data.repository

import app.reporrun.core.data.T0
import app.reporrun.core.datastore.SETTINGS_FILE_NAME
import app.reporrun.core.datastore.SettingsDataSource
import app.reporrun.core.datastore.createSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsRepositoryImplTest {
    private val path = Files.createTempDirectory("reporrun-settings").resolve(SETTINGS_FILE_NAME).absolutePathString()
    private val job = Job()
    private val dataSource = SettingsDataSource(createSettingsDataStore(CoroutineScope(Dispatchers.IO + job)) { path })

    @AfterTest
    fun closeDataStore() = runTest { job.cancelAndJoin() }

    @Test
    fun acceptsAndRevokesConsent() = runTest {
        val repository = SettingsRepositoryImpl(dataSource)

        repository.acceptConsent(T0)
        assertEquals(T0, repository.observeConsentAcceptedAt().first())

        repository.revokeConsent()
        assertNull(repository.observeConsentAcceptedAt().first())
    }

    @Test
    fun installIdIsAStableRandomUuid() = runTest {
        val repository = SettingsRepositoryImpl(dataSource)

        val first = repository.installId()

        assertTrue(UUID.matches(first), first)
        assertEquals(first, repository.installId())
    }

    @Test
    fun usesTheInjectedGeneratorOnlyTheFirstTime() = runTest {
        var calls = 0
        val repository = SettingsRepositoryImpl(dataSource) { "fixed-${++calls}" }

        repository.installId()
        repository.installId()

        assertEquals("fixed-1", repository.installId())
        assertEquals(1, calls)
    }

    private companion object {
        val UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    }
}
