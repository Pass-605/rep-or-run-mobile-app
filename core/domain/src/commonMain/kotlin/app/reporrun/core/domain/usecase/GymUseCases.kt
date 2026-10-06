package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.Flow
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
        val issue = gymNameIssue(cleanName)
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

/** Resultado de [RenameGymUseCase]. */
sealed interface RenameGymResult {
    /** El gimnasio tiene ya el nombre nuevo. Lo demás, incluido si está activo, no cambió. */
    data class Renamed(val gym: Gym) : RenameGymResult

    /** El nombre no vale: [issue] es [FieldIssue.Required] o [FieldIssue.TooLong]. */
    data class InvalidName(val issue: FieldIssue) : RenameGymResult

    /** Ese gimnasio no existe; no se guardó nada. */
    data object GymNotFound : RenameGymResult
}

/**
 * Cambia el nombre de un gimnasio, con las mismas reglas que [CreateGymUseCase]. Lee el gimnasio
 * guardado y solo le cambia el nombre: el inventario y los planes cuelgan de su id, no del nombre.
 */
class RenameGymUseCase(private val repository: GymRepository) {
    suspend operator fun invoke(gymId: GymId, name: String): RenameGymResult {
        val cleanName = name.trim()
        val issue = gymNameIssue(cleanName)
        val gym = repository.observeGyms().first().firstOrNull { it.id == gymId }
        return when {
            issue != null -> RenameGymResult.InvalidName(issue)

            gym == null -> RenameGymResult.GymNotFound

            else -> {
                val renamed = gym.copy(name = cleanName)
                repository.saveGym(renamed)
                RenameGymResult.Renamed(renamed)
            }
        }
    }
}

/** Todos los gimnasios, del más antiguo al más reciente; el activo lleva [Gym.isActive]. */
class ObserveGymsUseCase(private val repository: GymRepository) {
    operator fun invoke(): Flow<List<Gym>> = repository.observeGyms()
}

/** Por qué [cleanName], ya sin espacios alrededor, no vale como nombre de gimnasio; `null` si vale. */
private fun gymNameIssue(cleanName: String): FieldIssue? = when {
    cleanName.isEmpty() -> FieldIssue.Required
    cleanName.length > Gym.MAX_NAME_LENGTH -> FieldIssue.TooLong(Gym.MAX_NAME_LENGTH)
    else -> null
}

internal suspend fun GymRepository.hasGym(gymId: GymId): Boolean = observeGyms().first().any { it.id == gymId }
