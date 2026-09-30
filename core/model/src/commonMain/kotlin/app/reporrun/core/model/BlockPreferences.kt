package app.reporrun.core.model

import kotlinx.datetime.LocalDate

/**
 * Preferencias del bloque que se pide generar (plan §10, `BlockPreferences` del contrato).
 *
 * @property availableDays días en los que la persona puede entrenar; el plan solo usa estos.
 * @property blockDays duración del bloque; v1 la fija en [BLOCK_DAYS_V1].
 * @property cardioModes modos de cardio en palabras de la persona ("Caminadora"), sin repetir.
 * @property avoidExercises ejercicios que no quiere hacer, en texto libre.
 */
data class BlockPreferences(
    val id: BlockPreferencesId,
    val availableDays: Set<DayOfWeek>,
    val sessionMinutes: Int,
    val blockDays: Int,
    val startDate: LocalDate?,
    val cardioModes: List<String>,
    val avoidExercises: String,
    val unit: LoadUnit,
) {
    init {
        require(availableDays.isNotEmpty()) { "hace falta al menos un día disponible" }
        require(sessionMinutes in SESSION_MINUTES_RANGE) { "sessionMinutes fuera de $SESSION_MINUTES_RANGE" }
        require(blockDays == BLOCK_DAYS_V1) { "v1 solo admite bloques de $BLOCK_DAYS_V1 días" }
        require(cardioModes.size <= MAX_CARDIO_MODES) { "más de $MAX_CARDIO_MODES modos de cardio" }
        require(cardioModes.distinct().size == cardioModes.size) { "modos de cardio repetidos" }
        require(cardioModes.all { it.isNotBlank() && it.length <= MAX_CARDIO_MODE_LENGTH }) {
            "modo de cardio vacío o demasiado largo"
        }
        require(avoidExercises.length <= MAX_AVOID_LENGTH) { "avoidExercises demasiado largo" }
    }

    /** Límites del contrato. */
    companion object {
        const val BLOCK_DAYS_V1: Int = 14
        val SESSION_MINUTES_RANGE: IntRange = 20..180
        const val MAX_CARDIO_MODES: Int = 6
        const val MAX_CARDIO_MODE_LENGTH: Int = 40
        const val MAX_AVOID_LENGTH: Int = 400
    }
}
