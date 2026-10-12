package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.MetricRange
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.RirRange
import app.reporrun.core.model.TrialLoad
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

internal fun planExercise(code: String, order: Int, equipmentId: String = "eq_1") = PlanExercise(
    id = PlanExerciseId("exercise_$code"),
    code = ExerciseCode(code),
    order = order,
    name = "Ejercicio $code",
    equipmentId = EquipmentId(equipmentId),
    sets = 3,
    dose = ExerciseDose.Reps(MetricRange(low = 8, high = 12), RirRange(low = 2, high = 2)),
    rest = "2 min",
    trial = TrialLoad("Carga de ensayo"),
    target = "Pecho",
    unilateral = false,
    alternatives = emptyList(),
)

/** Un lunes: el día disponible de [preferences]. */
internal fun planDay(vararg exercises: PlanExercise) = PlanDay(
    id = PlanDayId("day_monday"),
    dayOfWeek = DayOfWeek.MONDAY,
    dayIndex = 0,
    session = "Sesión",
    priority = "Prioridad",
    cardio = null,
    warmup = "Calentamiento",
    mobility = "Movilidad",
    cooldown = "Vuelta a la calma",
    exercises = exercises.toList(),
)

/** Lo que devuelve un generador: por defecto, un plan válido para [preferences] e [inventory]. */
internal fun generatedPlan(
    days: List<PlanDay> = listOf(planDay(planExercise("L01", 1), planExercise("L02", 2, "eq_2"))),
) = GeneratedPlan(
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    focus = listOf("Fuerza básica"),
    notes = listOf("Las cargas son de ensayo"),
    days = days,
    model = "modelo-de-prueba",
    promptVersion = "generate-v1",
    schemaVersion = "1.0",
    generatedAt = NOW,
    inputSnapshotJson = """{"schemaVersion":"1.0"}""",
)

/** Un borrador ya guardado, para los tests que parten de un historial. */
internal fun draft(id: String, gymId: GymId = GYM_ID) = Plan(
    id = PlanId(id),
    gymId = gymId,
    createdAt = NOW,
    status = PlanStatus.DRAFT,
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    startDate = null,
    reviewDate = null,
    focus = emptyList(),
    notes = emptyList(),
    days = emptyList(),
    inputSnapshotJson = "{}",
    model = "modelo-de-prueba",
    promptVersion = "generate-v1",
    schemaVersion = "1.0",
)
