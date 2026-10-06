package app.reporrun.core.domain.di

import app.reporrun.core.domain.usecase.AcceptConsentUseCase
import app.reporrun.core.domain.usecase.AddEquipmentUseCase
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.CreateGymUseCase
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import app.reporrun.core.domain.usecase.ObserveGymsUseCase
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.domain.usecase.RenameGymUseCase
import app.reporrun.core.domain.usecase.SavePreferencesUseCase
import app.reporrun.core.domain.usecase.SaveProfileUseCase
import app.reporrun.core.domain.usecase.SetActiveGymUseCase
import app.reporrun.core.domain.usecase.ToggleEquipmentUseCase
import app.reporrun.core.domain.usecase.UuidIdGenerator
import app.reporrun.core.domain.usecase.ValidateProfileUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Los casos de uso de `:core:domain`. Un caso de uso nuevo se añade aquí, en la misma change
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
    factoryOf(::SavePreferencesUseCase)
    factoryOf(::BuildGenerationSummaryUseCase)
    factoryOf(::ObserveConsentUseCase)
    factoryOf(::AcceptConsentUseCase)
}
