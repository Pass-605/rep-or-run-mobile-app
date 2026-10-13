package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * El tema que eligió la persona; [ThemeMode.SYSTEM] mientras no elija ninguno. Vuelve a emitir
 * cada vez que cambia, que es lo que permite aplicarlo sin reiniciar la app.
 */
class ObserveThemeModeUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.observeThemeMode()
}

/** Guarda el tema que la persona elige en Ajustes. */
class SetThemeModeUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
