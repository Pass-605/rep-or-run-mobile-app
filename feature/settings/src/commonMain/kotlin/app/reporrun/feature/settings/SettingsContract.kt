// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.settings

import app.reporrun.core.model.Experience
import app.reporrun.core.model.ThemeMode
import kotlinx.datetime.LocalDate

/**
 * Ajustes.
 *
 * @property themeMode el tema guardado; `null` hasta que DataStore contesta, para no marcar uno
 *   que luego resulte no ser.
 * @property profile lo que la fila del perfil dice de él; `null` si no hay perfil guardado.
 * @property consentAcceptedOn el día local en que se aceptó el aviso; `null` si no consta.
 */
data class SettingsUiState(
    val themeMode: ThemeMode? = null,
    val profile: ProfileSummary? = null,
    val consentAcceptedOn: LocalDate? = null,
)

/** Lo que se enseña del perfil en su fila: no es el perfil, solo su resumen. */
data class ProfileSummary(val age: Int, val experience: Experience)

sealed interface SettingsEvent {
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent

    data object ProfileClicked : SettingsEvent

    data object NoticeClicked : SettingsEvent

    data object LicensesClicked : SettingsEvent
}

sealed interface SettingsEffect {
    data object ProfileRequested : SettingsEffect

    data object NoticeRequested : SettingsEffect

    data object LicensesRequested : SettingsEffect
}

/** Las salidas de Ajustes, juntas para que la pantalla no reciba una lambda por fila. */
internal data class SettingsActions(
    val onBack: () -> Unit,
    val onEditProfile: () -> Unit,
    val onReadNotice: () -> Unit,
    val onOpenLicenses: () -> Unit,
)
