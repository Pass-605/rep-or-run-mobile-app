package app.reporrun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** DataStore de ajustes de iOS, en Documents, junto a la base de datos. */
@OptIn(ExperimentalForeignApi::class)
fun settingsDataStore(scope: CoroutineScope): DataStore<Preferences> = createSettingsDataStore(scope) {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    "${checkNotNull(documents?.path) { "sin carpeta Documents" }}/$SETTINGS_FILE_NAME"
}
