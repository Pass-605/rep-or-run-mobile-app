package app.reporrun.core.datastore.di

import app.reporrun.core.datastore.SettingsDataSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * [SettingsDataSource] sobre el `DataStore<Preferences>` del grafo. Ese DataStore lo crea el
 * `platformModule` de `composeApp`, una sola vez: dos sobre el mismo archivo fallan.
 */
val datastoreModule: Module = module {
    singleOf(::SettingsDataSource)
}
