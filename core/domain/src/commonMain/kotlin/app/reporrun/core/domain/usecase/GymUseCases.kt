package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** Resultado de [CreateGymUseCase]. */
sealed interface CreateGymResult {
    /** El gimnasio existe. [Gym.isActive] dice si además quedó como activo. */
    data class Created(val gym: Gym) : CreateGymResult

    /** El nombre no vale: [issue] es [FieldIssue.Required] o [FieldIssue.TooLong]. */
    data class InvalidName(val issue: FieldIssue) : CreateGymResult
}

/**
 * Crea un gimnasio con el inventario vacío. Si no hay ningún gimnasio activo, el nuevo lo es:
 * la pantalla no tiene que encadenar dos llamadas ni puede quedarse a medias.
 */
class CreateGymUseCase(private val repository: GymRepository, private val ids: IdGenerator, private val clock: Clock) {
    suspend operator fun invoke(name: String, notes: String = ""): CreateGymResult {
        val cleanName = name.trim()
        val issue = when {
            cleanName.isEmpty() -> FieldIssue.Required
            cleanName.length > Gym.MAX_NAME_LENGTH -> FieldIssue.TooLong(Gym.MAX_NAME_LENGTH)
            else -> null
        }
        return if (issue != null) CreateGymResult.InvalidName(issue) else create(cleanName, notes.trim())
    }

    private suspend fun create(name: String, notes: String): CreateGymResult {
        val becomesActive = repository.observeActiveGym().first() == null
        val gym = Gym(
            id = GymId("$GYM_ID_PREFIX${ids.newId()}"),
            name = name,
            isActive = false,
            notes = notes,
            createdAt = clock.now(),
        )
        repository.saveGym(gym)
        if (becomesActive) repository.setActiveGym(gym.id)
        return CreateGymResult.Created(gym.copy(isActive = becomesActive))
    }

    private companion object {
        const val GYM_ID_PREFIX = "gym_"
    }
}

/** Resultado de [SetActiveGymUseCase]. */
sealed interface SetActiveGymResult {
    /** El gimnasio pedido es ahora el único activo. */
    data object Activated : SetActiveGymResult

    /** Ese gimnasio no existe; el activo no cambió. */
    data object GymNotFound : SetActiveGymResult
}

/**
 * Cambia el gimnasio activo. Comprueba antes que existe: [GymRepository.setActiveGym] desactiva
 * todos y marca uno, y con un id que ya no está dejaría la app sin gimnasio activo.
 */
class SetActiveGymUseCase(private val repository: GymRepository) {
    suspend operator fun invoke(gymId: GymId): SetActiveGymResult = if (repository.hasGym(gymId)) {
        repository.setActiveGym(gymId)
        SetActiveGymResult.Activated
    } else {
        SetActiveGymResult.GymNotFound
    }
}

internal suspend fun GymRepository.hasGym(gymId: GymId): Boolean = observeGyms().first().any { it.id == gymId }
