package app.reporrun.core.database

import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.reporrun.core.database.entity.CardioColumns
import app.reporrun.core.database.entity.ChangeProposalEntity
import app.reporrun.core.database.entity.CheckInEntity
import app.reporrun.core.database.entity.DoseColumns
import app.reporrun.core.database.entity.EquipmentEntity
import app.reporrun.core.database.entity.ExerciseFeedbackEntity
import app.reporrun.core.database.entity.ExportRecordEntity
import app.reporrun.core.database.entity.GymEntity
import app.reporrun.core.database.entity.PlanChangeEntity
import app.reporrun.core.database.entity.PlanDayEntity
import app.reporrun.core.database.entity.PlanEntity
import app.reporrun.core.database.entity.PlanExerciseAlternativeEntity
import app.reporrun.core.database.entity.PlanExerciseEntity
import app.reporrun.core.database.entity.ReviewEntity
import app.reporrun.core.database.entity.ReviewScheduleEntity
import app.reporrun.core.model.Confidence
import app.reporrun.core.model.DataQuality
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Decision
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.FeedbackRating
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.Metric
import app.reporrun.core.model.PlanStatus
import app.reporrun.core.model.ProposalType
import app.reporrun.core.model.ReviewFrequency
import app.reporrun.core.model.ReviewMetric
import app.reporrun.core.model.ReviewStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/*
 * Base en memoria y datos sintéticos para los tests de DAO. Nada de aquí sale de datos reales.
 */

fun inMemoryDatabase(): RepOrRunDatabase = Room.inMemoryDatabaseBuilder<RepOrRunDatabase>()
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

/** Filas de [table], contadas con SQL directo para ver lo que hay de verdad en disco. */
suspend fun RepOrRunDatabase.count(table: String): Long = useReaderConnection { connection ->
    connection.usePrepared("SELECT COUNT(*) FROM $table") { statement ->
        statement.step()
        statement.getLong(0)
    }
}

val T0: Instant = Instant.fromEpochMilliseconds(1_790_000_000_000)

fun gym(id: String = "gym_1", isActive: Boolean = false, createdAt: Instant = T0) =
    GymEntity(id = id, name = "Gimnasio $id", isActive = isActive, notes = "", createdAt = createdAt)

fun equipment(id: String, gymId: String = "gym_1", loadType: LoadType = LoadType.SELECTOR) = EquipmentEntity(
    id = id,
    gymId = gymId,
    catalogKey = null,
    name = "Aparato $id",
    category = EquipmentCategory.MACHINE,
    loadType = loadType,
    minKg = null,
    maxKg = null,
    incrementKg = 5.0,
    notes = null,
    isCustom = true,
)

/** Las filas de un plan listas para [app.reporrun.core.database.dao.PlanDao.insertPlan]. */
data class PlanRows(
    val plan: PlanEntity,
    val days: List<PlanDayEntity>,
    val exercises: List<PlanExerciseEntity>,
    val alternatives: List<PlanExerciseAlternativeEntity>,
)

/**
 * Plan sintético de dos días: el lunes con cardio y un ejercicio por repeticiones con RIR y una
 * alternativa; el jueves sin cardio y un carry en metros, sin RIR.
 */
fun planRows(
    id: String = "ROR-2026-09-30-v1",
    gymId: String = "gym_1",
    status: PlanStatus = PlanStatus.DRAFT,
    createdAt: Instant = T0,
    mainEquipment: String = "eq_press",
    altEquipment: String = "eq_db",
): PlanRows {
    val plan = PlanEntity(
        id = id,
        gymId = gymId,
        createdAt = createdAt,
        status = status,
        blockDays = 14,
        startDate = LocalDate(2026, 10, 5),
        reviewDate = LocalDate(2026, 10, 19),
        focus = listOf("Fuerza básica", "Técnica, \"sin prisa\""),
        notes = listOf("Cargas de ensayo: ajustar con el RIR"),
        inputSnapshotJson = """{"schemaVersion":"1"}""",
        model = "fake-model",
        promptVersion = "plan-gen-v1",
        schemaVersion = "1",
    )
    val monday = planDay(planId = id)
    val thursday = monday.copy(id = "$id-d1", dayOfWeek = DayOfWeek.THURSDAY, dayIndex = 1, cardio = null)
    val press = PlanExerciseEntity(
        id = "$id-e1",
        planDayId = monday.id,
        code = "L01",
        order = 1,
        name = "Press de pecho",
        equipmentId = mainEquipment,
        sets = 3,
        dose = DoseColumns(Metric.REPS, low = 8, high = 12, rirLow = 1, rirHigh = 2),
        rest = "2-3 min",
        trial = "20-30 kg en el selector",
        target = "Pecho",
        unilateral = false,
    )
    val carry = press.copy(
        id = "$id-e2",
        planDayId = thursday.id,
        code = "J01",
        name = "Paseo del granjero",
        equipmentId = altEquipment,
        dose = DoseColumns(Metric.METERS, low = 20, high = 30, rirLow = null, rirHigh = null),
        trial = "10 kg por mancuerna",
        unilateral = true,
    )
    val alternative = PlanExerciseAlternativeEntity(
        planExerciseId = press.id,
        position = 0,
        name = "Press con mancuernas",
        equipmentId = altEquipment,
        note = "Si la máquina está ocupada",
    )
    return PlanRows(plan, listOf(monday, thursday), listOf(press, carry), listOf(alternative))
}

