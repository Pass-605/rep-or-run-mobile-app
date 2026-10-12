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
import app.reporrun.feature.plan.resources.plan_prepare_another
import app.reporrun.feature.plan.resources.plan_session
import app.reporrun.feature.plan.resources.plan_session_cardio
import app.reporrun.feature.plan.resources.plan_session_cooldown
import app.reporrun.feature.plan.resources.plan_session_mobility
import app.reporrun.feature.plan.resources.plan_session_priority
import app.reporrun.feature.plan.resources.plan_session_warmup
import app.reporrun.feature.plan.resources.plan_title
import org.jetbrains.compose.resources.stringResource

/*
 * Las piezas de la vista del plan, de arriba abajo. Cada una dice de qué pantalla de Figma sale.
 */

/** La tarjeta de cabecera de la 07b: icono, línea secundaria, título y el estado en un chip. */
@Composable
internal fun PlanHeader(plan: Plan) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(RepOrRunDimens.IconButtonSize)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = RepOrRunIcons.Dumbbell,
                    contentDescription = null,
                    modifier = Modifier.size(RepOrRunDimens.IconButtonIconSize),
                    tint = RepOrRunTheme.extendedColors.iconPrimary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowTextSpacing),
            ) {
                Text(
                    plan.versionLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(plan.id.value, style = MaterialTheme.typography.titleMedium)
            }
            // El acento marca el estado en curso: el plan activo.
            RepOrRunStatusChip(stringResource(plan.status.label), highlighted = plan.status == PlanStatus.ACTIVE)
        }
        plan.focusLine()?.let { focus ->
            Text(
                focus,
                modifier = Modifier.padding(top = RepOrRunDimens.ListRowSpacing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Un chip por día con sesión, en una fila que hace scroll si no caben. El chip dice la
 * abreviatura; al lector de pantalla, el nombre entero y si está aprobado.
 */
@Composable
internal fun DaySelector(days: List<PlanDay>, selected: PlanDay, onEvent: (PlanEvent) -> Unit) {
    val approved = days.count { it.status == DayStatus.APPROVED }
    Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing)) {
        RepOrRunSectionTitle(
            stringResource(Res.string.plan_days),
            trailing = stringResource(Res.string.plan_days_approved, approved, days.size),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
        ) {
            days.forEach { day ->
                val description = day.chipDescription()
                RepOrRunChip(
                    label = stringResource(day.dayOfWeek.shortLabel),
                    selected = day.id == selected.id,
                    onClick = { onEvent(PlanEvent.DaySelected(day.id)) },
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
    }
}

/**
 * El bloque de énfasis de la 07. Todo el texto va en `inverseOnSurface`: Figma pone el overline
 * y las cifras en `#979797`, que no es color de texto (Anexo A).
 */
@Composable
internal fun DayHero(day: PlanDay, today: DayOfWeek) {
    RepOrRunEmphasisCard(modifier = Modifier.fillMaxWidth()) {
        Text(day.heading(today).uppercase(), style = RepOrRunTheme.extendedTypography.overline)
        Text(
            day.session,
            modifier = Modifier
                .padding(vertical = RepOrRunDimens.ListRowTextSpacing)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(day.countsLine(), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * «Detalles del día» de la 07b. La etiqueta va encima del valor y no a su izquierda: el
 * calentamiento y la movilidad son frases enteras.
 */
@Composable
internal fun SessionCard(day: PlanDay) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(bottom = RepOrRunDimens.ListRowSpacing),
            horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.plan_session),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RepOrRunStatusChip(stringResource(day.status.label))
        }
        SessionRow(stringResource(Res.string.plan_session_priority), day.priority)
        RepOrRunDivider()
        SessionRow(stringResource(Res.string.plan_session_cardio), day.cardio.line())
        RepOrRunDivider()
        SessionRow(stringResource(Res.string.plan_session_warmup), day.warmup)
        RepOrRunDivider()
        SessionRow(stringResource(Res.string.plan_session_mobility), day.mobility)
        RepOrRunDivider()
        SessionRow(stringResource(Res.string.plan_session_cooldown), day.cooldown)
    }
}

@Composable
private fun SessionRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = RepOrRunDimens.ListRowSpacing),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowTextSpacing),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * La tarjeta de ejercicio de la 07: el número de orden en un círculo, el nombre y la dosis. El
 * círculo es siempre neutro, porque aquí no hay ejercicios «hechos».
 *
 * Las alternativas van dentro, en una línea y sin número: son «elige una» en lugar de este
 * ejercicio, no ejercicios más, y no tienen series.
 *
 * @param machine el nombre del aparato, o `null` si ya no está en el inventario.
 */
@Composable
internal fun ExerciseCard(exercise: PlanExercise, machine: String?) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing)) {
            Box(
                modifier = Modifier
                    .size(RepOrRunDimens.ExerciseIndexSize)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(exercise.order.toString(), style = MaterialTheme.typography.labelMedium, color = secondary)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowTextSpacing),
            ) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                Text(exercise.doseLine(), style = MaterialTheme.typography.bodySmall, color = secondary)
                if (machine != null) Text(machine, style = MaterialTheme.typography.bodySmall, color = secondary)
                Text(
                    stringResource(Res.string.plan_exercise_trial, exercise.trial.text),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                )
            }
            if (exercise.unilateral) RepOrRunStatusChip(stringResource(Res.string.plan_exercise_unilateral))
        }
        exercise.alternativesLine()?.let { alternatives ->
            RepOrRunDivider(Modifier.padding(vertical = RepOrRunDimens.ListRowSpacing))
            Text(alternatives, style = MaterialTheme.typography.bodySmall, color = secondary)
        }
    }
}

/** Los avisos del bloque, tal como los trae el plan: cargas de ensayo, alternativas, series por lado. */
@Composable
internal fun NotesCard(notes: List<String>) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.plan_notes),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.padding(top = RepOrRunDimens.ListRowSpacing),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.FieldLabelSpacing),
        ) {
            notes.forEach { note -> Text(note, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
