package app.reporrun.feature.gym

import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Fakes escritos a mano, en memoria. El test del ViewModel usa los casos de uso reales sobre
 * un repositorio fake: así se prueba también que el ViewModel entiende sus resultados.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/** Devuelve `id1`, `id2`... */
internal class SequentialIds : IdGenerator {
    private var next = 1

    override fun newId(): String = "id${next++}"
}

/**
 * Solo la parte de gimnasios: el inventario es de C-08 y aquí no se toca.
 *
 * @param initial lo guardado al empezar; `null` imita a una base de datos que aún no ha contestado.
 */
internal class FakeGymRepository(initial: List<Gym>? = emptyList()) : GymRepository {
    private val gyms = MutableStateFlow(initial)

    /** Cuántas veces se ha escrito un gimnasio. */
    var saveCalls = 0
        private set

    val saved: List<Gym>
        get() = gyms.value.orEmpty()

    /** La base de datos contesta con lo que tenga. */
    fun answer() {
        gyms.value = saved
    }

    override fun observeGyms(): Flow<List<Gym>> = gyms.filterNotNull().map { all -> all.sortedBy { it.createdAt } }

    override fun observeActiveGym(): Flow<Gym?> = observeGyms().map { all -> all.firstOrNull { it.isActive } }

    override suspend fun saveGym(gym: Gym) {
        saveCalls++
        gyms.value = saved.filterNot { it.id == gym.id } + gym
    }

    /** Como `GymDao.setActive`: desactiva todos y marca uno, exista o no. */
    override suspend fun setActiveGym(gymId: GymId) {
        gyms.value = saved.map { it.copy(isActive = it.id == gymId) }
    }

    override suspend fun deleteGym(gymId: GymId) {
        gyms.value = saved.filterNot { it.id == gymId }
    }

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> = flowOf(emptyList())

    override suspend fun saveEquipment(equipment: List<Equipment>) = Unit

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion = EquipmentDeletion.Deleted
}

internal val NOW: Instant = Instant.parse("2026-10-05T10:00:00Z")

internal val CENTRAL = GymId("gym_central")
internal val NORTE = GymId("gym_norte")

/** Gimnasio sintético: ningún sitio real. */
internal fun gym(id: GymId, name: String, isActive: Boolean = false, createdAt: Instant = NOW) =
    Gym(id = id, name = name, isActive = isActive, notes = "", createdAt = createdAt)

/** Dos gimnasios guardados, con el primero activo. */
internal fun twoGyms() = listOf(
    gym(CENTRAL, "Gimnasio Central", isActive = true, createdAt = Instant.parse("2026-09-21T14:03:11Z")),
    gym(NORTE, "Gimnasio Norte", createdAt = Instant.parse("2026-09-28T09:30:00Z")),
)
