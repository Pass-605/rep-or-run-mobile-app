package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.reporrun.core.designsystem.RepOrRunDimens

/** Un paso de [RepOrRunStepper]: su nombre y, debajo, un dato opcional (la hora, «en curso»). */
@Immutable
data class RepOrRunStep(val label: String, val caption: String? = null)

internal enum class StepState { Done, Current, Pending }

internal fun stepState(index: Int, currentStep: Int): StepState = when {
    index < currentStep -> StepState.Done
    index == currentStep -> StepState.Current
    else -> StepState.Pending
}

/** Centro en X del nodo [index] de [count], con los extremos metidos [inset] hacia dentro. */
internal fun stepCenter(index: Int, count: Int, width: Float, inset: Float): Float =
    inset + (width - 2 * inset) * index / (count - 1)

/**
 * Stepper horizontal: nodos unidos por una línea, con el tramo recorrido en el bloque de
 * énfasis. El nodo de [currentStep] es el único sitio donde aparece el acento, que aquí sí
 * marca «vas por aquí». Los pasos anteriores a [currentStep] están hechos; con
 * `currentStep = steps.size` lo están todos.
 *
 * El diseño lo usa con tres pasos y dentro de una tarjeta: sobre el fondo de la pantalla la
 * línea pendiente no se vería.
 */
@Composable
fun RepOrRunStepper(steps: List<RepOrRunStep>, currentStep: Int, modifier: Modifier = Modifier) {
    require(steps.size >= 2) { "Un stepper necesita al menos dos pasos" }
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val reachedIndex = currentStep.coerceIn(0, steps.lastIndex)
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = reachedIndex.toFloat(),
                    range = 0f..steps.lastIndex.toFloat(),
                    steps = steps.lastIndex - 1,
                )
            }
            .stepperTrack(steps.size, reachedIndex, pending = colors.background, reached = colors.inverseSurface),
        content = {
            steps.forEachIndexed { index, step ->
                val state = stepState(index, currentStep)
                StepNode(state)
                Text(
                    step.label,
                    style = typography.labelSmall,
                    color = if (state == StepState.Pending) colors.onSurfaceVariant else colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    step.caption.orEmpty(),
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        measurePolicy = stepperMeasurePolicy(steps.size),
    )
}

/** La línea que une los nodos: entera en [pending] y, encima, hasta [reachedIndex] en [reached]. */
private fun Modifier.stepperTrack(count: Int, reachedIndex: Int, pending: Color, reached: Color) = drawBehind {
    val inset = RepOrRunDimens.StepperNodeInset.toPx()
    val y = RepOrRunDimens.StepperNodeSize.toPx() / 2
    val stroke = RepOrRunDimens.StepperTrackThickness.toPx()
    val start = Offset(stepCenter(0, count, size.width, inset), y)
    val end = Offset(stepCenter(count - 1, count, size.width, inset), y)
    drawLine(pending, start, end, stroke, StrokeCap.Round)
    drawLine(reached, start, Offset(stepCenter(reachedIndex, count, size.width, inset), y), stroke, StrokeCap.Round)
}

/** Cada paso aporta tres hijos, en este orden: nodo, nombre y dato. */
private const val CHILDREN_PER_STEP = 3

private fun stepperMeasurePolicy(count: Int) = MeasurePolicy { measurables, constraints ->
    val width = constraints.maxWidth
    val textConstraints = constraints.copy(minWidth = 0, minHeight = 0, maxWidth = width / count)
    val nodes = List(count) { measurables[it * CHILDREN_PER_STEP].measure(constraints.copy(minWidth = 0)) }
    val labels = List(count) { measurables[it * CHILDREN_PER_STEP + 1].measure(textConstraints) }
    val captions = List(count) { measurables[it * CHILDREN_PER_STEP + 2].measure(textConstraints) }

    val labelTop = nodes.maxOf { it.height } + RepOrRunDimens.StepperLabelSpacing.roundToPx()
    val captionTop = labelTop + labels.maxOf { it.height }
    val inset = RepOrRunDimens.StepperNodeInset.toPx()

    layout(width, captionTop + captions.maxOf { it.height }) {
        repeat(count) { index ->
            val center = stepCenter(index, count, width.toFloat(), inset)

            // Centrado bajo su nodo, sin salirse por los lados: en los extremos queda al borde.
            fun centered(itemWidth: Int) =
                (center - itemWidth / 2f).toInt().coerceIn(0, (width - itemWidth).coerceAtLeast(0))
            nodes[index].place(centered(nodes[index].width), 0)
            labels[index].place(centered(labels[index].width), labelTop)
            captions[index].place(centered(captions[index].width), captionTop)
        }
    }
}

@Composable
private fun StepNode(state: StepState) {
    val colors = MaterialTheme.colorScheme
    val current = state == StepState.Current
    Box(
        modifier = Modifier
            .size(RepOrRunDimens.StepperNodeSize)
            .background(if (state == StepState.Pending) colors.background else colors.inverseSurface, CircleShape)
            .then(
                if (current) {
                    Modifier.border(RepOrRunDimens.StepperNodeBorder, colors.primary, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(RepOrRunDimens.StepperNodeDotSize)
                .background(if (current) colors.primary else colors.surface, CircleShape),
        )
    }
}
