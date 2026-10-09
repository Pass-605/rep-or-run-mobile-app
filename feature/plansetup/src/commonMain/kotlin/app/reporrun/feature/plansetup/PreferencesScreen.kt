package app.reporrun.feature.plansetup

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunField
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.PreferencesField
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.plansetup.resources.Res
import app.reporrun.feature.plansetup.resources.prefs_avoid
import app.reporrun.feature.plansetup.resources.prefs_avoid_placeholder
import app.reporrun.feature.plansetup.resources.prefs_avoid_privacy
import app.reporrun.feature.plansetup.resources.prefs_block_days
import app.reporrun.feature.plansetup.resources.prefs_block_days_hint
import app.reporrun.feature.plansetup.resources.prefs_block_days_value
import app.reporrun.feature.plansetup.resources.prefs_cardio
import app.reporrun.feature.plansetup.resources.prefs_cardio_empty
import app.reporrun.feature.plansetup.resources.prefs_cardio_hint
import app.reporrun.feature.plansetup.resources.prefs_days
import app.reporrun.feature.plansetup.resources.prefs_days_hint
import app.reporrun.feature.plansetup.resources.prefs_error_days_required
import app.reporrun.feature.plansetup.resources.prefs_minutes_unit
import app.reporrun.feature.plansetup.resources.prefs_no_start_date
import app.reporrun.feature.plansetup.resources.prefs_save
import app.reporrun.feature.plansetup.resources.prefs_session_minutes
import app.reporrun.feature.plansetup.resources.prefs_start_date
import app.reporrun.feature.plansetup.resources.prefs_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Las preferencias, con estado. */
@Composable
internal fun PreferencesRoute(viewModel: PreferencesViewModel, onBack: () -> Unit, onPreferencesSaved: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PreferencesEffect.PreferencesSaved -> onPreferencesSaved()
        }
    }

    PreferencesScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/**
 * La pantalla 04: con qué se pide el siguiente bloque. Los errores salen junto a su campo, con
 * el límite que da el caso de uso; la pantalla no compara ningún número.
 *
 * La duración del bloque se enseña y no se edita: v1 solo admite una (plan §4), y el número
 * viene del estado, que lo toma del modelo.
 */
@Composable
internal fun PreferencesScreen(
    state: PreferencesUiState,
    onEvent: (PreferencesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanSetupScaffold(
        title = stringResource(Res.string.prefs_title),
        actionText = stringResource(Res.string.prefs_save),
        actionEnabled = !state.isLoading && !state.isSaving,
        onAction = { onEvent(PreferencesEvent.SaveClicked) },
        onBack = onBack,
        modifier = modifier,
    ) {
        // Hasta saber si hay preferencias guardadas no se pinta el formulario.
        if (state.isLoading) return@PlanSetupScaffold

        AvailableDays(state, onEvent)
        SessionMinutes(state, onEvent)
        BlockDays(state.blockDays)
        StartDate(state, onEvent)
        CardioModes(state, onEvent)
        AvoidExercises(state, onEvent)
    }
}

/** El selector de semana: un chip por día, de lunes a domingo. */
@Composable
private fun AvailableDays(state: PreferencesUiState, onEvent: (PreferencesEvent) -> Unit) {
    SectionTitle(stringResource(Res.string.prefs_days))
    SupportingText(stringResource(Res.string.prefs_days_hint))
    ChipGroup(
        options = DayOfWeek.entries,
        selected = { it in state.availableDays },
        label = { stringResource(it.shortLabel) },
        onClick = { onEvent(PreferencesEvent.DayToggled(it)) },
    )
    SectionError(
        when (val issue = state.errors[PreferencesField.AVAILABLE_DAYS]) {
            null -> null

            // «Falta este dato» no dice qué hacer con siete chips delante.
            FieldIssue.Required -> stringResource(Res.string.prefs_error_days_required)

            else -> issue.message()
        },
    )
}

@Composable
private fun SessionMinutes(state: PreferencesUiState, onEvent: (PreferencesEvent) -> Unit) {
    RepOrRunField(
        value = state.sessionMinutes,
        onValueChange = { onEvent(PreferencesEvent.SessionMinutesChanged(it)) },
        label = stringResource(Res.string.prefs_session_minutes),
        modifier = Modifier.fillMaxWidth(),
        unit = stringResource(Res.string.prefs_minutes_unit),
        errorText = state.errors[PreferencesField.SESSION_MINUTES]?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    )
}

@Composable
private fun BlockDays(blockDays: Int) {
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RepOrRunDimens.CardHorizontalPadding),
    ) {
        RepOrRunListRow(
            title = stringResource(Res.string.prefs_block_days),
            subtitle = stringResource(Res.string.prefs_block_days_hint),
            trailing = {
                Text(
                    pluralStringResource(Res.plurals.prefs_block_days_value, blockDays, blockDays),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
        )
    }
}

/** «Sin fecha» y las fechas de hoy en adelante. No caben en una línea: se deslizan. */
@Composable
private fun StartDate(state: PreferencesUiState, onEvent: (PreferencesEvent) -> Unit) {
    SectionTitle(stringResource(Res.string.prefs_start_date))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
    ) {
        RepOrRunChip(
            label = stringResource(Res.string.prefs_no_start_date),
            selected = state.startDate == null,
            onClick = { onEvent(PreferencesEvent.StartDateSelected(null)) },
        )
        state.startDateOptions.forEach { date ->
            RepOrRunChip(
                label = date.optionLabel(),
                selected = state.startDate == date,
                onClick = { onEvent(PreferencesEvent.StartDateSelected(date)) },
            )
        }
    }
}

/**
 * Los modos de cardio. No hay lista escrita aquí: son las máquinas de cardio del gimnasio
 * activo, y si no tiene ninguna la pantalla lo dice en vez de ofrecer algo que no hay.
 */
@Composable
private fun CardioModes(state: PreferencesUiState, onEvent: (PreferencesEvent) -> Unit) {
    SectionTitle(stringResource(Res.string.prefs_cardio))
    if (state.cardioOptions.isEmpty()) {
        RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.prefs_cardio_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    SupportingText(stringResource(Res.string.prefs_cardio_hint))
    ChipGroup(
        options = state.cardioOptions,
        selected = { it in state.cardioModes },
        label = { it },
        onClick = { onEvent(PreferencesEvent.CardioModeToggled(it)) },
    )
    SectionError(state.errors[PreferencesField.CARDIO_MODES]?.message())
}

@Composable
private fun AvoidExercises(state: PreferencesUiState, onEvent: (PreferencesEvent) -> Unit) {
    RepOrRunField(
        value = state.avoidExercises,
        onValueChange = { onEvent(PreferencesEvent.AvoidExercisesChanged(it)) },
        label = stringResource(Res.string.prefs_avoid),
        modifier = Modifier.fillMaxWidth(),
        placeholder = stringResource(Res.string.prefs_avoid_placeholder),
        errorText = state.errors[PreferencesField.AVOID_EXERCISES]?.message(),
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
    SupportingText(stringResource(Res.string.prefs_avoid_privacy))
}
