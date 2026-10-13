package app.reporrun.feature.settings

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** El `val xxxModule` del feature: su ViewModel y nada más. La pantalla de licencias no tiene. */
val settingsModule: Module = module {
    viewModelOf(::SettingsViewModel)
}
