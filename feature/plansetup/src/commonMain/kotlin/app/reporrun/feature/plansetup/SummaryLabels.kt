package app.reporrun.feature.plansetup

import androidx.compose.runtime.Composable
import app.reporrun.core.domain.usecase.GenerationBlocker
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.UserProfile
import app.reporrun.feature.plansetup.resources.Res
import app.reporrun.feature.plansetup.resources.summary_blocker_consent
import app.reporrun.feature.plansetup.resources.summary_blocker_equipment
import app.reporrun.feature.plansetup.resources.summary_blocker_gym
import app.reporrun.feature.plansetup.resources.summary_blocker_line
import app.reporrun.feature.plansetup.resources.summary_blocker_preferences
import app.reporrun.feature.plansetup.resources.summary_blocker_profile
import app.reporrun.feature.plansetup.resources.summary_blockers_title
import app.reporrun.feature.plansetup.resources.summary_experience_advanced
import app.reporrun.feature.plansetup.resources.summary_experience_beginner
import app.reporrun.feature.plansetup.resources.summary_experience_intermediate
import app.reporrun.feature.plansetup.resources.summary_goal_general_fitness
import app.reporrun.feature.plansetup.resources.summary_goal_hypertrophy
import app.reporrun.feature.plansetup.resources.summary_goal_recomposition
import app.reporrun.feature.plansetup.resources.summary_goal_strength
import app.reporrun.feature.plansetup.resources.summary_gym_categories
import app.reporrun.feature.plansetup.resources.summary_gym_empty
import app.reporrun.feature.plansetup.resources.summary_gym_machines
import app.reporrun.feature.plansetup.resources.summary_gym_value
import app.reporrun.feature.plansetup.resources.summary_profile_age
import app.reporrun.feature.plansetup.resources.summary_profile_value
import app.reporrun.feature.plansetup.resources.summary_week_from
import app.reporrun.feature.plansetup.resources.summary_week_no_start
import app.reporrun.feature.plansetup.resources.summary_week_separator
import kotlinx.datetime.number
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Del resumen a sus textos. Los `when` son exhaustivos sin `else`: un valor nuevo en un enum
 * del contrato, o un bloqueo nuevo en el caso de uso, no compila hasta que tiene su texto.
 *
 * La experiencia y el objetivo ya tienen texto en `feature:profile`, en mayúscula y para un
 * chip; un feature no puede usar otro (CLAUDE.md §3), así que aquí van los suyos, en minúscula
 * y para ir dentro de una frase.
 */

private val Experience.summaryLabel: StringResource
    get() = when (this) {
        Experience.BEGINNER -> Res.string.summary_experience_beginner
        Experience.INTERMEDIATE -> Res.string.summary_experience_intermediate
        Experience.ADVANCED -> Res.string.summary_experience_advanced
    }

private val Goal.summaryLabel: StringResource
    get() = when (this) {
        Goal.RECOMPOSITION -> Res.string.summary_goal_recomposition
        Goal.HYPERTROPHY -> Res.string.summary_goal_hypertrophy
        Goal.STRENGTH -> Res.string.summary_goal_strength
        Goal.GENERAL_FITNESS -> Res.string.summary_goal_general_fitness
    }

/** «30 años · intermedio · recomposición». */
@Composable
internal fun UserProfile.summaryLine(): String = stringResource(
    Res.string.summary_profile_value,
    pluralStringResource(Res.plurals.summary_profile_age, age, age),
    stringResource(experience.summaryLabel),
    stringResource(goal.summaryLabel),
)

/** «18 máquinas · 4 categorías», o que el gimnasio aún no tiene ninguna. */
@Composable
internal fun List<Equipment>.summaryLine(): String {
    if (isEmpty()) return stringResource(Res.string.summary_gym_empty)
    val categories = distinctBy { it.category }.size
    return stringResource(
        Res.string.summary_gym_value,
        pluralStringResource(Res.plurals.summary_gym_machines, size, size),
        pluralStringResource(Res.plurals.summary_gym_categories, categories, categories),
    )
}

/** «L · M · J · S desde el 21/09»: los días de lunes a domingo, y la fecha de inicio si la hay. */
@Composable
internal fun BlockPreferences.summaryLine(): String {
    val separator = stringResource(Res.string.summary_week_separator)
    val days = availableDays.sorted().map { stringResource(it.initial) }.joinToString(separator)
    val start = startDate ?: return stringResource(Res.string.summary_week_no_start, days)
    val date = "${start.day.toString().padStart(2, '0')}/${start.month.number.toString().padStart(2, '0')}"
    return stringResource(Res.string.summary_week_from, days, date)
}

@Composable
private fun GenerationBlocker.label(): String = when (this) {
    GenerationBlocker.MissingProfile -> stringResource(Res.string.summary_blocker_profile)
    GenerationBlocker.MissingActiveGym -> stringResource(Res.string.summary_blocker_gym)
    is GenerationBlocker.NotEnoughEquipment -> stringResource(Res.string.summary_blocker_equipment, current, required)
    GenerationBlocker.MissingPreferences -> stringResource(Res.string.summary_blocker_preferences)
    GenerationBlocker.MissingConsent -> stringResource(Res.string.summary_blocker_consent)
}

/**
 * El motivo de que no se pueda generar: una cabecera y **una línea por cosa que falta**, en el
 * orden en que las da el caso de uso (perfil, gimnasio, preferencias, consentimiento), que es
 * también el del recorrido. Se listan todas, sean una o cinco: así no hay que arreglar una para
 * descubrir la siguiente. `null` si no falta nada.
 */
@Composable
internal fun List<GenerationBlocker>.reason(): String? {
    if (isEmpty()) return null
    val lines = map { stringResource(Res.string.summary_blocker_line, it.label()) }
    return (listOf(stringResource(Res.string.summary_blockers_title)) + lines).joinToString("\n")
}
