// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.plansetup

import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.PreferencesField
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * El formulario de las preferencias del bloque.
 *
 * - [sessionMinutes] va como **texto**, tal como está en el campo. Convertirlo y decidir si vale
 *   es cosa del ViewModel y del caso de uso, no del estado.
 * - [blockDays] no se edita: v1 solo admite [BlockPreferences.BLOCK_DAYS_V1] (plan §4). Está en
 *   el estado para que la pantalla lo enseñe sin escribir el número.
 * - [startDate] es `null` mientras la persona no elija una: la fecha de inicio es opcional.
 *   [startDateOptions] son las fechas entre las que se elige, de hoy en adelante.
 * - [cardioOptions] sale del inventario del gimnasio activo; [cardioModes] son las elegidas.
 * - [errors] lleva el motivo que dio el caso de uso, con su límite. La frase la pone la
 *   pantalla; aquí no hay textos.
 * - [isLoading] empieza en `true`: hasta saber si hay preferencias guardadas no se pinta el
 *   formulario, para que no aparezca vacío y se rellene de golpe.
 */
data class PreferencesUiState(
    val isLoading: Boolean = true,
    val availableDays: Set<DayOfWeek> = emptySet(),
    val sessionMinutes: String = "",
    val blockDays: Int = BlockPreferences.BLOCK_DAYS_V1,
    val startDate: LocalDate? = null,
    val startDateOptions: List<LocalDate> = emptyList(),
    val cardioOptions: List<String> = emptyList(),
    val cardioModes: Set<String> = emptySet(),
    val avoidExercises: String = "",
    val errors: Map<PreferencesField, FieldIssue> = emptyMap(),
    val isSaving: Boolean = false,
)

sealed interface PreferencesEvent {
    data class DayToggled(val day: DayOfWeek) : PreferencesEvent

    data class SessionMinutesChanged(val value: String) : PreferencesEvent

    /** [date] es `null` para «sin fecha». */
    data class StartDateSelected(val date: LocalDate?) : PreferencesEvent

    data class CardioModeToggled(val mode: String) : PreferencesEvent

    data class AvoidExercisesChanged(val value: String) : PreferencesEvent

    data object SaveClicked : PreferencesEvent
}

sealed interface PreferencesEffect {
    /** Las preferencias ya están guardadas. */
    data object PreferencesSaved : PreferencesEffect
}
