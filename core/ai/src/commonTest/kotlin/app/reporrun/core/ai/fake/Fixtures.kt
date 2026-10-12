package app.reporrun.core.ai.fake

import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlin.time.Clock
import kotlin.time.Instant

// Datos sintéticos: ninguna persona real. El gimnasio es el del ejemplo del contrato.

internal val NOW: Instant = Instant.parse("2026-10-05T12:20:40Z")

internal val GYM_ID = GymId("gym_central")

internal class FixedClock(private val instant: Instant = NOW) : Clock {
    override fun now(): Instant = instant
}

/** Identificadores predecibles y distintos: `id1`, `id2`… */
internal class SequentialIds : IdGenerator {
    private var last = 0

    override fun newId(): String = "id${++last}"
}

internal fun gym() = Gym(id = GYM_ID, name = "Gimnasio Central", isActive = true, notes = "", createdAt = NOW)

internal fun profile() = UserProfile(
    id = ProfileId("profile"),
    alias = "Sol",
    age = 30,
    heightCm = 170.0,
    weightKg = 72.5,
    waistCm = null,
    hipCm = null,
    experience = Experience.INTERMEDIATE,
    goal = Goal.RECOMPOSITION,
    priorityMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.CHEST),
    limitations = "",
    preferences = "",
    updatedAt = NOW,
)

/** Por defecto, los días y el cardio del ejemplo del contrato. */
internal fun preferences(
    availableDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY),
    cardioModes: List<String> = listOf("Caminadora"),
) = BlockPreferences(
    id = BlockPreferencesId("preferences"),
    availableDays = availableDays,
    sessionMinutes = 75,
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    startDate = null,
    cardioModes = cardioModes,
    avoidExercises = "",
    unit = LoadUnit.KG,
)

internal fun equipment(
    id: String,
    catalogKey: String? = null,
    name: String = "Aparato $id",
    category: EquipmentCategory = EquipmentCategory.MACHINE,
    loadType: LoadType = LoadType.SELECTOR,
) = Equipment(
    id = EquipmentId(id),
    gymId = GYM_ID,
    catalogKey = catalogKey?.let(::CatalogKey),
    name = name,
    category = category,
    loadType = loadType,
    minKg = null,
    maxKg = null,
    incrementKg = null,
    notes = null,
    isCustom = catalogKey == null,
)

/** El inventario del gimnasio de ejemplo del contrato, con sus mismos identificadores. */
internal fun exampleInventory(): List<Equipment> = FakeExamples.generateRequest.gym.equipment.map {
    equipment(
        id = it.id,
        name = it.name,
        category = EquipmentCategory.valueOf(it.category.name),
        loadType = LoadType.valueOf(it.loadType.name),
    )
}

/**
 * Las mismas máquinas del ejemplo como las guarda la app de verdad: marcadas desde el catálogo,
 * con identificadores que no se parecen a los del ejemplo.
 */
internal fun catalogInventory(): List<Equipment> = listOf(
    equipment("eq_a1", "chest_press", "Press de pecho"),
    equipment("eq_a2", "chest_supported_row", "Remo con apoyo", loadType = LoadType.PLATES_TOTAL),
    equipment("eq_a3", "lat_pulldown", "Jalón al pecho"),
    equipment("eq_a4", "shoulder_press_machine", "Press de hombro"),
    equipment("eq_a5", "pendulum_squat", "Sentadilla péndulo", loadType = LoadType.SLED),
    equipment("eq_a6", "lying_leg_curl", "Curl femoral tumbado"),
    equipment("eq_a7", "leg_extension", "Extensión de cuádriceps"),
    equipment("eq_a8", "functional_trainer", "Polea doble", EquipmentCategory.CABLE),
    equipment("eq_a9", "dumbbells", "Mancuernas", EquipmentCategory.FREE_WEIGHT, LoadType.DUMBBELL),
    equipment("eq_a10", "exercise_mat", "Colchoneta", EquipmentCategory.BODYWEIGHT, LoadType.BODYWEIGHT),
    equipment("eq_a11", "treadmill", "Caminadora", EquipmentCategory.CARDIO, LoadType.CARDIO),
)

internal fun generationRequest(
    equipment: List<Equipment> = exampleInventory(),
    preferences: BlockPreferences = preferences(),
) = PlanGenerationRequest(profile = profile(), gym = gym(), equipment = equipment, preferences = preferences)

/** La versión guardada de un plan generado, como la armará quien lo guarde. */
internal fun GeneratedPlan.toPlan() = Plan(
    id = PlanId("ROR-2026-10-05-v1"),
    gymId = GYM_ID,
    createdAt = generatedAt,
    status = PlanStatus.ACTIVE,
    blockDays = blockDays,
    startDate = null,
    reviewDate = null,
    focus = focus,
    notes = notes,
    days = days,
    inputSnapshotJson = inputSnapshotJson,
    model = model,
    promptVersion = promptVersion,
    schemaVersion = schemaVersion,
)
