package app.reporrun.core.data.mapper

import app.reporrun.core.database.entity.ChangeProposalEntity
import app.reporrun.core.database.entity.CheckInEntity
import app.reporrun.core.database.entity.CheckInWithFeedback
import app.reporrun.core.database.entity.ExerciseFeedbackEntity
import app.reporrun.core.database.entity.ReviewEntity
import app.reporrun.core.database.entity.ReviewScheduleEntity
import app.reporrun.core.model.ChangeProposal
import app.reporrun.core.model.ChangeProposalId
import app.reporrun.core.model.CheckIn
import app.reporrun.core.model.CheckInId
import app.reporrun.core.model.CheckInRecord
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ExerciseFeedback
import app.reporrun.core.model.ExerciseFeedbackId
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.ProposedChange
import app.reporrun.core.model.Review
import app.reporrun.core.model.ReviewId
import app.reporrun.core.model.ReviewSchedule
import app.reporrun.core.model.ReviewScheduleId

/*
 * Revisiones ↔ filas. Las propuestas vienen del LLM, pero llegan aquí ya construidas como
 * modelo (y validadas por ProposalValidator): la lectura construye directamente, como en el
 * perfil.
 */

internal fun ReviewSchedule.toEntity() = ReviewScheduleEntity(
    id = id.value,
    planId = planId.value,
    frequency = frequency,
    customDays = customDays,
    anchorDate = anchorDate,
    weekday = weekday,
    time = time,
    metrics = metrics,
    remindersEnabled = remindersEnabled,
    nextDueAt = nextDueAt,
)

internal fun ReviewScheduleEntity.toModel() = ReviewSchedule(
    id = ReviewScheduleId(id),
    planId = PlanId(planId),
    frequency = frequency,
    customDays = customDays,
    anchorDate = anchorDate,
    weekday = weekday,
    time = time,
    metrics = metrics,
    remindersEnabled = remindersEnabled,
    nextDueAt = nextDueAt,
)

internal fun Review.toEntity() = ReviewEntity(
    id = id.value,
    planId = planId.value,
    scheduleId = scheduleId.value,
    dueAt = dueAt,
    completedAt = completedAt,
    status = status,
    dataQuality = dataQuality,
)

internal fun ReviewEntity.toModel() = Review(
    id = ReviewId(id),
    planId = PlanId(planId),
    scheduleId = ReviewScheduleId(scheduleId),
    dueAt = dueAt,
    completedAt = completedAt,
    status = status,
    dataQuality = dataQuality,
)

internal fun CheckIn.toEntity() = CheckInEntity(
    id = id.value,
    reviewId = reviewId.value,
    weightKg = weightKg,
    waistCm = waistCm,
    hipCm = hipCm,
    sessionsPlanned = sessionsPlanned,
    sessionsDone = sessionsDone,
    avgSleepH = avgSleepH,
    avgSteps = avgSteps,
    energy = energy,
    fatigue = fatigue,
    painNotes = painNotes,
    painDate = painDate,
    notes = notes,
)

internal fun CheckInEntity.toModel() = CheckIn(
    id = CheckInId(id),
    reviewId = ReviewId(reviewId),
    weightKg = weightKg,
    waistCm = waistCm,
    hipCm = hipCm,
    sessionsPlanned = sessionsPlanned,
    sessionsDone = sessionsDone,
    avgSleepH = avgSleepH,
    avgSteps = avgSteps,
    energy = energy,
    fatigue = fatigue,
    painNotes = painNotes,
    painDate = painDate,
    notes = notes,
)

internal fun ExerciseFeedback.toEntity() = ExerciseFeedbackEntity(
    id = id.value,
    checkInId = checkInId.value,
    planExerciseId = planExerciseId.value,
    usualLoad = usualLoad,
    loadUnitNote = loadUnitNote,
    repsReached = repsReached,
    rirPerceived = rirPerceived,
    rating = rating,
    techniqueOk = techniqueOk,
    equipmentChanged = equipmentChanged,
    sessionsWithData = sessionsWithData,
    notes = notes,
)

internal fun ExerciseFeedbackEntity.toModel() = ExerciseFeedback(
    id = ExerciseFeedbackId(id),
    checkInId = CheckInId(checkInId),
    planExerciseId = PlanExerciseId(planExerciseId),
    usualLoad = usualLoad,
    loadUnitNote = loadUnitNote,
    repsReached = repsReached,
    rirPerceived = rirPerceived,
    rating = rating,
    techniqueOk = techniqueOk,
    equipmentChanged = equipmentChanged,
    sessionsWithData = sessionsWithData,
    notes = notes,
)

/** Room no garantiza el orden de un `@Relation`: el feedback se ordena por id. */
internal fun CheckInWithFeedback.toModel() =
    CheckInRecord(checkIn.toModel(), feedback.sortedBy { it.id }.map { it.toModel() })

internal fun ChangeProposal.toEntity() = ChangeProposalEntity(
    id = id.value,
    reviewId = reviewId.value,
    type = change.type,
    targetExerciseCode = change.targetExerciseCode?.value,
    targetDayOfWeek = change.targetDayOfWeek,
    replacementEquipmentId = change.replacementEquipmentId?.value,
    description = change.description,
    rationale = change.rationale,
    confidence = change.confidence,
    decision = decision,
    decidedAt = decidedAt,
)

internal fun ChangeProposalEntity.toModel() = ChangeProposal(
    id = ChangeProposalId(id),
    reviewId = ReviewId(reviewId),
    change = ProposedChange(
        type = type,
        targetExerciseCode = targetExerciseCode?.let(::ExerciseCode),
        targetDayOfWeek = targetDayOfWeek,
        replacementEquipmentId = replacementEquipmentId?.let(::EquipmentId),
        description = description,
        rationale = rationale,
        confidence = confidence,
    ),
    decision = decision,
    decidedAt = decidedAt,
)
