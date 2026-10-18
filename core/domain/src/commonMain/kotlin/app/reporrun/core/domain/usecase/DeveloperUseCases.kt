package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.port.SampleGymSource
import app.reporrun.core.domain.repository.DeveloperSettingsRepository
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.model.AiBackend
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/*
 * Casos de uso de la sección de desarrollo de Ajustes. Solo se ofrecen en una build de
 * depuración; quién decide eso es la raíz de composición, no estos casos de uso.
 */

/** De dónde sale el plan, según lo guardado. */
class ObserveAiBackendUseCase(private val repository: DeveloperSettingsRepository) {
    operator fun invoke(): Flow<AiBackend> = repository.observeAiBackend()
}

/** Guarda de dónde sale el plan. Vale para la siguiente generación, sin reiniciar. */
class SetAiBackendUseCase(private val repository: DeveloperSettingsRepository) {
    suspend operator fun invoke(backend: AiBackend) = repository.setAiBackend(backend)
}

/**
 * Carga el gimnasio de ejemplo: lo guarda con su inventario, lo deja activo y **sustituye las
 * preferencias** por las del ejemplo. El id es fijo, así que cargarlo otra vez deja lo mismo.
 *
 * @return el gimnasio cargado.
 */
class LoadSampleGymUseCase(
    private val source: SampleGymSource,
    private val gyms: GymRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Gym {
        val sample = source.sampleGym(SAMPLE_GYM_ID)
        // Cargado otra vez, conserva la fecha de la primera: es el mismo gimnasio.
        val createdAt = gyms.observeGyms().first().firstOrNull { it.id == SAMPLE_GYM_ID }?.createdAt ?: clock.now()
        val gym = Gym(id = SAMPLE_GYM_ID, name = sample.name, isActive = false, notes = "", createdAt = createdAt)
        gyms.saveGym(gym)
        gyms.saveEquipment(sample.equipment)
        gyms.setActiveGym(gym.id)
        preferences.savePreferences(sample.preferences)
        return gym.copy(isActive = true)
    }

    companion object {
        /** El id del gimnasio de ejemplo: uno solo, por mucho que se cargue. */
        val SAMPLE_GYM_ID: GymId = GymId("gym_ejemplo")
    }
}
