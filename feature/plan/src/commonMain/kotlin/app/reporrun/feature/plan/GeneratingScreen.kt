package app.reporrun.feature.plan

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunButtonStyle
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.model.PlanId
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.plan.resources.Res
import app.reporrun.feature.plan.resources.generating_cancel
import app.reporrun.feature.plan.resources.generating_close
import app.reporrun.feature.plan.resources.generating_problem_line
import app.reporrun.feature.plan.resources.generating_problems_title
import app.reporrun.feature.plan.resources.generating_retry_in
import app.reporrun.feature.plan.resources.generating_subtitle
import app.reporrun.feature.plan.resources.generating_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * Cada cuánto cambia el mensaje de la espera. Lo bastante para leer una frase corta entera, y lo
 * bastante poco para que en una espera de pocos segundos se vea que cambia.
 */
private val WaitMessageInterval = 2.seconds

/** Generando, con estado. */
@Composable
internal fun GeneratingRoute(
    viewModel: GeneratingViewModel,
    onPlanGenerated: (PlanId) -> Unit,
    onBackToSummary: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is GeneratingEffect.PlanGenerated -> onPlanGenerated(effect.planId)
            GeneratingEffect.BackToSummary -> onBackToSummary()
        }
    }

    GeneratingScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * La pantalla 06: una espera a pantalla completa y, si no hay plan, por qué y qué se puede hacer.
 *
 * No usa `RepOrRunScreenScaffold`, que es la forma «barra, scroll y acción al pie»: aquí no hay
 * barra ni título arriba, solo cerrar. Lo que sí comparte es el pie fijo.
 *
 * De Figma no se copian el porcentaje ni la lista de tres pasos: la app no sabe cuánto falta ni
 * en qué paso va el servicio. El anillo es indeterminado y el mensaje describe la espera.
 */
@Composable
internal fun GeneratingScreen(
    state: GeneratingUiState,
    onEvent: (GeneratingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val failure = state.failure?.texts()
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = RepOrRunDimens.ScreenPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = RepOrRunDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RepOrRunIconButton(
                icon = RepOrRunIcons.Close,
                contentDescription = stringResource(
                    if (failure == null) Res.string.generating_cancel else Res.string.generating_close,
                ),
                onClick = { onEvent(GeneratingEvent.CloseClicked) },
                modifier = Modifier.align(Alignment.End),
            )
            Spacer(Modifier.height(RepOrRunDimens.WaitTopSpacing))
            if (failure == null) Waiting() else Failed(failure)
        }
        if (failure == null) {
            RepOrRunButton(
                text = stringResource(Res.string.generating_cancel),
                onClick = { onEvent(GeneratingEvent.CloseClicked) },
                modifier = Modifier.fillMaxWidth().padding(vertical = RepOrRunDimens.SectionSpacing),
                style = RepOrRunButtonStyle.Tonal,
            )
        } else {
            // Tras un límite de peticiones el pie espera lo que pidió el servicio, y lo cuenta.
            RepOrRunButton(
                text = if (state.canRetryNow) {
                    failure.action
                } else {
                    stringResource(Res.string.generating_retry_in, state.secondsUntilRetry)
                },
                onClick = { onEvent(failure.actionEvent) },
                modifier = Modifier.fillMaxWidth().padding(vertical = RepOrRunDimens.SectionSpacing),
                enabled = state.canRetryNow,
            )
        }
    }
}

/** El anillo sin porcentaje, el título y el mensaje que rota. */
@Composable
private fun ColumnScope.Waiting() {
    val colors = MaterialTheme.colorScheme
    // El progreso es uno de los sitios donde el acento sí marca.
    CircularProgressIndicator(
        modifier = Modifier.size(RepOrRunDimens.WaitRingSize),
        color = colors.primary,
        strokeWidth = RepOrRunDimens.WaitRingStroke,
        trackColor = colors.surface,
        strokeCap = StrokeCap.Butt,
    )
    Text(
        stringResource(Res.string.generating_title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(Res.string.generating_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    WaitMessage()
}

/**
 * El mensaje de ahora, con el punto en acento que en Figma marca el paso en curso. Cambia cada
 * [WaitMessageInterval] y vuelve a empezar: la espera real puede durar más que la lista.
 */
@Composable
private fun WaitMessage() {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(WaitMessageInterval)
            index = (index + 1) % WaitMessages.size
        }
    }
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(RepOrRunDimens.WaitDotSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Crossfade(
                targetState = index,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            ) { current ->
                Text(stringResource(WaitMessages[current]), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Sin plan: qué pasó y, si la validación dio problemas, todos, tal como vienen. El título va en
 * el color del texto principal: el acento se reserva para estado, no para alarmar.
 */
@Composable
private fun ColumnScope.Failed(failure: FailureTexts) {
    Text(
        failure.title,
        modifier = Modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        failure.body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (failure.problems.isNotEmpty()) Problems(failure.problems)
}

/** El overline va en acento y dentro de la tarjeta: sobre el lienzo no cumple (Anexo A). */
@Composable
private fun Problems(problems: List<String>) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.generating_problems_title).uppercase(),
            style = RepOrRunTheme.extendedTypography.overline,
            color = RepOrRunTheme.extendedColors.accentText,
        )
        Column(
            modifier = Modifier.padding(top = RepOrRunDimens.FieldLabelSpacing),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.FieldLabelSpacing),
        ) {
            problems.forEach { problem ->
                Text(
                    stringResource(Res.string.generating_problem_line, problem),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
