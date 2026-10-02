package app.reporrun.core.domain.usecase

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.LoadUnit
import kotlinx.datetime.LocalDate

/**
 * Las preferencias del bloque tal como salen del formulario, sin validar.
 *
 * @property blockDays v1 solo admite [BlockPreferences.BLOCK_DAYS_V1]; otro valor es un error.
 * @property cardioModes en palabras de la persona; se recortan y se quitan vacíos y repetidos.
 */
data class PreferencesInput(
    val availableDays: Set<DayOfWeek>,
    val sessionMinutes: Int?,
    val startDate: LocalDate?,
    val cardioModes: List<String>,
    val avoidExercises: String,
    val blockDays: Int = BlockPreferences.BLOCK_DAYS_V1,
)

/** Campos de [PreferencesInput] que pueden llevar un error. */
enum class PreferencesField { AVAILABLE_DAYS, SESSION_MINUTES, BLOCK_DAYS, CARDIO_MODES, AVOID_EXERCISES }

/**
 * Recorta el texto y deja los modos de cardio sin vacíos ni repetidos (ignorando mayúsculas):
 * un espacio de más no es un error de la persona.
 */
internal fun PreferencesInput.normalized(): PreferencesInput = copy(
    cardioModes = cardioModes.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() },
    avoidExercises = avoidExercises.trim(),
)

/** Todos los errores de unas preferencias ya [normalized]. */
internal fun PreferencesInput.errors(): List<FieldError<PreferencesField>> = FieldChecks<PreferencesField>().apply {
    if (availableDays.isEmpty()) add(PreferencesField.AVAILABLE_DAYS, FieldIssue.Required)
    required(PreferencesField.SESSION_MINUTES, sessionMinutes)
    inRange(PreferencesField.SESSION_MINUTES, sessionMinutes, BlockPreferences.SESSION_MINUTES_RANGE)
    inRange(PreferencesField.BLOCK_DAYS, blockDays, BlockPreferences.BLOCK_DAYS_V1..BlockPreferences.BLOCK_DAYS_V1)
    maxCount(PreferencesField.CARDIO_MODES, cardioModes.size, BlockPreferences.MAX_CARDIO_MODES)
    if (cardioModes.any { it.length > BlockPreferences.MAX_CARDIO_MODE_LENGTH }) {
        add(PreferencesField.CARDIO_MODES, FieldIssue.TooLong(BlockPreferences.MAX_CARDIO_MODE_LENGTH))
    }
    maxLength(PreferencesField.AVOID_EXERCISES, avoidExercises, BlockPreferences.MAX_AVOID_LENGTH)
}.errors

/** Construye el modelo. Solo se llama con [errors] vacía; `null` si falta un obligatorio. */
internal fun PreferencesInput.toPreferencesOrNull(id: BlockPreferencesId): BlockPreferences? {
    if (sessionMinutes == null) return null
    return BlockPreferences(
        id = id,
        availableDays = availableDays,
        sessionMinutes = sessionMinutes,
        blockDays = blockDays,
        startDate = startDate,
        cardioModes = cardioModes,
        avoidExercises = avoidExercises,
        unit = LoadUnit.KG,
    )
}
