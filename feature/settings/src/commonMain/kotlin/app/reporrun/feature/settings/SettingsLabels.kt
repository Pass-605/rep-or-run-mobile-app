package app.reporrun.feature.settings

import androidx.compose.runtime.Composable
import app.reporrun.core.model.Experience
import app.reporrun.core.model.ThemeMode
import app.reporrun.feature.settings.resources.Res
import app.reporrun.feature.settings.resources.settings_experience_advanced
import app.reporrun.feature.settings.resources.settings_experience_beginner
import app.reporrun.feature.settings.resources.settings_experience_intermediate
import app.reporrun.feature.settings.resources.settings_notice_accepted
import app.reporrun.feature.settings.resources.settings_profile_summary
import app.reporrun.feature.settings.resources.settings_theme_dark
import app.reporrun.feature.settings.resources.settings_theme_light
import app.reporrun.feature.settings.resources.settings_theme_system
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/* Cómo se dice en la pantalla cada dato del estado. */

@Composable
internal fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.DARK -> Res.string.settings_theme_dark
        ThemeMode.LIGHT -> Res.string.settings_theme_light
    },
)

/** «30 años · intermedio». */
@Composable
internal fun ProfileSummary.label(): String = stringResource(
    Res.string.settings_profile_summary,
    age,
    stringResource(
        when (experience) {
            Experience.BEGINNER -> Res.string.settings_experience_beginner
            Experience.INTERMEDIATE -> Res.string.settings_experience_intermediate
            Experience.ADVANCED -> Res.string.settings_experience_advanced
        },
    ),
)

/** «Aceptado el 19/09/2026». */
@Composable
internal fun LocalDate.acceptedLabel(): String {
    val date = "${day.twoDigits()}/${month.number.twoDigits()}/$year"
    return stringResource(Res.string.settings_notice_accepted, date)
}

private fun Int.twoDigits() = toString().padStart(2, '0')
