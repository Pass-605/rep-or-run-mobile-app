package app.reporrun.core.domain.usecase

import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlin.time.Instant

/**
 * El perfil tal como sale del formulario, sin validar. Los obligatorios son anulables para que
 * «falta» sea un error más y no una decisión de la pantalla.
 */
data class ProfileInput(
    val alias: String?,
    val age: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val experience: Experience?,
    val goal: Goal?,
    val priorityMuscles: Set<MuscleGroup>,
    val limitations: String,
    val preferences: String,
)

/** Campos de [ProfileInput] que pueden llevar un error. */
enum class ProfileField {
    ALIAS,
    AGE,
    HEIGHT_CM,
    WEIGHT_KG,
    WAIST_CM,
    HIP_CM,
    EXPERIENCE,
    GOAL,
    PRIORITY_MUSCLES,
    LIMITATIONS,
    PREFERENCES,
}

/**
 * Reglas de negocio del perfil que no son las del contrato. El resto de límites (talla, peso,
 * perímetros, longitudes) son los de [UserProfile], que son los del contrato.
 */
object ProfileRules {
    const val MIN_AGE: Int = 16
    const val MAX_AGE: Int = 90

    /**
     * Edad admitida. Más estrecha que [UserProfile.AGE_RANGE] por arriba; por abajo no puede
     * bajar de lo que admite el contrato.
     */
    val AGE_RANGE: IntRange = MIN_AGE..MAX_AGE
}

/** Recorta el texto libre; un alias en blanco es «sin alias». */
internal fun ProfileInput.normalized(): ProfileInput = copy(
    alias = alias?.trim()?.ifEmpty { null },
    limitations = limitations.trim(),
    preferences = preferences.trim(),
)

/** Todos los errores de un perfil ya [normalized]. Vacía si se puede construir el modelo. */
internal fun ProfileInput.errors(): List<FieldError<ProfileField>> = FieldChecks<ProfileField>().apply {
    maxLength(ProfileField.ALIAS, alias, UserProfile.MAX_ALIAS_LENGTH)
    required(ProfileField.AGE, age)
    inRange(ProfileField.AGE, age, ProfileRules.AGE_RANGE)
    required(ProfileField.HEIGHT_CM, heightCm)
    inRange(ProfileField.HEIGHT_CM, heightCm, UserProfile.HEIGHT_CM_RANGE)
    inRange(ProfileField.WEIGHT_KG, weightKg, UserProfile.WEIGHT_KG_RANGE)
    inRange(ProfileField.WAIST_CM, waistCm, UserProfile.GIRTH_CM_RANGE)
    inRange(ProfileField.HIP_CM, hipCm, UserProfile.GIRTH_CM_RANGE)
    required(ProfileField.EXPERIENCE, experience)
    required(ProfileField.GOAL, goal)
    maxCount(ProfileField.PRIORITY_MUSCLES, priorityMuscles.size, UserProfile.MAX_PRIORITY_MUSCLES)
    maxLength(ProfileField.LIMITATIONS, limitations, UserProfile.MAX_FREE_TEXT_LENGTH)
    maxLength(ProfileField.PREFERENCES, preferences, UserProfile.MAX_FREE_TEXT_LENGTH)
}.errors

/**
 * Construye el modelo. Solo se llama con [errors] vacía: es lo que garantiza que el constructor
 * de [UserProfile] no lanza. `null` si falta un obligatorio.
 */
internal fun ProfileInput.toProfileOrNull(id: ProfileId, updatedAt: Instant): UserProfile? {
    val hasMeasures = age != null && heightCm != null
    val hasChoices = experience != null && goal != null
    if (!hasMeasures || !hasChoices) return null
    return UserProfile(
        id = id,
        alias = alias,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        waistCm = waistCm,
        hipCm = hipCm,
        experience = experience,
        goal = goal,
        priorityMuscles = priorityMuscles,
        limitations = limitations,
        preferences = preferences,
        updatedAt = updatedAt,
    )
}
