package app.reporrun.core.model

import kotlin.time.Instant

/**
 * Perfil de la persona (plan §10). Los límites son los del contrato (`Profile` en
 * `common.schema.json`), así un perfil válido aquí siempre se puede enviar.
 *
 * Edad, talla y medidas describen a la persona; **nada** calcula cargas a partir de ellas
 * (plan §4.4). [limitations] se respeta como restricción, sin interpretarla clínicamente
 * (plan §4.9).
 *
 * @property alias nombre opcional para la UI; nunca un nombre real obligatorio.
 * @property priorityMuscles hasta [MAX_PRIORITY_MUSCLES] grupos, sin repetir.
 */
data class UserProfile(
    val id: ProfileId,
    val alias: String?,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val experience: Experience,
    val goal: Goal,
    val priorityMuscles: Set<MuscleGroup>,
    val limitations: String,
    val preferences: String,
    val updatedAt: Instant,
) {
    init {
        require(alias == null || alias.length <= MAX_ALIAS_LENGTH) { "alias demasiado largo" }
        require(age in AGE_RANGE) { "age fuera de $AGE_RANGE" }
        require(heightCm in HEIGHT_CM_RANGE) { "heightCm fuera de $HEIGHT_CM_RANGE" }
        require(weightKg == null || weightKg in WEIGHT_KG_RANGE) { "weightKg fuera de $WEIGHT_KG_RANGE" }
        require(waistCm == null || waistCm in GIRTH_CM_RANGE) { "waistCm fuera de $GIRTH_CM_RANGE" }
        require(hipCm == null || hipCm in GIRTH_CM_RANGE) { "hipCm fuera de $GIRTH_CM_RANGE" }
        require(priorityMuscles.size <= MAX_PRIORITY_MUSCLES) { "más de $MAX_PRIORITY_MUSCLES músculos prioritarios" }
        require(limitations.length <= MAX_FREE_TEXT_LENGTH) { "limitations demasiado largo" }
        require(preferences.length <= MAX_FREE_TEXT_LENGTH) { "preferences demasiado largo" }
    }

    /** Límites del contrato, expuestos para que la UI valide con los mismos números. */
    companion object {
        const val MAX_ALIAS_LENGTH: Int = 40
        const val MAX_PRIORITY_MUSCLES: Int = 4
        const val MAX_FREE_TEXT_LENGTH: Int = 600
        val AGE_RANGE: IntRange = 16..100
        val HEIGHT_CM_RANGE: ClosedFloatingPointRange<Double> = 120.0..230.0
        val WEIGHT_KG_RANGE: ClosedFloatingPointRange<Double> = 30.0..300.0
        val GIRTH_CM_RANGE: ClosedFloatingPointRange<Double> = 40.0..200.0
    }
}
