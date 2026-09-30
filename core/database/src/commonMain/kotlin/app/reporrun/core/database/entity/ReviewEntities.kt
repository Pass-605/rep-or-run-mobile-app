package app.reporrun.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.reporrun.core.model.Confidence
import app.reporrun.core.model.DataQuality
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Decision
import app.reporrun.core.model.FeedbackRating
import app.reporrun.core.model.ProposalType
import app.reporrun.core.model.ReviewFrequency
import app.reporrun.core.model.ReviewMetric
import app.reporrun.core.model.ReviewStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/*
 * Revisiones: plan → review_schedule → review → check_in → exercise_feedback, y
 * review → change_proposal. Todo cuelga del plan con `CASCADE`: borrar un plan borra sus
 * revisiones y lo que se registró en ellas.
 */

/** Fila de `ReviewSchedule`. */
@Entity(
    tableName = "review_schedule",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class ReviewScheduleEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val frequency: ReviewFrequency,
    val customDays: Int?,
    val anchorDate: LocalDate,
    val weekday: DayOfWeek?,
    val time: LocalTime,
    val metrics: Set<ReviewMetric>,
    val remindersEnabled: Boolean,
    val nextDueAt: Instant,
)

/** Fila de `Review`. */
@Entity(
    tableName = "review",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReviewScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId"), Index("scheduleId")],
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val scheduleId: String,
    val dueAt: Instant,
    val completedAt: Instant?,
    val status: ReviewStatus,
    val dataQuality: DataQuality?,
)

/** Fila de `CheckIn`. */
@Entity(
    tableName = "check_in",
    foreignKeys = [
        ForeignKey(
            entity = ReviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reviewId")],
)
data class CheckInEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val weightKg: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val sessionsPlanned: Int,
    val sessionsDone: Int,
    val avgSleepH: Double?,
    val avgSteps: Int?,
    val energy: Int?,
    val fatigue: Int?,
    val painNotes: String,
    val painDate: LocalDate?,
    val notes: String,
)

/**
 * Fila de `ExerciseFeedback`. Se va con su check-in y con el ejercicio del plan al que se
 * refiere. [repsReached] es un array JSON de enteros; `NULL` si no se registró.
 */
@Entity(
    tableName = "exercise_feedback",
    foreignKeys = [
        ForeignKey(
            entity = CheckInEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkInId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["planExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("checkInId"), Index("planExerciseId")],
)
data class ExerciseFeedbackEntity(
    @PrimaryKey val id: String,
    val checkInId: String,
    val planExerciseId: String,
    val usualLoad: Double?,
    val loadUnitNote: String,
    val repsReached: List<Int>?,
    val rirPerceived: Int?,
    val rating: FeedbackRating,
    val techniqueOk: Boolean?,
    val equipmentChanged: Boolean,
    val sessionsWithData: Int,
    val notes: String,
)

/**
 * Fila de `ChangeProposal`, con su `ProposedChange` aplanado en columnas (`type`,
 * `targetExerciseCode`, `targetDayOfWeek`, `replacementEquipmentId`, `description`,
 * `rationale`, `confidence`).
 *
 * [replacementEquipmentId] apunta al inventario sin `onDelete`, como los ejercicios del plan.
 */
@Entity(
    tableName = "change_proposal",
    foreignKeys = [
        ForeignKey(
            entity = ReviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["replacementEquipmentId"],
        ),
    ],
    indices = [Index("reviewId"), Index("replacementEquipmentId")],
)
data class ChangeProposalEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val type: ProposalType,
    val targetExerciseCode: String?,
    val targetDayOfWeek: DayOfWeek?,
    val replacementEquipmentId: String?,
    val description: String,
    val rationale: String,
    val confidence: Confidence,
    val decision: Decision,
    val decidedAt: Instant?,
)
