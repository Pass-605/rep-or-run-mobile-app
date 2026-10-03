package app.reporrun.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import app.reporrun.core.database.RepOrRunDatabase
import app.reporrun.core.database.repOrRunDatabaseBuilder
import app.reporrun.core.datastore.settingsDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

/** En iOS los dos archivos van en Documents y no hace falta nada de la plataforma. */
internal val iosPlatformModule: Module = module {
    single<RoomDatabase.Builder<RepOrRunDatabase>> { repOrRunDatabaseBuilder() }
    single<DataStore<Preferences>> { settingsDataStore(processIoScope()) }
}
