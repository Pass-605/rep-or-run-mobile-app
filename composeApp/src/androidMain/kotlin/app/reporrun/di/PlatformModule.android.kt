package app.reporrun.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import app.reporrun.core.database.RepOrRunDatabase
import app.reporrun.core.database.repOrRunDatabaseBuilder
import app.reporrun.core.datastore.settingsDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Lo que en Android necesita un `Context`: dónde está el archivo de Room y el de DataStore.
 * El contexto entra como parámetro y no sale de aquí, así que el resto del grafo no sabe que
 * existe (ni hace falta `koin-android`).
 */
internal fun androidPlatformModule(context: Context): Module = module {
    single<RoomDatabase.Builder<RepOrRunDatabase>> { repOrRunDatabaseBuilder(context) }
    single<DataStore<Preferences>> { settingsDataStore(context, processIoScope()) }
}

/**
 * Arranca el grafo de la app. Lo llama `RepOrRunApplication.onCreate()` y nadie más: el grafo
 * vive lo que vive el proceso, no lo que vive una actividad.
 */
fun startRepOrRun(context: Context) {
    startRepOrRunKoin(androidPlatformModule(context.applicationContext))
}
