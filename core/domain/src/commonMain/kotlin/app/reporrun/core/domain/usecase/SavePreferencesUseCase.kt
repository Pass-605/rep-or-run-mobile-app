package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Resultado de [SavePreferencesUseCase]. */
sealed interface SavePreferencesResult {
    /** Las preferencias quedaron guardadas como [preferences], ya normalizadas. */
    data class Saved(val preferences: BlockPreferences) : SavePreferencesResult

    /** No se guardó nada. [errors] trae todos los campos que fallan. */
    data class Invalid(val errors: List<FieldError<PreferencesField>>) : SavePreferencesResult
}

/**
 * Valida y guarda las preferencias del siguiente bloque. Hay una sola fila por instalación:
 * guardar sustituye a la anterior. Sin al menos un día disponible no se guarda nada.
 */
class SavePreferencesUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(input: PreferencesInput): SavePreferencesResult {
        val clean = input.normalized()
        val errors = clean.errors()
        val preferences = if (errors.isEmpty()) clean.toPreferencesOrNull(currentId()) else null
        return if (preferences == null) {
            SavePreferencesResult.Invalid(errors)
        } else {
            repository.savePreferences(preferences)
            SavePreferencesResult.Saved(preferences)
        }
    }

    private suspend fun currentId(): BlockPreferencesId =
        repository.observePreferences().first()?.id ?: SINGLE_PREFERENCES_ID

    private companion object {
        val SINGLE_PREFERENCES_ID = BlockPreferencesId("preferences")
    }
}

/** Las preferencias guardadas, o `null` si todavía no se han configurado. */
class ObservePreferencesUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<BlockPreferences?> = repository.observePreferences()
}