fun planChange(id: String = "ch_1", planId: String, previousPlanId: String?, reviewId: String? = null) =
    PlanChangeEntity(
        id = id,
        planId = planId,
        previousPlanId = previousPlanId,
        reviewId = reviewId,
        date = LocalDate(2026, 10, 14),
        summary = "Resumen",
        reason = "Motivo",
        approvedAt = T0,
    )

fun schedule(planId: String) = ReviewScheduleEntity(
    id = "sched_1",
    planId = planId,
    frequency = ReviewFrequency.CUSTOM,
    customDays = 10,
    anchorDate = LocalDate(2026, 10, 5),
    weekday = DayOfWeek.SUNDAY,
    time = LocalTime(7, 30),
    metrics = setOf(ReviewMetric.SLEEP, ReviewMetric.WEIGHT),
    remindersEnabled = true,
    nextDueAt = T0 + 10.days,
)

fun review(planId: String) = ReviewEntity(
    id = "rev_1",
    planId = planId,
    scheduleId = "sched_1",
    dueAt = T0 + 10.days,
    completedAt = null,
    status = ReviewStatus.PROPOSED,
    dataQuality = DataQuality.LIMITED,
)

fun checkIn() = CheckInEntity(
    id = "ci_1",
    reviewId = "rev_1",
    weightKg = null,
    waistCm = null,
    hipCm = null,
    sessionsPlanned = 4,
    sessionsDone = 3,
    avgSleepH = 7.5,
    avgSteps = 8000,
    energy = 4,
    fatigue = 2,
    painNotes = "",
    painDate = null,
    notes = "",
)

fun feedback(id: String, planExerciseId: String) = ExerciseFeedbackEntity(
    id = id,
    checkInId = "ci_1",
    planExerciseId = planExerciseId,
    usualLoad = 25.0,
    loadUnitNote = "número del selector",
    repsReached = listOf(12, 10, 9),
    rirPerceived = 2,
    rating = FeedbackRating.OK,
    techniqueOk = true,
    equipmentChanged = false,
    sessionsWithData = 3,
    notes = "",
)

fun proposal(id: String = "prop_1") = ChangeProposalEntity(
    id = id,
    reviewId = "rev_1",
    type = ProposalType.SWAP_EXERCISE,
    targetExerciseCode = "L01",
    targetDayOfWeek = null,
    replacementEquipmentId = "eq_db",
    description = "Cambiar a mancuernas",
    rationale = "La máquina estuvo ocupada",
    confidence = Confidence.MEDIUM,
    decision = Decision.PENDING,
    decidedAt = null,
)

/** Calendario, revisión, check-in con un feedback y una propuesta, colgando de [planId]. */
suspend fun seedReviewData(db: RepOrRunDatabase, planId: String, planExerciseId: String) {
    val dao = db.reviewDao()
    dao.upsertSchedule(schedule(planId))
    dao.upsertReview(review(planId))
    db.checkInDao().saveCheckIn(checkIn(), listOf(feedback("fb_1", planExerciseId)))
    dao.insertProposals(listOf(proposal()))
    db.planHistoryDao().insertExport(ExportRecordEntity("exp_1", planId, T0, "plan.pdf", pages = 8))
}

/** Lunes de empuje con cardio; base para los días de [planRows]. */
private fun planDay(planId: String) = PlanDayEntity(
    id = "$planId-d0",
    planId = planId,
    dayOfWeek = DayOfWeek.MONDAY,
    dayIndex = 0,
    session = "Empuje",
    priority = "Pecho",
    cardio = CardioColumns(
        minMinutes = 10,
        maxMinutes = 15,
        mode = "Caminadora",
        rpe = "5-6",
        speed = "5 km/h",
        incline = null,
    ),
    warmup = "5 min suave",
    mobility = "Hombros",
    cooldown = "Estiramientos",
    status = DayStatus.PROPOSED,
)
