package app.reporrun.feature.profile

import androidx.compose.runtime.Composable
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.MuscleGroup
import app.reporrun.feature.profile.resources.Res
import app.reporrun.feature.profile.resources.profile_error_out_of_range
import app.reporrun.feature.profile.resources.profile_error_required
import app.reporrun.feature.profile.resources.profile_error_too_long
import app.reporrun.feature.profile.resources.profile_error_too_many
import app.reporrun.feature.profile.resources.profile_error_too_short
import app.reporrun.feature.profile.resources.profile_experience_advanced
import app.reporrun.feature.profile.resources.profile_experience_beginner
import app.reporrun.feature.profile.resources.profile_experience_intermediate
import app.reporrun.feature.profile.resources.profile_goal_general_fitness
import app.reporrun.feature.profile.resources.profile_goal_general_fitness_cardio
import app.reporrun.feature.profile.resources.profile_goal_general_fitness_meaning
import app.reporrun.feature.profile.resources.profile_goal_general_fitness_reps
import app.reporrun.feature.profile.resources.profile_goal_general_fitness_rest
import app.reporrun.feature.profile.resources.profile_goal_general_fitness_volume
import app.reporrun.feature.profile.resources.profile_goal_hypertrophy
import app.reporrun.feature.profile.resources.profile_goal_hypertrophy_cardio
import app.reporrun.feature.profile.resources.profile_goal_hypertrophy_meaning
import app.reporrun.feature.profile.resources.profile_goal_hypertrophy_reps
import app.reporrun.feature.profile.resources.profile_goal_hypertrophy_rest
import app.reporrun.feature.profile.resources.profile_goal_hypertrophy_volume
import app.reporrun.feature.profile.resources.profile_goal_recomposition
import app.reporrun.feature.profile.resources.profile_goal_recomposition_cardio
import app.reporrun.feature.profile.resources.profile_goal_recomposition_meaning
import app.reporrun.feature.profile.resources.profile_goal_recomposition_reps
import app.reporrun.feature.profile.resources.profile_goal_recomposition_rest
import app.reporrun.feature.profile.resources.profile_goal_recomposition_volume
import app.reporrun.feature.profile.resources.profile_goal_strength
import app.reporrun.feature.profile.resources.profile_goal_strength_cardio
import app.reporrun.feature.profile.resources.profile_goal_strength_meaning
import app.reporrun.feature.profile.resources.profile_goal_strength_reps
import app.reporrun.feature.profile.resources.profile_goal_strength_rest
import app.reporrun.feature.profile.resources.profile_goal_strength_volume
import app.reporrun.feature.profile.resources.profile_muscle_abductors
import app.reporrun.feature.profile.resources.profile_muscle_adductors
import app.reporrun.feature.profile.resources.profile_muscle_back
import app.reporrun.feature.profile.resources.profile_muscle_biceps
import app.reporrun.feature.profile.resources.profile_muscle_calves
import app.reporrun.feature.profile.resources.profile_muscle_chest
import app.reporrun.feature.profile.resources.profile_muscle_core
import app.reporrun.feature.profile.resources.profile_muscle_forearms
import app.reporrun.feature.profile.resources.profile_muscle_full_body
import app.reporrun.feature.profile.resources.profile_muscle_glutes
import app.reporrun.feature.profile.resources.profile_muscle_hamstrings
import app.reporrun.feature.profile.resources.profile_muscle_quads
import app.reporrun.feature.profile.resources.profile_muscle_shoulders
import app.reporrun.feature.profile.resources.profile_muscle_triceps
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Del dominio a los textos. Cada `when` es exhaustivo sin `else`: un valor nuevo en un enum del
 * contrato no compila hasta que tiene su texto.
 */

internal val Experience.label: StringResource
    get() = when (this) {
        Experience.BEGINNER -> Res.string.profile_experience_beginner
        Experience.INTERMEDIATE -> Res.string.profile_experience_intermediate
        Experience.ADVANCED -> Res.string.profile_experience_advanced
    }

internal val Goal.label: StringResource
    get() = when (this) {
        Goal.RECOMPOSITION -> Res.string.profile_goal_recomposition
        Goal.HYPERTROPHY -> Res.string.profile_goal_hypertrophy
        Goal.STRENGTH -> Res.string.profile_goal_strength
        Goal.GENERAL_FITNESS -> Res.string.profile_goal_general_fitness
    }

