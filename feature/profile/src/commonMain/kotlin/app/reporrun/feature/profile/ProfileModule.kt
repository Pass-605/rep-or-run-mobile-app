package app.reporrun.feature.profile

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * El `val xxxModule` de un feature: sus ViewModels y nada más. `viewModelOf` resuelve el
 * constructor contra el grafo, así que añadir un caso de uso al ViewModel no toca este archivo.
 */
val profileModule: Module = module {
    viewModelOf(::ProfileViewModel)
}
