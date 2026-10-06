package app.reporrun.feature.gym

import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
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
 * Gimnasios e inventario en memoria, como los de `:core:data` en lo que un caso de uso nota.
 *
 * @param initial lo guardado al empezar; `null` imita a una base de datos que aún no ha contestado.
 */
internal class FakeGymRepository(initial: List<Gym>? = emptyList()) : GymRepository {
    private val gyms = MutableStateFlow(initial)
    private val equipment = MutableStateFlow<List<Equipment>>(emptyList())

    /** Cuántos usos en planes tiene cada aparato; con alguno, no se puede borrar. */
    val usages = mutableMapOf<EquipmentId, Int>()

    val savedEquipment: List<Equipment>
        get() = equipment.value

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

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> =
        equipment.map { all -> all.filter { it.gymId == gymId }.sortedBy { it.name } }

    override suspend fun saveEquipment(equipment: List<Equipment>) {
        val replaced = equipment.map { it.id }.toSet()
        this.equipment.value = this.equipment.value.filterNot { it.id in replaced } + equipment
    }

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion {
        val inUse = usages[equipmentId] ?: 0
        if (inUse > 0) return EquipmentDeletion.InUse(inUse)
        equipment.value = equipment.value.filterNot { it.id == equipmentId }
        return EquipmentDeletion.Deleted
    }
}

internal class FakeEquipmentCatalogRepository(items: List<EquipmentCatalogItem>) : EquipmentCatalogRepository {
    private val catalog = MutableStateFlow(items)

    override fun observeCatalog(): Flow<List<EquipmentCatalogItem>> = catalog

    override suspend fun loadSeed(): Int = 0
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

/** Una entrada del catálogo. Sintética: los nombres son genéricos de cualquier gimnasio. */
internal fun catalogItem(
    key: String,
    name: String,
    aliases: List<String> = emptyList(),
    category: EquipmentCategory = EquipmentCategory.MACHINE,
    loadType: LoadType = LoadType.SELECTOR,
) = EquipmentCatalogItem(
    key = CatalogKey(key),
    name = name,
    aliases = aliases,
    category = category,
    loadType = loadType,
    primaryMuscles = setOf(MuscleGroup.CHEST),
)

/** Cuatro entradas en tres categorías, fuera de orden a propósito. */
internal fun smallCatalog() = listOf(
    catalogItem("treadmill", "Treadmill", listOf("Caminadora", "Cinta"), EquipmentCategory.CARDIO, LoadType.CARDIO),
    catalogItem("pec_deck", "Pec Deck", listOf("Mariposa", "Contractor de pecho en máquina")),
    catalogItem("cable_tower", "Cable Tower", listOf("Torre de poleas"), EquipmentCategory.CABLE),
    catalogItem("chest_press", "Chest Press", listOf("Press de pecho")),
)

/** Un aparato del gimnasio [gymId]; con [catalogKey] viene del catálogo, sin ella es personalizado. */
internal fun equipment(
    id: String,
    name: String,
    catalogKey: String? = null,
    gymId: GymId = CENTRAL,
    category: EquipmentCategory = EquipmentCategory.MACHINE,
    maxKg: Double? = null,
) = Equipment(
    id = EquipmentId(id),
    gymId = gymId,
    catalogKey = catalogKey?.let(::CatalogKey),
    name = name,
    category = category,
    loadType = LoadType.SELECTOR,
    minKg = null,
    maxKg = maxKg,
    incrementKg = null,
    notes = null,
    isCustom = catalogKey == null,
)
