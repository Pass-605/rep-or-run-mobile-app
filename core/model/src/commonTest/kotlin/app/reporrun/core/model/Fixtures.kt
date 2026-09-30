package app.reporrun.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

// Datos sintéticos, calcados de docs/contract/examples: ninguna persona real.

internal val NOW: Instant = Instant.parse("2026-09-21T14:03:11Z")

internal fun exercise(
    code: String = "L01",
    sets: Int = 3,
    unilateral: Boolean = false,
    dose: ExerciseDose = ExerciseDose.Reps(MetricRange(6, 10), RirRange(2, 2)),
    alternatives: List<Alternative> = emptyList(),
) = PlanExercise(
    id = PlanExerciseId("ex_$code"),
    code = ExerciseCode(code),
    order = 1,
    name = "Chest Press convergente",
    equipmentId = EquipmentId("eq_chest_press"),
    sets = sets,
    dose = dose,
    rest = "2,5-3 min",
    trial = TrialLoad("20-35 kg en el selector"),
    target = "Pecho",
    unilateral = unilateral,
    alternatives = alternatives,
)

internal fun day(exercises: List<PlanExercise>, cardio: CardioPrescription? = null) = PlanDay(
    id = PlanDayId("day_mon"),
    dayOfWeek = DayOfWeek.MONDAY,
    dayIndex = 0,
    session = "Upper A + core",
    priority = "Pecho + espalda",
    cardio = cardio,
    warmup = "Caminadora 5-7 min y 2-3 aproximaciones",
    mobility = "Círculos de brazos",
    cooldown = "Estiramientos suaves",
    exercises = exercises,
)

internal fun plan(id: String = "ROR-2026-09-21-v1", days: List<PlanDay> = listOf(day(listOf(exercise())))) = Plan(
    id = PlanId(id),
    gymId = GymId("gym_central"),
    createdAt = NOW,
    status = PlanStatus.DRAFT,
    blockDays = 14,
    startDate = LocalDate(2026, 9, 21),
    reviewDate = LocalDate(2026, 10, 5),
    focus = listOf("Pecho"),
    notes = listOf("Las cargas son de ensayo."),
    days = days,
    inputSnapshotJson = "{}",
    model = "gpt-modelo-ejemplo",
    promptVersion = "plan-gen-v1",
    schemaVersion = "1",
)

internal fun proposed(
    type: ProposalType = ProposalType.INCREASE_LOAD,
    code: String? = "L01",
    day: DayOfWeek? = null,
    replacement: String? = null,
) = ProposedChange(
    type = type,
    targetExerciseCode = code?.let(::ExerciseCode),
    targetDayOfWeek = day,
    replacementEquipmentId = replacement?.let(::EquipmentId),
    description = "Subir al siguiente número del selector",
    rationale = "Tres sesiones en el tope del rango con el RIR previsto",
    confidence = Confidence.HIGH,
)
