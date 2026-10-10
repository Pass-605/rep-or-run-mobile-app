package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable

/** `review-request.schema.json`: el cuerpo de `POST /v1/plans:review`. */
@Serializable
data class ReviewPlanRequest(
    val schemaVersion: String,
    val promptVersion: String,
    val locale: String,
    val daysElapsed: Int,
    val plan: PlanDto,
    val gym: GymDto,
    val profileSummary: ProfileSummaryDto,
    val checkIn: CheckInDto,
    val exerciseFeedback: List<ExerciseFeedbackDto>,
    val previousReviews: List<PreviousReviewDto>,
)

/** `ProfileSummary`: la parte del perfil que basta para revisar. */
@Serializable
data class ProfileSummaryDto(
    val age: Int,
    val experience: ExperienceDto,
    val goal: GoalDto,
    val priorityMuscles: List<MuscleGroupDto>,
    val limitations: String,
    val sessionMinutes: Int,
    val availableDays: List<DayOfWeekDto>,
)

/** `CheckIn`. [painDate] es una fecha ISO tal cual viaja. */
@Serializable
data class CheckInDto(
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val sessionsPlanned: Int,
    val sessionsDone: Int,
    val avgSleepH: Double? = null,
    val avgSteps: Int? = null,
    val energy: Int? = null,
    val fatigue: Int? = null,
    val painNotes: String,
    val painDate: String? = null,
    val notes: String,
)

/** `ExerciseFeedback`: cómo fue un ejercicio del plan vigente durante el bloque. */
@Serializable
data class ExerciseFeedbackDto(
    val exerciseCode: String,
    val usualLoad: Double? = null,
    val loadUnitNote: String,
    val repsReached: List<Int>? = null,
    val rirPerceived: Int? = null,
    val rating: FeedbackRatingDto,
    val techniqueOk: Boolean? = null,
    val equipmentChanged: Boolean,
    val sessionsWithData: Int,
    val notes: String,
)

/** `PreviousReview`. [date] es una fecha ISO tal cual viaja. */
@Serializable
data class PreviousReviewDto(val date: String, val summary: String, val appliedChanges: List<String>)

/**
 * `review-response.schema.json`: la respuesta 200 de `POST /v1/plans:review`.
 * [generatedAt] es una fecha y hora ISO tal cual viaja.
 */
@Serializable
data class ReviewPlanResponse(
    val schemaVersion: String,
    val promptVersion: String,
    val model: String,
    val generatedAt: String,
    val usage: UsageDto,
    val review: ReviewResultDto,
)

/**
 * `review-result.schema.json`: lo que devuelve el modelo al revisar. Son propuestas; nada de
 * aquí cambia el plan, y nada es de fiar hasta pasar por `ProposalValidator`.
 */
@Serializable
data class ReviewResultDto(
    val dataQuality: DataQualityDto,
    val summary: String,
    val observations: List<String>,
    val proposals: List<ProposalDto>,
    val questions: List<String>,
)

/** `Proposal`. El schema es estricto: los anulables también son obligatorios. */
@Serializable
data class ProposalDto(
    val type: ProposalTypeDto,
    val targetExerciseCode: String?,
    val targetDayOfWeek: DayOfWeekDto?,
    val replacementEquipmentId: String?,
    val description: String,
    val rationale: String,
    val confidence: ConfidenceDto,
)
