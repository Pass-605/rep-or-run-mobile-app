package app.reporrun.feature.plansetup

import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Fakes escritos a mano, en memoria. El test del ViewModel usa los casos de uso reales sobre
 * repositorios fake: así se prueba también que el ViewModel entiende sus resultados.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/**
 * Preferencias en memoria.
 *
 * @param answered `false` imita a una base de datos que aún no ha contestado.
 */
internal class FakePreferencesRepository(initial: BlockPreferences? = null, answered: Boolean = true) :
    PreferencesRepository {
    private val preferences = MutableStateFlow(initial)
    private val ready = MutableStateFlow(if (answered) Unit else null)

    val saved: BlockPreferences?
        get() = preferences.value

    /** La base de datos contesta con lo que tenga. */
    fun answer() {
        ready.value = Unit
    }

    override fun observePreferences(): Flow<BlockPreferences?> = ready.filterNotNull().map { preferences.value }

    override suspend fun savePreferences(preferences: BlockPreferences) {
        this.preferences.value = preferences
    }
}

/** Un gimnasio activo y su inventario, que es lo único que mira la pantalla. */
internal class FakeGymRepository(equipment: List<Equipment> = emptyList()) : GymRepository {
    private val gyms =
        MutableStateFlow(listOf(Gym(CENTRAL, "Gimnasio Central", isActive = true, notes = "", createdAt = NOW)))
    private val equipment = MutableStateFlow(equipment)

    override fun observeGyms(): Flow<List<Gym>> = gyms

    override fun observeActiveGym(): Flow<Gym?> = gyms.map { all -> all.firstOrNull { it.isActive } }

    override suspend fun saveGym(gym: Gym) {
        gyms.value = gyms.value.filterNot { it.id == gym.id } + gym
    }

    override suspend fun setActiveGym(gymId: GymId) {
        gyms.value = gyms.value.map { it.copy(isActive = it.id == gymId) }
    }

    override suspend fun deleteGym(gymId: GymId) {
        gyms.value = gyms.value.filterNot { it.id == gymId }
    }

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> =
        equipment.map { all -> all.filter { it.gymId == gymId }.sortedBy { it.name } }

    override suspend fun saveEquipment(equipment: List<Equipment>) {
        val replaced = equipment.map { it.id }.toSet()
        this.equipment.value = this.equipment.value.filterNot { it.id in replaced } + equipment
    }

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion {
        equipment.value = equipment.value.filterNot { it.id == equipmentId }
        return EquipmentDeletion.Deleted
    }
}

/** Jueves 8 de octubre de 2026. */
internal val NOW: Instant = Instant.parse("2026-10-08T10:00:00Z")
internal val TODAY = LocalDate(2026, 10, 8)

internal val CENTRAL = GymId("gym_central")

/** Un aparato del gimnasio activo. Sintético: los nombres son genéricos de cualquier gimnasio. */
internal fun equipment(id: String, name: String, category: EquipmentCategory = EquipmentCategory.CARDIO) = Equipment(
    id = EquipmentId(id),
    gymId = CENTRAL,
    catalogKey = null,
    name = name,
    category = category,
    loadType = if (category == EquipmentCategory.CARDIO) LoadType.CARDIO else LoadType.SELECTOR,
    minKg = null,
    maxKg = null,
    incrementKg = null,
    notes = null,
    isCustom = true,
)

/** Cinta y bicicleta, más una máquina que no es de cardio. */
internal fun inventoryWithCardio() = listOf(
    equipment("eq_treadmill", "Treadmill"),
    equipment("eq_bike", "Bike"),
    equipment("eq_pec_deck", "Pec Deck", EquipmentCategory.MACHINE),
)

internal fun savedPreferences(
    startDate: LocalDate? = null,
    cardioModes: List<String> = listOf("Treadmill"),
    sessionMinutes: Int = 75,
) = BlockPreferences(
    id = BlockPreferencesId("prefs_previas"),
    availableDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    sessionMinutes = sessionMinutes,
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    startDate = startDate,
    cardioModes = cardioModes,
    avoidExercises = "Sentadilla búlgara",
    unit = LoadUnit.KG,
)
