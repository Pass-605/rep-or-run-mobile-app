package app.reporrun.feature.plansetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.designsystem.component.RepOrRunScreenScaffold
import app.reporrun.core.designsystem.component.RepOrRunSectionError
import app.reporrun.core.designsystem.component.RepOrRunStatCard
import app.reporrun.core.domain.usecase.GenerationSummary
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.plansetup.resources.Res
import app.reporrun.feature.plansetup.resources.summary_back
import app.reporrun.feature.plansetup.resources.summary_generate
import app.reporrun.feature.plansetup.resources.summary_gym
import app.reporrun.feature.plansetup.resources.summary_gym_missing
import app.reporrun.feature.plansetup.resources.summary_headline_blocked
import app.reporrun.feature.plansetup.resources.summary_headline_ready
import app.reporrun.feature.plansetup.resources.summary_profile
import app.reporrun.feature.plansetup.resources.summary_profile_missing
import app.reporrun.feature.plansetup.resources.summary_sent_body
import app.reporrun.feature.plansetup.resources.summary_sent_title
import app.reporrun.feature.plansetup.resources.summary_stat_days
import app.reporrun.feature.plansetup.resources.summary_stat_empty
import app.reporrun.feature.plansetup.resources.summary_stat_machines
import app.reporrun.feature.plansetup.resources.summary_stat_minutes
import app.reporrun.feature.plansetup.resources.summary_stat_minutes_unit
import app.reporrun.feature.plansetup.resources.summary_title
import app.reporrun.feature.plansetup.resources.summary_week
import app.reporrun.feature.plansetup.resources.summary_week_missing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** El resumen, con estado. */
@Composable
internal fun SummaryRoute(viewModel: SummaryViewModel, actions: SummaryActions, onGenerate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SummaryEffect.GenerationRequested -> onGenerate()
        }
    }

    SummaryScreen(state = state, onEvent = viewModel::onEvent, actions = actions)
}

/**
 * La pantalla 05: lo que se va a enviar, antes de gastar una generación. Tres cifras, una fila
 * por cada cosa que se puede corregir y qué sale del dispositivo.
 *
 * Si falta algo, la acción del pie se deshabilita y **el motivo va arriba**, bajo el titular: al
 * pie quedaría fuera de la vista en cuanto el contenido hiciera scroll. Qué falta lo dice el
 * resumen; la pantalla no compara ningún número.
 *
 * No lleva el overline «Paso 4 de 4» de Figma: ninguna pantalla lo lleva y el recorrido real no
 * tiene cuatro pasos.
 */
@Composable
internal fun SummaryScreen(
    state: SummaryUiState,
    onEvent: (SummaryEvent) -> Unit,
    actions: SummaryActions,
    modifier: Modifier = Modifier,
) {
    RepOrRunScreenScaffold(
        title = stringResource(Res.string.summary_title),
        backContentDescription = stringResource(Res.string.summary_back),
        actionText = stringResource(Res.string.summary_generate),
        actionEnabled = state.canGenerate,
        onAction = { onEvent(SummaryEvent.GenerateClicked) },
        modifier = modifier,
        onBack = actions.onBack,
    ) {
        // Hasta que el almacenamiento contesta no se pinta nada: ni el resumen ni lo que falta.
        val summary = state.summary ?: return@RepOrRunScreenScaffold

        Headline(summary)
        RepOrRunSectionError(summary.blockers.reason())
        Stats(summary)
        EditRows(summary, actions)
        WhatIsSent()
    }
}

@Composable
private fun Headline(summary: GenerationSummary) {
    // Sin preferencias no se llega a «todo listo», pero el número tiene que salir de algún sitio.
    val blockDays = summary.preferences?.blockDays ?: BlockPreferences.BLOCK_DAYS_V1
    Text(
        if (summary.canGenerate) {
            pluralStringResource(Res.plurals.summary_headline_ready, blockDays, blockDays)
        } else {
            stringResource(Res.string.summary_headline_blocked)
        },
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
    )
}

/**
 * Días por semana, máquinas y minutos por sesión. Los minutos van en acento: vienen con un valor
 * por defecto, y esta es la pantalla donde se ve antes de enviarlo.
 */
@Composable
private fun Stats(summary: GenerationSummary) {
    val empty = stringResource(Res.string.summary_stat_empty)
    val days = summary.preferences?.availableDays?.size
    val machines = summary.equipment.size.takeIf { summary.gym != null }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
    ) {
        RepOrRunStatCard(
            value = days?.toString() ?: empty,
            label = pluralStringResource(Res.plurals.summary_stat_days, days ?: 0),
            modifier = Modifier.weight(1f),
        )
        RepOrRunStatCard(
            value = machines?.toString() ?: empty,
            label = pluralStringResource(Res.plurals.summary_stat_machines, machines ?: 0),
            modifier = Modifier.weight(1f),
        )
        RepOrRunStatCard(
            value = summary.preferences?.sessionMinutes?.toString() ?: empty,
            label = stringResource(Res.string.summary_stat_minutes),
            modifier = Modifier.weight(1f),
            unit = stringResource(Res.string.summary_stat_minutes_unit),
            highlighted = true,
        )
    }
}

/** Perfil, gimnasio y semana: cada fila dice lo que hay guardado, o que falta, y lo abre. */
@Composable
private fun EditRows(summary: GenerationSummary, actions: SummaryActions) {
    Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.CardListSpacing)) {
        EditRow(
            title = stringResource(Res.string.summary_profile),
            subtitle = summary.profile?.summaryLine() ?: stringResource(Res.string.summary_profile_missing),
            icon = RepOrRunIcons.Person,
            onClick = actions.onEditProfile,
        )
        EditRow(
            title = summary.gym?.name ?: stringResource(Res.string.summary_gym),
            subtitle = if (summary.gym == null) {
                stringResource(Res.string.summary_gym_missing)
            } else {
                summary.equipment.summaryLine()
            },
            icon = RepOrRunIcons.Dumbbell,
            onClick = { actions.onEditGym(summary.gym?.id) },
        )
        EditRow(
            title = stringResource(Res.string.summary_week),
            subtitle = summary.preferences?.summaryLine() ?: stringResource(Res.string.summary_week_missing),
            icon = RepOrRunIcons.Calendar,
            onClick = actions.onEditPreferences,
        )
    }
}

/** Una fila en su propia tarjeta. Pulsa la tarjeta entera, no solo el texto. */
@Composable
private fun EditRow(title: String, subtitle: String, icon: Painter, onClick: () -> Unit) {
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = RepOrRunDimens.CardHorizontalPadding),
    ) {
        RepOrRunListRow(title = title, subtitle = subtitle, leadingIcon = icon)
    }
}

/** El overline va en acento y dentro de la tarjeta: sobre el lienzo no cumple (Anexo A). */
@Composable
private fun WhatIsSent() {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.summary_sent_title).uppercase(),
            style = RepOrRunTheme.extendedTypography.overline,
            color = RepOrRunTheme.extendedColors.accentText,
        )
        Text(
            stringResource(Res.string.summary_sent_body),
            modifier = Modifier.padding(top = RepOrRunDimens.FieldLabelSpacing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
