package app.reporrun.core.domain.di

import app.reporrun.core.domain.usecase.AcceptConsentUseCase
import app.reporrun.core.domain.usecase.ActivatePlanUseCase
import app.reporrun.core.domain.usecase.AddEquipmentUseCase
import app.reporrun.core.domain.usecase.ApprovePlanDayUseCase
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.CreateGymUseCase
import app.reporrun.core.domain.usecase.GeneratePlanUseCase
import app.reporrun.core.domain.usecase.HasSavedPlansUseCase
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.domain.usecase.LoadCatalogSeedUseCase
import app.reporrun.core.domain.usecase.ObserveCardioOptionsUseCase
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import app.reporrun.core.domain.usecase.ObserveGymInventoryUseCase
import app.reporrun.core.domain.usecase.ObserveGymsUseCase
import app.reporrun.core.domain.usecase.ObservePlanUseCase
import app.reporrun.core.domain.usecase.ObservePreferencesUseCase
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.domain.usecase.ObserveThemeModeUseCase
import app.reporrun.core.domain.usecase.RemoveEquipmentUseCase
import app.reporrun.core.domain.usecase.RenameGymUseCase
import app.reporrun.core.domain.usecase.SavePreferencesUseCase
import app.reporrun.core.domain.usecase.SaveProfileUseCase
import app.reporrun.core.domain.usecase.SetActiveGymUseCase
import app.reporrun.core.domain.usecase.SetThemeModeUseCase
import app.reporrun.core.domain.usecase.ToggleEquipmentUseCase
import app.reporrun.core.domain.usecase.UpdateEquipmentUseCase
import app.reporrun.core.domain.usecase.UuidIdGenerator
import app.reporrun.core.domain.usecase.ValidateProfileUseCase
import app.reporrun.core.domain.validation.PlanValidator
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Los casos de uso y validadores de `:core:domain`. Un caso de uso nuevo se añade aquí, en la misma change
 * que lo crea.
 *
 * Son `factory` porque no guardan estado: cada ViewModel recibe el suyo. Los repositorios, el
 * `Clock` y los dispatchers que piden por constructor no se definen aquí: los pone el grafo de
 * `composeApp` (`dataModule` y `appModule`).
 */
val domainModule: Module = module {
    single<IdGenerator> { UuidIdGenerator }

    factoryOf(::SaveProfileUseCase)
    factoryOf(::ValidateProfileUseCase)
    factoryOf(::ObserveProfileUseCase)
    factoryOf(::CreateGymUseCase)
    factoryOf(::SetActiveGymUseCase)
    factoryOf(::RenameGymUseCase)
    factoryOf(::ObserveGymsUseCase)
    factoryOf(::AddEquipmentUseCase)
    factoryOf(::ToggleEquipmentUseCase)
    factoryOf(::UpdateEquipmentUseCase)
    factoryOf(::RemoveEquipmentUseCase)
    factoryOf(::ObserveGymInventoryUseCase)
    factoryOf(::LoadCatalogSeedUseCase)
    factoryOf(::SavePreferencesUseCase)
    factoryOf(::ObservePreferencesUseCase)
    factoryOf(::ObserveCardioOptionsUseCase)
    factoryOf(::BuildGenerationSummaryUseCase)
    factoryOf(::GeneratePlanUseCase)
    factoryOf(::ObservePlanUseCase)
    factoryOf(::ApprovePlanDayUseCase)
    factoryOf(::ActivatePlanUseCase)
    factoryOf(::HasSavedPlansUseCase)
    factoryOf(::ObserveConsentUseCase)
    factoryOf(::AcceptConsentUseCase)
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)

    factoryOf(::PlanValidator)
}
