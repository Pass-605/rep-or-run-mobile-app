package app.reporrun.feature.profile

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunScreenScaffold
import app.reporrun.core.designsystem.component.RepOrRunStep
import app.reporrun.core.designsystem.component.RepOrRunStepper
import app.reporrun.feature.profile.resources.Res
import app.reporrun.feature.profile.resources.profile_back
import app.reporrun.feature.profile.resources.profile_step_basics
import app.reporrun.feature.profile.resources.profile_step_goal
import app.reporrun.feature.profile.resources.profile_title
import org.jetbrains.compose.resources.stringResource

/* Lo que comparten los dos pasos. Todo se compone con piezas de `:core:designsystem`. */

internal const val STEP_BASICS = 0
internal const val STEP_GOAL = 1

/**
 * El armazón de un paso: el de `:core:designsystem` con el título del perfil y, bajo la barra,
 * el stepper que dice en cuál de los dos pasos se está.
 *
 * @param onBack `null` en el paso 1 del alta: es el primer destino y no hay a dónde volver.
 */
@Composable
internal fun ProfileStepScaffold(
    step: Int,
    actionText: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: Painter? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    RepOrRunScreenScaffold(
        title = stringResource(Res.string.profile_title),
        backContentDescription = stringResource(Res.string.profile_back),
        actionText = actionText,
        actionEnabled = actionEnabled,
        onAction = onAction,
        modifier = modifier,
        actionIcon = actionIcon,
        onBack = onBack,
    ) {
        RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
            RepOrRunStepper(
                steps = listOf(
                    RepOrRunStep(stringResource(Res.string.profile_step_basics)),
                    RepOrRunStep(stringResource(Res.string.profile_step_goal)),
                ),
                currentStep = step,
            )
        }
        content()
    }
}
