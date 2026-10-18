package app.reporrun.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunButtonStyle
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.designsystem.component.RepOrRunEmphasisCard
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunScreenFrame
import app.reporrun.core.designsystem.component.RepOrRunScrollContent
import app.reporrun.core.designsystem.component.RepOrRunSectionTitle
import app.reporrun.core.designsystem.component.RepOrRunStatusChip
import app.reporrun.core.designsystem.component.RepOrRunSupportingText
import app.reporrun.core.designsystem.component.RepOrRunTopBar
import app.reporrun.core.domain.usecase.PlanDetails
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanStatus
import app.reporrun.feature.plan.resources.Res
import app.reporrun.feature.plan.resources.plan_activate
import app.reporrun.feature.plan.resources.plan_approve_day
import app.reporrun.feature.plan.resources.plan_back
import app.reporrun.feature.plan.resources.plan_days
import app.reporrun.feature.plan.resources.plan_days_approved
import app.reporrun.feature.plan.resources.plan_empty_action
import app.reporrun.feature.plan.resources.plan_empty_body
import app.reporrun.feature.plan.resources.plan_empty_title
import app.reporrun.feature.plan.resources.plan_exercise_trial
import app.reporrun.feature.plan.resources.plan_exercise_unilateral
import app.reporrun.feature.plan.resources.plan_exercises
import app.reporrun.feature.plan.resources.plan_notes
import app.reporrun.feature.plan.resources.plan_open_settings
import app.reporrun.feature.plan.resources.plan_prepare_another
import app.reporrun.feature.plan.resources.plan_session
import app.reporrun.feature.plan.resources.plan_session_cardio
import app.reporrun.feature.plan.resources.plan_session_cooldown
import app.reporrun.feature.plan.resources.plan_session_mobility
import app.reporrun.feature.plan.resources.plan_session_priority
import app.reporrun.feature.plan.resources.plan_session_warmup
import app.reporrun.feature.plan.resources.plan_title
import org.jetbrains.compose.resources.stringResource

/** La vista del plan, con estado. */
@Composable
internal fun PlanRoute(
    viewModel: PlanViewModel,
    onBack: (() -> Unit)?,
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PlanScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenSummary = onOpenSummary,
        onOpenSettings = onOpenSettings,
    )
}

/**
 * El plan por día, sobre las pantallas 07 y 07b de Figma: el bloque de énfasis con la sesión, la
 * tarjeta de detalles del día y los ejercicios numerados. De ellas **no** se copia el seguimiento
 * de la sesión («Empezar», el progreso, «Hecho / En curso»), el buscador ni la barra inferior: la
 * app no tiene nada de eso. Y lo que no dibujan —elegir día, aprobar, activar y el estado vacío—
 * se compone con los patrones del design system.
 *
 * El pie cambia con el plan y a veces no está, así que no cabe en `RepOrRunScreenScaffold`.
 *
 * @param onBack `null` cuando la vista es el destino inicial y no hay a dónde volver.
 * @param onOpenSummary ir al resumen previo a generar. Es la salida cuando no hay atrás.
 * @param onOpenSettings abrir Ajustes. Está siempre, también mientras carga y sin plan.
 */
@Composable
internal fun PlanScreen(
    state: PlanUiState,
    onEvent: (PlanEvent) -> Unit,
    onBack: (() -> Unit)?,
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = state.details
    val day = state.selectedDay
    RepOrRunScreenFrame(
        modifier = modifier,
        footer = footerOf(state, onEvent, onOpenSummary),
    ) {
        RepOrRunScrollContent {
            RepOrRunTopBar(
                title = stringResource(Res.string.plan_title),
                backContentDescription = stringResource(Res.string.plan_back),
                onBack = onBack,
                trailing = {
                    RepOrRunIconButton(
                        icon = RepOrRunIcons.Settings,
                        contentDescription = stringResource(Res.string.plan_open_settings),
                        onClick = onOpenSettings,
                    )
                },
            )
            when {
                // Hasta que Room contesta no se pinta nada, para que no parpadee el estado vacío.
                state.isLoading -> Unit

                details == null || day == null -> NoActivePlan()

                else -> {
                    PlanContent(details, day, state, onEvent)
                    // Tras generar, atrás ya lleva al resumen; como destino inicial no hay otra salida.
                    if (onBack == null) {
                        RepOrRunButton(
                            text = stringResource(Res.string.plan_prepare_another),
                            onClick = onOpenSummary,
                            modifier = Modifier.fillMaxWidth(),
                            style = RepOrRunButtonStyle.Outline,
                        )
                    }
                }
            }
        }
    }
}

/** El pie de cada estado, o `null` si no lleva: mientras carga y con el plan ya activo. */
@Composable
private fun footerOf(
    state: PlanUiState,
    onEvent: (PlanEvent) -> Unit,
    onOpenSummary: () -> Unit,
): (@Composable () -> Unit)? {
    val action = state.action
    return when {
        state.isLoading -> null

        state.plan == null -> {
            {
                RepOrRunButton(
                    text = stringResource(Res.string.plan_empty_action),
                    onClick = onOpenSummary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        action == null -> null

        else -> {
            { PlanFooter(action, onEvent) }
        }
    }
}

@Composable
private fun PlanFooter(action: PlanAction, onEvent: (PlanEvent) -> Unit) {
    when (action) {
        PlanAction.ApproveDay -> RepOrRunButton(
            text = stringResource(Res.string.plan_approve_day),
            onClick = { onEvent(PlanEvent.ApproveDayClicked) },
            modifier = Modifier.fillMaxWidth(),
            icon = RepOrRunIcons.Check,
        )

        is PlanAction.ActivatePlan -> Column(
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        ) {
            // El motivo va encima y en texto normal: el botón deshabilitado no llega a contraste.
            action.pendingHint()?.let { hint ->
                RepOrRunSupportingText(hint, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            RepOrRunButton(
                text = stringResource(Res.string.plan_activate),
                onClick = { onEvent(PlanEvent.ActivatePlanClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = action.enabled,
            )
        }
    }
}

@Composable
private fun ColumnScope.NoActivePlan() {
    Text(
        stringResource(Res.string.plan_empty_title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        stringResource(Res.string.plan_empty_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ColumnScope.PlanContent(
    details: PlanDetails,
    day: PlanDay,
    state: PlanUiState,
    onEvent: (PlanEvent) -> Unit,
) {
    PlanHeader(details.plan)
    DaySelector(state.days, day, onEvent)
    DayHero(day, state.today)
    SessionCard(day)
    RepOrRunSectionTitle(stringResource(Res.string.plan_exercises), trailing = day.exercises.size.toString())
    Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing)) {
        day.exercises.sortedBy { it.order }.forEach { exercise ->
            ExerciseCard(exercise, machine = details.equipmentName(exercise.equipmentId))
        }
    }
    if (details.plan.notes.isNotEmpty()) NotesCard(details.plan.notes)
}
