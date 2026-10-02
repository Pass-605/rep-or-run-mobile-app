package app.reporrun.core.domain.usecase

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlin.time.Instant

// Datos sintéticos: ninguna persona real.

internal val NOW: Instant = Instant.parse("2026-09-21T14:03:11Z")

internal val GYM_ID = GymId("gym_central")

internal fun profileInput() = ProfileInput(
    alias = "Sam",
    age = 30,
    heightCm = 170.0,
    weightKg = 70.0,
    waistCm = null,
    hipCm = null,
    experience = Experience.INTERMEDIATE,
    goal = Goal.HYPERTROPHY,
    priorityMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.BACK),
    limitations = "",
    preferences = "",
)

internal fun profile(id: String = "profile") = UserProfile(
    id = ProfileId(id),
    alias = null,
    age = 30,
    heightCm = 170.0,
    weightKg = null,
    waistCm = null,
    hipCm = null,
    experience = Experience.BEGINNER,
    goal = Goal.GENERAL_FITNESS,
    priorityMuscles = emptySet(),
    limitations = "",
    preferences = "",
    updatedAt = NOW,
)

internal fun preferencesInput() = PreferencesInput(
    availableDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    sessionMinutes = 75,
    startDate = null,
    cardioModes = listOf("Caminadora"),
    avoidExercises = "",
)

internal fun preferences(id: String = "preferences") = BlockPreferences(
    id = BlockPreferencesId(id),
    availableDays = setOf(DayOfWeek.MONDAY),
    sessionMinutes = 60,
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    startDate = null,
    cardioModes = emptyList(),
    avoidExercises = "",
    unit = LoadUnit.KG,
)

internal fun gym(id: GymId = GYM_ID, isActive: Boolean = false, createdAt: Instant = NOW) =
    Gym(id = id, name = "Gimnasio ${id.value}", isActive = isActive, notes = "", createdAt = createdAt)

internal fun equipment(id: String, gymId: GymId = GYM_ID, name: String = "Aparato $id", catalogKey: String? = null) =
    Equipment(
        id = EquipmentId(id),
        gymId = gymId,
        catalogKey = catalogKey?.let(::CatalogKey),
        name = name,
        category = EquipmentCategory.MACHINE,
        loadType = LoadType.SELECTOR,
        minKg = null,
        maxKg = null,
        incrementKg = null,
        notes = null,
        isCustom = catalogKey == null,
    )

/** [count] aparatos distintos del gimnasio [gymId]. */
internal fun inventory(count: Int, gymId: GymId = GYM_ID): List<Equipment> =
    (1..count).map { equipment("eq_$it", gymId) }

internal fun equipmentInput(name: String = "Polea alta") =
    EquipmentInput(name = name, category = EquipmentCategory.CABLE, loadType = LoadType.SELECTOR)

internal fun catalogItem(key: String = "chest_press", name: String = "Chest Press") = EquipmentCatalogItem(
    key = CatalogKey(key),
    name = name,
    aliases = listOf("Press de pecho"),
    category = EquipmentCategory.MACHINE,
    loadType = LoadType.SELECTOR,
    primaryMuscles = setOf(MuscleGroup.CHEST),
)
