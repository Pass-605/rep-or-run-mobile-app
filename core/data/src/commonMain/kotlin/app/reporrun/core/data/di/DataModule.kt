package app.reporrun.core.data.di

import app.reporrun.core.data.repository.DeveloperSettingsRepositoryImpl
import app.reporrun.core.data.repository.EquipmentCatalogRepositoryImpl
import app.reporrun.core.data.repository.GymRepositoryImpl
import app.reporrun.core.data.repository.PlanRepositoryImpl
import app.reporrun.core.data.repository.PreferencesRepositoryImpl
import app.reporrun.core.data.repository.ProfileRepositoryImpl
import app.reporrun.core.data.repository.ReviewRepositoryImpl
import app.reporrun.core.data.repository.SettingsRepositoryImpl
import app.reporrun.core.domain.repository.DeveloperSettingsRepository
import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.ReviewRepository
import app.reporrun.core.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Los repositorios, registrados solo por su interfaz de `:core:domain`: nadie fuera de este
 * módulo puede pedir un `XxxRepositoryImpl`. Los DAOs y `SettingsDataSource` vienen de
 * `databaseModule` y `datastoreModule`.
 *
 * Son `single`: no guardan estado propio, pero comparten los `Flow` de Room y no hay motivo
 * para crear uno por pantalla.
 */
val dataModule: Module = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<GymRepository> { GymRepositoryImpl(get(), get()) }
    single<EquipmentCatalogRepository> { EquipmentCatalogRepositoryImpl(get()) }
    single<PlanRepository> { PlanRepositoryImpl(get(), get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<DeveloperSettingsRepository> { DeveloperSettingsRepositoryImpl(get()) }
}