internal val MuscleGroup.label: StringResource
    get() = when (this) {
        MuscleGroup.CHEST -> Res.string.profile_muscle_chest
        MuscleGroup.BACK -> Res.string.profile_muscle_back
        MuscleGroup.SHOULDERS -> Res.string.profile_muscle_shoulders
        MuscleGroup.BICEPS -> Res.string.profile_muscle_biceps
        MuscleGroup.TRICEPS -> Res.string.profile_muscle_triceps
        MuscleGroup.FOREARMS -> Res.string.profile_muscle_forearms
        MuscleGroup.QUADS -> Res.string.profile_muscle_quads
        MuscleGroup.HAMSTRINGS -> Res.string.profile_muscle_hamstrings
        MuscleGroup.GLUTES -> Res.string.profile_muscle_glutes
        MuscleGroup.ABDUCTORS -> Res.string.profile_muscle_abductors
        MuscleGroup.ADDUCTORS -> Res.string.profile_muscle_adductors
        MuscleGroup.CALVES -> Res.string.profile_muscle_calves
        MuscleGroup.CORE -> Res.string.profile_muscle_core
        MuscleGroup.FULL_BODY -> Res.string.profile_muscle_full_body
    }

/**
 * Lo que la tarjeta cuenta de un objetivo: qué significa y cómo cambia el plan.
 *
 * **Son constantes de la app, no salida del modelo.** Si las redactara el LLM cambiarían de
 * una persona a otra y podrían contradecir el plan que luego genera. Aquí hay una por
 * objetivo, la misma para todo el mundo.
 */
internal data class GoalExplanation(
    val meaning: StringResource,
    val reps: StringResource,
    val volume: StringResource,
    val rest: StringResource,
    val cardio: StringResource,
)

internal val Goal.explanation: GoalExplanation
    get() = when (this) {
        Goal.RECOMPOSITION -> GoalExplanation(
            meaning = Res.string.profile_goal_recomposition_meaning,
            reps = Res.string.profile_goal_recomposition_reps,
            volume = Res.string.profile_goal_recomposition_volume,
            rest = Res.string.profile_goal_recomposition_rest,
            cardio = Res.string.profile_goal_recomposition_cardio,
        )

        Goal.HYPERTROPHY -> GoalExplanation(
            meaning = Res.string.profile_goal_hypertrophy_meaning,
            reps = Res.string.profile_goal_hypertrophy_reps,
            volume = Res.string.profile_goal_hypertrophy_volume,
            rest = Res.string.profile_goal_hypertrophy_rest,
            cardio = Res.string.profile_goal_hypertrophy_cardio,
        )

        Goal.STRENGTH -> GoalExplanation(
            meaning = Res.string.profile_goal_strength_meaning,
            reps = Res.string.profile_goal_strength_reps,
            volume = Res.string.profile_goal_strength_volume,
            rest = Res.string.profile_goal_strength_rest,
            cardio = Res.string.profile_goal_strength_cardio,
        )

        Goal.GENERAL_FITNESS -> GoalExplanation(
            meaning = Res.string.profile_goal_general_fitness_meaning,
            reps = Res.string.profile_goal_general_fitness_reps,
            volume = Res.string.profile_goal_general_fitness_volume,
            rest = Res.string.profile_goal_general_fitness_rest,
            cardio = Res.string.profile_goal_general_fitness_cardio,
        )
    }

/** La frase de un error. El límite viene del caso de uso; aquí no hay ningún número. */
@Composable
internal fun FieldIssue.message(): String = when (this) {
    FieldIssue.Required -> stringResource(Res.string.profile_error_required)

    is FieldIssue.OutOfRange -> stringResource(
        Res.string.profile_error_out_of_range,
        min.toFieldText(),
        max.toFieldText(),
    )

    is FieldIssue.TooShort -> stringResource(Res.string.profile_error_too_short, minLength)

    is FieldIssue.TooLong -> stringResource(Res.string.profile_error_too_long, maxLength)

    is FieldIssue.TooMany -> stringResource(Res.string.profile_error_too_many, max)
}
