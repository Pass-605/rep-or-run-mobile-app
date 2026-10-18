package app.reporrun.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import app.reporrun.core.database.RepOrRunDatabase
import app.reporrun.core.database.repOrRunDatabaseBuilder
import app.reporrun.core.datastore.settingsDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Lo que en Android necesita un `Context`: dónde está el archivo de Room y el de DataStore, y
 * la versión del paquete y si es de depuración. Esta librería no tiene `BuildConfig.DEBUG`
 * propio y el de `androidApp` no se ve desde aquí; `FLAG_DEBUGGABLE` lo pone AGP según el
 * build type del APK, que es lo mismo.
 * El contexto entra como parámetro y no sale de aquí, así que el resto del grafo no sabe que
 * existe (ni hace falta `koin-android`).
 */
internal fun androidPlatformModule(context: Context): Module = module {
    single<RoomDatabase.Builder<RepOrRunDatabase>> { repOrRunDatabaseBuilder(context) }
    single<DataStore<Preferences>> { settingsDataStore(context, processIoScope()) }
    single {
        AppInfo(
            versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
            isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        )
    }
}

/**
 * Arranca el grafo de la app. Lo llama `RepOrRunApplication.onCreate()` y nadie más: el grafo
 * vive lo que vive el proceso, no lo que vive una actividad.
 */
fun startRepOrRun(context: Context) {
    startRepOrRunKoin(androidPlatformModule(context.applicationContext))
}
