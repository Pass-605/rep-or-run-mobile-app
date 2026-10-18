// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.settings

import app.reporrun.core.model.AiBackend
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
 * @property aiBackend de dónde sale el plan, según lo guardado; `null` hasta que DataStore
 *   contesta. Solo se enseña en una build de depuración.
 * @property sampleGym en qué punto está la carga del gimnasio de ejemplo (depuración).
 */
data class SettingsUiState(
    val themeMode: ThemeMode? = null,
    val profile: ProfileSummary? = null,
    val consentAcceptedOn: LocalDate? = null,
    val aiBackend: AiBackend? = null,
    val sampleGym: SampleGymLoad = SampleGymLoad.NOT_LOADED,
)

/** La carga del gimnasio de ejemplo desde Ajustes, en esta visita. */
enum class SampleGymLoad { NOT_LOADED, LOADING, LOADED }

/** Lo que se enseña del perfil en su fila: no es el perfil, solo su resumen. */
data class ProfileSummary(val age: Int, val experience: Experience)

sealed interface SettingsEvent {
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent

    data object ProfileClicked : SettingsEvent

    data object NoticeClicked : SettingsEvent

    data object LicensesClicked : SettingsEvent

    /** Depuración: generar con el proxy ([useProxy]) o con los ejemplos sin red. */
    data class ProxyToggled(val useProxy: Boolean) : SettingsEvent

    /** Depuración: cargar el gimnasio de ejemplo del contrato. */
    data object SampleGymClicked : SettingsEvent
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
