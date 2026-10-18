package app.reporrun.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.domain.usecase.ObserveThemeModeUseCase
import app.reporrun.core.domain.usecase.SetThemeModeUseCase
import app.reporrun.core.model.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Ajustes observa tres cosas y escribe una: el tema. El perfil y el aviso se editan y se leen en
 * sus pantallas; aquí solo se enseña su resumen, que se pone al día solo al volver de ellas.
 *
 * **No retira el consentimiento**: lo enseña. Mientras sea así, la app no puede quedarse sin él
 * después de haberlo dado.
 *
 * @param timeZone para decir en qué día local se aceptó el aviso.
 */
class SettingsViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    observeProfile: ObserveProfileUseCase,
    observeConsent: ObserveConsentUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val timeZone: TimeZone,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeThemeMode().collect { mode -> _state.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            observeProfile().collect { profile ->
                _state.update { it.copy(profile = profile?.let { ProfileSummary(it.age, it.experience) }) }
            }
        }
        viewModelScope.launch {
            observeConsent().collect { acceptedAt ->
                _state.update { it.copy(consentAcceptedOn = acceptedAt?.toLocalDateTime(timeZone)?.date) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeModeSelected -> selectThemeMode(event.mode)
            SettingsEvent.ProfileClicked -> _effects.trySend(SettingsEffect.ProfileRequested)
            SettingsEvent.NoticeClicked -> _effects.trySend(SettingsEffect.NoticeRequested)
            SettingsEvent.LicensesClicked -> _effects.trySend(SettingsEffect.LicensesRequested)
        }
    }

    private fun selectThemeMode(mode: ThemeMode) {
        // El estado no se toca aquí: lo cambia lo guardado al emitir, igual que cambia el tema de
        // la app. Así el chip marcado y lo que se ve no pueden decir cosas distintas.
        if (_state.value.themeMode == mode) return
        viewModelScope.launch { setThemeMode(mode) }
    }
}
