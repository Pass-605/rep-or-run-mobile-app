package app.reporrun.core.data

import app.reporrun.core.model.Alternative
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.CardioPrescription
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Equipment
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
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/*
 * Datos sintéticos para los tests de :core:data. Nada de aquí sale de datos reales.
 */

val T0: Instant = Instant.fromEpochMilliseconds(1_790_000_000_000)

val GYM_ID = GymId("gym_1")

fun gym(id: GymId = GYM_ID, isActive: Boolean = false, createdAt: Instant = T0) =
    Gym(id = id, name = "Gimnasio ${id.value}", isActive = isActive, notes = "", createdAt = createdAt)

fun equipment(id: String, gymId: GymId = GYM_ID, loadType: LoadType = LoadType.SELECTOR) = Equipment(
    id = EquipmentId(id),
    gymId = gymId,
    catalogKey = CatalogKey("chest_press"),
    name = "Aparato $id",
    category = EquipmentCategory.MACHINE,
    loadType = loadType,
    minKg = 5.0,
    maxKg = 100.0,
    incrementKg = 5.0,
    notes = null,
    isCustom = false,
)

/**
 * Plan de dos días: el lunes con cardio, un ejercicio por repeticiones con RIR y dos
 * alternativas, y otro sin RIR; el jueves sin cardio, con un carry en metros y una plancha en
 * segundos.
 */
fun plan(id: String = "ROR-2026-09-30-v1", gymId: GymId = GYM_ID, status: PlanStatus = PlanStatus.DRAFT): Plan {
    val exercises = exercises(id)
    val monday = PlanDay(
        id = PlanDayId("$id-d0"),
        dayOfWeek = DayOfWeek.MONDAY,
        dayIndex = 0,
        session = "Empuje",
        priority = "Pecho",
        cardio = CardioPrescription(10, 15, "Caminadora", "5-6", "5 km/h", null),
        warmup = "5 min suave",
        mobility = "Hombros",
        cooldown = "Estiramientos",
        exercises = exercises.take(2),
        status = DayStatus.APPROVED,
    )
    val thursday = monday.copy(
        id = PlanDayId("$id-d1"),
        dayOfWeek = DayOfWeek.THURSDAY,
        dayIndex = 1,
        cardio = null,
        exercises = exercises.drop(2),
        status = DayStatus.PROPOSED,
    )
    return Plan(
        id = PlanId(id),
        gymId = gymId,
        createdAt = T0,
        status = status,
        blockDays = BlockPreferences.BLOCK_DAYS_V1,
        startDate = LocalDate(2026, 10, 5),
        reviewDate = LocalDate(2026, 10, 19),
        focus = listOf("Fuerza básica"),
        notes = listOf("Cargas de ensayo: ajustar con el RIR"),
        days = listOf(monday, thursday),
        inputSnapshotJson = """{"schemaVersion":"1"}""",
        model = "fake-model",
        promptVersion = "plan-gen-v1",
        schemaVersion = "1",
    )
}

/**
 * Lunes: press con RIR y dos alternativas, y remo sin RIR. Jueves: carry en metros y plancha en
 * segundos.
 */
private fun exercises(id: String): List<PlanExercise> {
    val press = PlanExercise(
        id = PlanExerciseId("$id-e1"),
        code = ExerciseCode("L01"),
        order = 1,
        name = "Press de pecho",
        equipmentId = EquipmentId("eq_press"),
        sets = 3,
        dose = ExerciseDose.Reps(MetricRange(8, 12), RirRange(1, 2)),
        rest = "2-3 min",
        trial = TrialLoad("20-30 kg en el selector"),
        target = "Pecho",
        unilateral = false,
        alternatives = listOf(
            Alternative("Press con mancuernas", EquipmentId("eq_db"), "Si la máquina está ocupada"),
            Alternative("Flexiones", EquipmentId("eq_floor"), null),
        ),
    )
    val row = press.copy(
        id = PlanExerciseId("$id-e2"),
        code = ExerciseCode("L02"),
        order = 2,
        name = "Remo con pecho apoyado",
        equipmentId = EquipmentId("eq_row"),
        dose = ExerciseDose.Reps(MetricRange(10, 12), rir = null),
        alternatives = emptyList(),
    )
    val carry = press.copy(
        id = PlanExerciseId("$id-e3"),
        code = ExerciseCode("J01"),
        name = "Paseo del granjero",
        equipmentId = EquipmentId("eq_db"),
        dose = ExerciseDose.Meters(MetricRange(20, 30)),
        trial = TrialLoad("10 kg por mancuerna"),
        unilateral = true,
        alternatives = emptyList(),
    )
    val plank = carry.copy(
        id = PlanExerciseId("$id-e4"),
        code = ExerciseCode("J02"),
        order = 2,
        name = "Plancha",
        equipmentId = EquipmentId("eq_floor"),
        dose = ExerciseDose.Seconds(MetricRange(30, 45)),
        trial = TrialLoad("Peso corporal, 0 kg"),
        unilateral = false,
    )
    return listOf(press, row, carry, plank)
}

/** Los aparatos que usa [plan], para que las claves foráneas se cumplan. */
val PLAN_EQUIPMENT: List<String> = listOf("eq_press", "eq_row", "eq_db", "eq_floor")

fun profile() = UserProfile(
    id = ProfileId("profile_1"),
    alias = null,
    age = 35,
    heightCm = 175.0,
    weightKg = null,
    waistCm = null,
    hipCm = null,
    experience = Experience.INTERMEDIATE,
    goal = Goal.HYPERTROPHY,
    priorityMuscles = setOf(MuscleGroup.BACK, MuscleGroup.GLUTES),
    limitations = "",
    preferences = "",
    updatedAt = T0,
)

fun preferences() = BlockPreferences(
    id = BlockPreferencesId("prefs_1"),
    availableDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    sessionMinutes = 60,
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    startDate = null,
    cardioModes = listOf("Caminadora", "Bicicleta"),
    avoidExercises = "",
    unit = LoadUnit.KG,
)
