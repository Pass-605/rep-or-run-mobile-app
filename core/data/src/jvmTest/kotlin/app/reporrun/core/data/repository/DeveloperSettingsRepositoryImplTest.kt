package app.reporrun.core.data.repository

import app.reporrun.core.datastore.SETTINGS_FILE_NAME
import app.reporrun.core.datastore.SettingsDataSource
import app.reporrun.core.datastore.createSettingsDataStore
import app.reporrun.core.model.AiBackend
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

class DeveloperSettingsRepositoryImplTest {
    private val path = Files.createTempDirectory("reporrun-dev").resolve(SETTINGS_FILE_NAME).absolutePathString()
    private val job = Job()
    private val dataSource = SettingsDataSource(createSettingsDataStore(CoroutineScope(Dispatchers.IO + job)) { path })
    private val repository = DeveloperSettingsRepositoryImpl(dataSource)

    @AfterTest
    fun closeDataStore() = runTest { job.cancelAndJoin() }

    @Test
    fun usesTheExamplesUntilSomethingIsChosen() = runTest {
        assertEquals(AiBackend.FAKE, repository.observeAiBackend().first())
    }

    @Test
    fun storesTheChosenBackend() = runTest {
        repository.setAiBackend(AiBackend.PROXY)

        assertEquals(AiBackend.PROXY, repository.observeAiBackend().first())
    }

    @Test
    fun usesTheExamplesWhenTheStoredValueIsUnknown() = runTest {
        dataSource.setAiBackend("OPENAI_DIRECTO")

        assertEquals(AiBackend.FAKE, repository.observeAiBackend().first())
    }
}
