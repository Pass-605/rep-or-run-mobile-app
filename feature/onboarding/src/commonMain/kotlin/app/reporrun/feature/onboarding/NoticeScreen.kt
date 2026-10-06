package app.reporrun.feature.onboarding

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
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.feature.onboarding.resources.Res
import app.reporrun.feature.onboarding.resources.notice_back
import app.reporrun.feature.onboarding.resources.notice_consent_body
import app.reporrun.feature.onboarding.resources.notice_consent_title
import app.reporrun.feature.onboarding.resources.notice_data_sent_body
import app.reporrun.feature.onboarding.resources.notice_data_sent_title
import app.reporrun.feature.onboarding.resources.notice_intro
import app.reporrun.feature.onboarding.resources.notice_local_data_body
import app.reporrun.feature.onboarding.resources.notice_local_data_title
import app.reporrun.feature.onboarding.resources.notice_not_medical_body
import app.reporrun.feature.onboarding.resources.notice_not_medical_title
import app.reporrun.feature.onboarding.resources.notice_recipient_body
import app.reporrun.feature.onboarding.resources.notice_recipient_title
import app.reporrun.feature.onboarding.resources.notice_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * El aviso completo. No tiene ViewModel porque no tiene nada que observar ni que guardar: es
 * texto fijo. Leerlo no acepta nada; se acepta en la bienvenida, y solo allí.
 *
 * No está dibujado en Figma: usa los componentes y los roles del tema, y a diferencia de la
 * bienvenida **sí** sigue el tema de la app. Se llega desde la bienvenida y, con el
 * consentimiento ya dado, desde fuera del feature (`navigateToNotice`).
 */
@Composable
internal fun NoticeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
    ) {
        RepOrRunIconButton(
            icon = RepOrRunIcons.ArrowBack,
            contentDescription = stringResource(Res.string.notice_back),
            onClick = onBack,
        )
        Text(stringResource(Res.string.notice_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.notice_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NoticeSection(Res.string.notice_not_medical_title, Res.string.notice_not_medical_body)
        NoticeSection(Res.string.notice_data_sent_title, Res.string.notice_data_sent_body)
        NoticeSection(Res.string.notice_recipient_title, Res.string.notice_recipient_body)
        NoticeSection(Res.string.notice_local_data_title, Res.string.notice_local_data_body)
        NoticeSection(Res.string.notice_consent_title, Res.string.notice_consent_body)
    }
}

@Composable
private fun NoticeSection(title: StringResource, body: StringResource) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.FieldLabelSpacing)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
