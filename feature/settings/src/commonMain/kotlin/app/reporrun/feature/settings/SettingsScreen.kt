package app.reporrun.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunChipGroup
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.designsystem.component.RepOrRunScreenFrame
import app.reporrun.core.designsystem.component.RepOrRunScrollContent
import app.reporrun.core.model.ThemeMode
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.settings.resources.Res
import app.reporrun.feature.settings.resources.settings_back
import app.reporrun.feature.settings.resources.settings_licenses
import app.reporrun.feature.settings.resources.settings_licenses_summary
import app.reporrun.feature.settings.resources.settings_notice
import app.reporrun.feature.settings.resources.settings_profile
import app.reporrun.feature.settings.resources.settings_section_about
import app.reporrun.feature.settings.resources.settings_section_appearance
import app.reporrun.feature.settings.resources.settings_section_data
import app.reporrun.feature.settings.resources.settings_section_privacy
import app.reporrun.feature.settings.resources.settings_title
import app.reporrun.feature.settings.resources.settings_version
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Ajustes, con estado. */
@Composable
internal fun SettingsRoute(viewModel: SettingsViewModel, appVersion: String, actions: SettingsActions) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.ProfileRequested -> actions.onEditProfile()
            SettingsEffect.NoticeRequested -> actions.onReadNotice()
            SettingsEffect.LicensesRequested -> actions.onOpenLicenses()
        }
    }

    SettingsScreen(state = state, appVersion = appVersion, onEvent = viewModel::onEvent, onBack = actions.onBack)
}

/**
 * Ajustes, sobre la pantalla 13 de Figma: el título grande, las secciones con su overline y las
 * tarjetas de filas. De ella **no** se copian «Gimnasios», «Recordatorios», «Borrar todos los
 * datos» ni el botón de perfil de la cabecera: no entran en esta versión. Y lo que no dibuja
 * —elegir el tema— va en una sección propia con los chips del design system.
 *
 * Lleva botón de volver porque aquí no es una pestaña: se abre desde la vista del plan.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RepOrRunScreenFrame(modifier = modifier) {
        RepOrRunScrollContent {
            RepOrRunIconButton(
                icon = RepOrRunIcons.ArrowBack,
                contentDescription = stringResource(Res.string.settings_back),
                onClick = onBack,
            )
            Text(
                stringResource(Res.string.settings_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.displayLarge,
            )

            SectionLabel(Res.string.settings_section_appearance)
            RepOrRunChipGroup(
                options = ThemeMode.entries,
                selected = { it == state.themeMode },
                label = { it.label() },
                onClick = { onEvent(SettingsEvent.ThemeModeSelected(it)) },
            )

            SectionLabel(Res.string.settings_section_data)
            RepOrRunCard(modifier = Modifier.fillMaxWidth(), contentPadding = RowCardPadding) {
                RepOrRunListRow(
                    title = stringResource(Res.string.settings_profile),
                    subtitle = state.profile?.label(),
                    leadingIcon = RepOrRunIcons.Person,
                    onClick = { onEvent(SettingsEvent.ProfileClicked) },
                )
            }

            SectionLabel(Res.string.settings_section_privacy)
            RepOrRunCard(modifier = Modifier.fillMaxWidth(), contentPadding = RowCardPadding) {
                RepOrRunListRow(
                    title = stringResource(Res.string.settings_notice),
                    subtitle = state.consentAcceptedOn?.acceptedLabel(),
                    leadingIcon = RepOrRunIcons.Settings,
                    onClick = { onEvent(SettingsEvent.NoticeClicked) },
                )
            }

            SectionLabel(Res.string.settings_section_about)
            RepOrRunCard(modifier = Modifier.fillMaxWidth(), contentPadding = RowCardPadding) {
                RepOrRunListRow(
                    title = stringResource(Res.string.settings_licenses),
                    subtitle = stringResource(Res.string.settings_licenses_summary),
                    leadingIcon = RepOrRunIcons.Settings,
                    onClick = { onEvent(SettingsEvent.LicensesClicked) },
                )
            }
            Text(
                stringResource(Res.string.settings_version, appVersion),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** El relleno de una tarjeta de filas: el lateral de tarjeta y el vertical corto de la pantalla 13. */
private val RowCardPadding = PaddingValues(
    horizontal = RepOrRunDimens.CardHorizontalPadding,
    vertical = RepOrRunDimens.ListCardVerticalPadding,
)

/** El overline de una sección, sobre el lienzo. Va en `onSurfaceVariant`, no en acento. */
@Composable
private fun SectionLabel(text: StringResource) {
    Text(
        // El estilo no fuerza las mayúsculas: las pone quien lo usa.
        stringResource(text).uppercase(),
        modifier = Modifier.semantics { heading() },
        style = RepOrRunTheme.extendedTypography.overline,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
