package app.reporrun.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunButtonStyle
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunWatermark
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.onboarding.resources.Res
import app.reporrun.feature.onboarding.resources.welcome_accept
import app.reporrun.feature.onboarding.resources.welcome_body
import app.reporrun.feature.onboarding.resources.welcome_brand
import app.reporrun.feature.onboarding.resources.welcome_headline
import app.reporrun.feature.onboarding.resources.welcome_notice_data_sent
import app.reporrun.feature.onboarding.resources.welcome_notice_local_data
import app.reporrun.feature.onboarding.resources.welcome_notice_not_medical
import app.reporrun.feature.onboarding.resources.welcome_notice_title
import app.reporrun.feature.onboarding.resources.welcome_overline
import app.reporrun.feature.onboarding.resources.welcome_read_notice
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * La mitad con estado de la pantalla, y la única que conoce el ViewModel. Hace tres cosas:
 * recoge el estado, traduce cada efecto a una lambda de navegación y pasa `onEvent` hacia
 * abajo. No pinta nada.
 *
 * Recibe **lambdas**, no el `NavController`: un feature no sabe qué hay al otro lado.
 */
@Composable
internal fun WelcomeRoute(
    onConsentAccepted: () -> Unit,
    onReadNotice: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    // `WithLifecycle`: con la app en segundo plano deja de recoger y no recompone para nadie.
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.ConsentAccepted -> onConsentAccepted()
            OnboardingEffect.NoticeRequested -> onReadNotice()
        }
    }

    WelcomeScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * La mitad sin estado: una función de `(state, onEvent)`. No hay ViewModel, ni Koin, ni
 * navegación, así que es la que se previsualiza y se captura para el PR con un estado
 * escrito a mano.
 *
 * **Superficie constante** (Anexo A): la bienvenida se ve igual en claro y en oscuro. Por eso
 * todo va dentro de un `RepOrRunTheme(darkTheme = true)` propio, y cada color sale de un rol
 * de ese tema, no de un literal.
 *
 * Tres cosas no están como en Figma, a propósito:
 * - El subtítulo va en `onSurfaceVariant`; en el diseño es `#979797`, que no es color de texto.
 * - La acción principal es `inverseSurface` (blanca aquí); en el diseño es naranja, y el
 *   naranja marca estado, no manda.
 * - Los checks van en `accentText` del tema oscuro; en el diseño resuelven al del claro, que
 *   sobre esta tarjeta no llega a 3:1.
 */
@Composable
internal fun WelcomeScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    RepOrRunTheme(darkTheme = true) {
        // `Surface` y no un `Box` con fondo: fija también el color del contenido, que si no
        // llegaría del tema de fuera.
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box {
                RepOrRunWatermark(Modifier.matchParentSize())
                // Con fuente grande o pantalla baja no cabe: aceptar tiene que seguir al alcance.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .safeDrawingPadding()
                        .padding(
                            start = RepOrRunDimens.ScreenPadding,
                            top = RepOrRunDimens.WelcomeTopPadding,
                            end = RepOrRunDimens.ScreenPadding,
                            bottom = RepOrRunDimens.ScreenPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.WelcomeSectionSpacing),
                ) {
                    WelcomeHero()
                    NoticeSummaryCard()
                    RepOrRunButton(
                        text = stringResource(Res.string.welcome_accept),
                        onClick = { onEvent(OnboardingEvent.AcceptClicked) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                    )
                    RepOrRunButton(
                        text = stringResource(Res.string.welcome_read_notice),
                        onClick = { onEvent(OnboardingEvent.ReadNoticeClicked) },
                        modifier = Modifier.fillMaxWidth(),
                        style = RepOrRunButtonStyle.Outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeHero() {
    Text(stringResource(Res.string.welcome_brand), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(RepOrRunDimens.WelcomeHeroSpacing))
    Text(
        // El estilo no fuerza las mayúsculas: las pone quien lo usa.
        stringResource(Res.string.welcome_overline).uppercase(),
        style = RepOrRunTheme.extendedTypography.overline,
    )
    Text(stringResource(Res.string.welcome_headline), style = MaterialTheme.typography.displayLarge)
    Text(
        stringResource(Res.string.welcome_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** El resumen del aviso: lo mínimo que hay que haber visto antes de aceptar. */
@Composable
private fun NoticeSummaryCard() {
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(RepOrRunDimens.WelcomeSectionSpacing),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChecklistRowSpacing)) {
            Text(stringResource(Res.string.welcome_notice_title), style = MaterialTheme.typography.labelMedium)
            NoticePoint(stringResource(Res.string.welcome_notice_not_medical))
            NoticePoint(stringResource(Res.string.welcome_notice_data_sent))
            NoticePoint(stringResource(Res.string.welcome_notice_local_data))
        }
    }
}

@Composable
private fun NoticePoint(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChecklistIconSpacing)) {
        Icon(
            painter = RepOrRunIcons.Check,
            // Decorativo: el punto ya se lee en el texto.
            contentDescription = null,
            modifier = Modifier.size(RepOrRunDimens.ChecklistIconSize),
            tint = RepOrRunTheme.extendedColors.accentText,
        )
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}
