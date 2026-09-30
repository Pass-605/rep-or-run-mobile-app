package app.reporrun.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/*
 * Lectura de un plan entero en una sola consulta transaccional. Room no garantiza el orden de
 * las listas de un @Relation: el mapper ordena por `dayIndex`, `order` y `position`.
 */

/** Un plan con sus días. */
data class PlanWithDays(
    @Embedded val plan: PlanEntity,
    @Relation(entity = PlanDayEntity::class, parentColumn = "id", entityColumn = "planId")
    val days: List<PlanDayWithExercises>,
)

/** Un día con sus ejercicios. */
data class PlanDayWithExercises(
    @Embedded val day: PlanDayEntity,
    @Relation(entity = PlanExerciseEntity::class, parentColumn = "id", entityColumn = "planDayId")
    val exercises: List<PlanExerciseWithAlternatives>,
)

/** Un ejercicio con sus alternativas. */
data class PlanExerciseWithAlternatives(
    @Embedded val exercise: PlanExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "planExerciseId")
    val alternatives: List<PlanExerciseAlternativeEntity>,
)

/** Un check-in con el feedback de cada ejercicio. */
data class CheckInWithFeedback(
    @Embedded val checkIn: CheckInEntity,
    @Relation(parentColumn = "id", entityColumn = "checkInId")
    val feedback: List<ExerciseFeedbackEntity>,
)
