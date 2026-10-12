package app.reporrun.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunSupportingText
import app.reporrun.core.designsystem.component.RepOrRunTopBar
import app.reporrun.core.model.PlanId
import app.reporrun.feature.plan.resources.Res
import app.reporrun.feature.plan.resources.plan_draft_back
import app.reporrun.feature.plan.resources.plan_draft_body
import app.reporrun.feature.plan.resources.plan_draft_headline
import app.reporrun.feature.plan.resources.plan_draft_title
import app.reporrun.feature.plan.resources.plan_draft_version
import org.jetbrains.compose.resources.stringResource

/**
 * A dónde llega un plan recién generado mientras no exista la vista del plan por día: dice que
 * el borrador está guardado y con qué versión. No lee el plan ni deja hacer nada con él.
 * **Lo sustituye C-12**, con sus cinco textos.
 */
@Composable
internal fun PlanDraftScreen(planId: PlanId, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
    ) {
        RepOrRunTopBar(
            title = stringResource(Res.string.plan_draft_title),
            backContentDescription = stringResource(Res.string.plan_draft_back),
            onBack = onBack,
        )
        Text(
            stringResource(Res.string.plan_draft_headline),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        // El overline va en acento y dentro de la tarjeta: sobre el lienzo no cumple (Anexo A).
        RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.plan_draft_version).uppercase(),
                style = RepOrRunTheme.extendedTypography.overline,
                color = RepOrRunTheme.extendedColors.accentText,
            )
            Text(
                planId.value,
                modifier = Modifier.padding(top = RepOrRunDimens.FieldLabelSpacing),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        RepOrRunSupportingText(stringResource(Res.string.plan_draft_body))
    }
}
