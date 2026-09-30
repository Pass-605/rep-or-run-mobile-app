package app.reporrun.core.domain.repository

import app.reporrun.core.model.BlockPreferences
import kotlinx.coroutines.flow.Flow

/** Preferencias con las que se pide el siguiente bloque. Hay una sola fila por instalación. */
interface PreferencesRepository {
    /** Las preferencias guardadas, o `null` si todavía no se han configurado. */
    fun observePreferences(): Flow<BlockPreferences?>

    /** Crea o sustituye las preferencias. */
    suspend fun savePreferences(preferences: BlockPreferences)
}
