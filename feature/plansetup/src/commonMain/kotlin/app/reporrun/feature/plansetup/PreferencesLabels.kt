package app.reporrun.feature.plansetup

import androidx.compose.runtime.Composable
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.model.DayOfWeek
import app.reporrun.feature.plansetup.resources.Res
import app.reporrun.feature.plansetup.resources.prefs_day_friday
import app.reporrun.feature.plansetup.resources.prefs_day_monday
import app.reporrun.feature.plansetup.resources.prefs_day_saturday
import app.reporrun.feature.plansetup.resources.prefs_day_sunday
import app.reporrun.feature.plansetup.resources.prefs_day_thursday
import app.reporrun.feature.plansetup.resources.prefs_day_tuesday
import app.reporrun.feature.plansetup.resources.prefs_day_wednesday
import app.reporrun.feature.plansetup.resources.prefs_error_out_of_range
import app.reporrun.feature.plansetup.resources.prefs_error_required
import app.reporrun.feature.plansetup.resources.prefs_error_too_long
import app.reporrun.feature.plansetup.resources.prefs_error_too_many
import app.reporrun.feature.plansetup.resources.prefs_error_too_short
import app.reporrun.feature.plansetup.resources.prefs_start_date_option
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Del dominio a los textos. El `when` de los días es exhaustivo sin `else`: un valor nuevo en el
 * enum del contrato no compila hasta que tiene su texto.
 */

internal val DayOfWeek.shortLabel: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.prefs_day_monday
        DayOfWeek.TUESDAY -> Res.string.prefs_day_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.prefs_day_wednesday
        DayOfWeek.THURSDAY -> Res.string.prefs_day_thursday
        DayOfWeek.FRIDAY -> Res.string.prefs_day_friday
        DayOfWeek.SATURDAY -> Res.string.prefs_day_saturday
        DayOfWeek.SUNDAY -> Res.string.prefs_day_sunday
    }

/** Una fecha de inicio como se lee en su chip: «Jue 8/10». */
@Composable
internal fun LocalDate.optionLabel(): String {
    // Los dos enums empiezan en lunes: el de kotlinx-datetime y el del contrato.
    val weekday = DayOfWeek.entries[dayOfWeek.ordinal]
    return stringResource(Res.string.prefs_start_date_option, stringResource(weekday.shortLabel), day, month.number)
}

/** La frase de un error. El límite viene del caso de uso; aquí no hay ningún número. */
@Composable
internal fun FieldIssue.message(): String = when (this) {
    FieldIssue.Required -> stringResource(Res.string.prefs_error_required)

    // Los únicos rangos de esta pantalla son de enteros.
    is FieldIssue.OutOfRange -> stringResource(Res.string.prefs_error_out_of_range, min.toInt(), max.toInt())

    is FieldIssue.TooShort -> stringResource(Res.string.prefs_error_too_short, minLength)

    is FieldIssue.TooLong -> stringResource(Res.string.prefs_error_too_long, maxLength)

    is FieldIssue.TooMany -> stringResource(Res.string.prefs_error_too_many, max)
}
