package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.port.SampleGym
import app.reporrun.core.domain.port.SampleGymSource
import app.reporrun.core.domain.repository.DeveloperSettingsRepository
import app.reporrun.core.model.AiBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class DeveloperUseCasesTest {
    private val gyms = FakeGymRepository()
    private val preferences = FakePreferencesRepository(initial = preferences())
    private val sampleGymId = LoadSampleGymUseCase.SAMPLE_GYM_ID
    private val sampleSource = SampleGymSource { gymId ->
        SampleGym(
            name = "Gimnasio de ejemplo",
            equipment = listOf(equipment("eq_a", gymId = gymId), equipment("eq_b", gymId = gymId)),
            preferences = preferences().copy(sessionMinutes = 75),
        )
    }

    @Test
    fun storesTheChosenBackend() = runTest {
        val repository = InMemoryDeveloperSettings()

        SetAiBackendUseCase(repository)(AiBackend.PROXY)

        assertEquals(AiBackend.PROXY, ObserveAiBackendUseCase(repository)().first())
    }

    @Test
    fun loadsTheSampleGymAsTheActiveOneWithItsInventoryAndPreferences() = runTest {
        gyms.saveGym(gym(isActive = true))

        LoadSampleGymUseCase(sampleSource, gyms, preferences, FixedClock(NOW))()

        assertEquals(sampleGymId, gyms.observeActiveGym().first()?.id)
        assertEquals(listOf("eq_a", "eq_b"), gyms.observeEquipment(sampleGymId).first().map { it.id.value })
        assertEquals(75, preferences.saved?.sessionMinutes)
    }

    @Test
    fun loadingItTwiceLeavesASingleSampleGymAndKeepsItsCreationDate() = runTest {
        LoadSampleGymUseCase(sampleSource, gyms, preferences, FixedClock(NOW))()
        LoadSampleGymUseCase(sampleSource, gyms, preferences, FixedClock(NOW + 1.days))()

        val samples = gyms.savedGyms.filter { it.id == sampleGymId }
        assertEquals(1, samples.size)
        assertEquals(NOW, samples.single().createdAt)
        assertEquals(2, gyms.observeEquipment(sampleGymId).first().size)
    }
}

private class InMemoryDeveloperSettings : DeveloperSettingsRepository {
    private val backend = MutableStateFlow(AiBackend.FAKE)

    override fun observeAiBackend(): Flow<AiBackend> = backend

    override suspend fun setAiBackend(backend: AiBackend) {
        this.backend.value = backend
    }
}
