package app.reporrun.core.datastore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.absolutePathString
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SettingsDataSourceTest {
    private val path = Files.createTempDirectory("reporrun-settings").resolve(SETTINGS_FILE_NAME).absolutePathString()
    private val job = Job()
    private val settings = SettingsDataSource(createSettingsDataStore(CoroutineScope(Dispatchers.IO + job)) { path })

    @AfterTest
    fun closeDataStore() = runTest { job.cancelAndJoin() }

    @Test
    fun consentIsNullUntilAccepted() = runTest {
        assertNull(settings.consentAcceptedAt.first())
    }

    @Test
    fun storesAndRevokesConsent() = runTest {
        val acceptedAt = Instant.fromEpochMilliseconds(1_790_000_000_000)

        settings.setConsentAcceptedAt(acceptedAt)
        assertEquals(acceptedAt, settings.consentAcceptedAt.first())

        settings.setConsentAcceptedAt(null)
        assertNull(settings.consentAcceptedAt.first())
    }

    @Test
    fun generatesInstallIdOnlyOnce() = runTest {
        val generated = AtomicInteger()
        val generate = { "install-${generated.incrementAndGet()}" }

        val first = settings.installId(generate)
        val second = settings.installId(generate)

        assertEquals("install-1", first)
        assertEquals(first, second)
        assertEquals(1, generated.get())
    }

    @Test
    fun concurrentCallsShareTheSameInstallId() = runTest {
        val generated = AtomicInteger()

        val ids = List(8) {
            async(Dispatchers.IO) { settings.installId { "install-${generated.incrementAndGet()}" } }
        }.awaitAll()

        assertEquals(setOf("install-1"), ids.toSet())
    }

    @Test
    fun installIdSurvivesReopeningTheFile() = runTest {
        val first = settings.installId { "install-a" }
        job.cancelAndJoin()

        // Como tras reiniciar la app: mismo archivo, DataStore nuevo.
        val reopenedJob = Job()
        val reopened =
            SettingsDataSource(createSettingsDataStore(CoroutineScope(Dispatchers.IO + reopenedJob)) { path })
        val second = reopened.installId { "install-b" }
        reopenedJob.cancelAndJoin()

        assertEquals(first, second)
    }
}
