// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.profile

import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.ProfileField
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.MuscleGroup

/**
 * El formulario del perfil, entero: los dos pasos comparten estado porque comparten ViewModel.
 *
 * - Los números van como **texto**, tal como están en el campo: «72,5», «», «17». Convertirlos
 *   y decidir si valen es cosa del ViewModel y del caso de uso, no del estado.
 * - [errors] lleva el motivo que dio el caso de uso, con su límite. La frase la pone la
 *   pantalla; aquí no hay textos.
 * - [isLoading] empieza en `true`: hasta saber si hay un perfil guardado no se pinta el
 *   formulario, para que no aparezca vacío y se rellene de golpe.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val alias: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val waist: String = "",
    val hip: String = "",
    val experience: Experience? = null,
    val goal: Goal? = null,
    val priorityMuscles: Set<MuscleGroup> = emptySet(),
    val limitations: String = "",
    val preferences: String = "",
    val errors: Map<ProfileField, FieldIssue> = emptyMap(),
    val isSaving: Boolean = false,
)

/** Los campos que se teclean. Cada uno sabe a qué campo del perfil corresponde su error. */
enum class ProfileTextField(val field: ProfileField) {
    ALIAS(ProfileField.ALIAS),
    AGE(ProfileField.AGE),
    HEIGHT(ProfileField.HEIGHT_CM),
    WEIGHT(ProfileField.WEIGHT_KG),
    WAIST(ProfileField.WAIST_CM),
    HIP(ProfileField.HIP_CM),
    LIMITATIONS(ProfileField.LIMITATIONS),
    PREFERENCES(ProfileField.PREFERENCES),
}

sealed interface ProfileEvent {
    data class TextChanged(val field: ProfileTextField, val value: String) : ProfileEvent

    data class ExperienceSelected(val experience: Experience) : ProfileEvent

    data class GoalSelected(val goal: Goal) : ProfileEvent

    data class MuscleToggled(val muscle: MuscleGroup) : ProfileEvent

    /** Fin del paso 1. */
    data object ContinueClicked : ProfileEvent

    /** Fin del paso 2. */
    data object SaveClicked : ProfileEvent
}

sealed interface ProfileEffect {
    /** El paso 1 no tiene errores: se puede pasar al 2. */
    data object BasicsCompleted : ProfileEffect

    /** Al guardar apareció un error en un campo del paso 1: hay que volver a él para verlo. */
    data object BasicsNeedReview : ProfileEffect

    /** El perfil ya está guardado. */
    data object ProfileSaved : ProfileEffect
}
