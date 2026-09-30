package app.reporrun.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope

/** DataStore de ajustes de Android, en la carpeta de archivos privada de la app. */
fun settingsDataStore(context: Context, scope: CoroutineScope): DataStore<Preferences> {
    val appContext = context.applicationContext
    return createSettingsDataStore(scope) { appContext.filesDir.resolve(SETTINGS_FILE_NAME).absolutePath }
}
