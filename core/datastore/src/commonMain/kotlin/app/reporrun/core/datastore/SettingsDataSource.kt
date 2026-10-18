package app.reporrun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import kotlin.time.Instant

/**
 * Ajustes de la instalación en DataStore Preferences: claves simples que no son datos de
 * entrenamiento (plan §9.3). Aquí no hay datos de salud.
 *
 * Solo puede haber **un** [DataStore] por archivo en todo el proceso: el grafo de la app crea
 * uno con [createSettingsDataStore] y lo comparte.
 */
class SettingsDataSource(private val dataStore: DataStore<Preferences>) {
    /** Cuándo se aceptó el consentimiento, o `null` si no se ha aceptado. */
    val consentAcceptedAt: Flow<Instant?> = dataStore.data
        .map { preferences -> preferences[CONSENT_ACCEPTED_AT]?.let(Instant::fromEpochMilliseconds) }
        .distinctUntilChanged()

    /** Guarda la aceptación del consentimiento, o la borra con `null`. */
    suspend fun setConsentAcceptedAt(acceptedAt: Instant?) {
        dataStore.edit { preferences ->
            if (acceptedAt == null) {
                preferences.remove(CONSENT_ACCEPTED_AT)
            } else {
                preferences[CONSENT_ACCEPTED_AT] = acceptedAt.toEpochMilliseconds()
            }
        }
    }

    /**
     * El identificador de la instalación. La primera vez lo crea con [generate] y lo guarda;
     * después devuelve siempre el mismo. La edición de DataStore es atómica, así que dos
     * llamadas simultáneas no generan dos identificadores.
     */
    suspend fun installId(generate: () -> String): String {
        dataStore.data.first()[INSTALL_ID]?.let { return it }
        val updated = dataStore.edit { preferences ->
            if (preferences[INSTALL_ID] == null) preferences[INSTALL_ID] = generate()
        }
        return checkNotNull(updated[INSTALL_ID]) { "installId no guardado" }
    }

    /**
     * El tema elegido, tal como se guardó, o `null` si no se ha elegido ninguno. Es texto: qué
     * valores son válidos lo sabe quien lo traduce, no este módulo.
     */
    val themeMode: Flow<String?> = dataStore.data
        .map { preferences -> preferences[THEME_MODE] }
        .distinctUntilChanged()

    /** Guarda el tema elegido. */
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences -> preferences[THEME_MODE] = mode }
    }

    /**
     * De dónde sale el plan en depuración, tal como se guardó, o `null` si no se ha elegido.
     * Texto, como [themeMode].
     */
    val aiBackend: Flow<String?> = dataStore.data
        .map { preferences -> preferences[AI_BACKEND] }
        .distinctUntilChanged()

    /** Guarda de dónde sale el plan en depuración. */
    suspend fun setAiBackend(backend: String) {
        dataStore.edit { preferences -> preferences[AI_BACKEND] = backend }
    }

    private companion object {
        val CONSENT_ACCEPTED_AT = longPreferencesKey("consent_accepted_at")
        val INSTALL_ID = stringPreferencesKey("install_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AI_BACKEND = stringPreferencesKey("ai_backend")
    }
}

/** Nombre del archivo de ajustes. DataStore Preferences exige la extensión `.preferences_pb`. */
const val SETTINGS_FILE_NAME: String = "reporrun_settings.preferences_pb"

/**
 * DataStore de ajustes en la ruta absoluta que da [producePath]. Las lecturas y escrituras
 * corren en [scope], que inyecta el grafo de la app (con un dispatcher de E/S); cancelarlo
 * cierra el DataStore. Cada plataforma tiene su atajo: `settingsDataStore(context, scope)` en
 * Android y `settingsDataStore(scope)` en iOS.
 */
fun createSettingsDataStore(scope: CoroutineScope, producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { producePath().toPath() })
