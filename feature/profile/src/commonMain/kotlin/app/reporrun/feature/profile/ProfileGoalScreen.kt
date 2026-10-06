package app.reporrun.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.domain.usecase.ProfileField
import app.reporrun.core.model.Goal
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.UserProfile
import app.reporrun.feature.profile.resources.Res
import app.reporrun.feature.profile.resources.profile_free_text_privacy
import app.reporrun.feature.profile.resources.profile_goal
import app.reporrun.feature.profile.resources.profile_goal_card_cardio
import app.reporrun.feature.profile.resources.profile_goal_card_changes_title
import app.reporrun.feature.profile.resources.profile_goal_card_meaning_title
import app.reporrun.feature.profile.resources.profile_goal_card_no_nutrition
import app.reporrun.feature.profile.resources.profile_goal_card_reps
import app.reporrun.feature.profile.resources.profile_goal_card_rest
import app.reporrun.feature.profile.resources.profile_goal_card_volume
import app.reporrun.feature.profile.resources.profile_limitations
import app.reporrun.feature.profile.resources.profile_limitations_placeholder
import app.reporrun.feature.profile.resources.profile_preferences
import app.reporrun.feature.profile.resources.profile_preferences_placeholder
import app.reporrun.feature.profile.resources.profile_priority_muscles
import app.reporrun.feature.profile.resources.profile_save
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Paso 2, con estado. Recibe el mismo ViewModel que el paso 1. */
@Composable
internal fun ProfileGoalRoute(
    viewModel: ProfileViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileEffects(viewModel, onContinue = onContinue, onBack = onBack, onProfileSaved = onProfileSaved)

    ProfileGoalScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/**
 * Paso 2 de la pantalla 02 de Figma: qué busca la persona. Los músculos prioritarios y las
 * preferencias no están en el diseño v2; se componen con los mismos chips y el mismo campo.
 *
 * Una cosa no está como en Figma, a propósito: la nota de alimentación va en
 * `onSurfaceVariant`. En el diseño claro es `#979797`, que no es color de texto (2,86:1).
 */
@Composable
internal fun ProfileGoalScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileStepScaffold(
        step = STEP_GOAL,
        actionText = stringResource(Res.string.profile_save),
        actionEnabled = !state.isSaving,
        onAction = { onEvent(ProfileEvent.SaveClicked) },
        modifier = modifier,
        onBack = onBack,
    ) {
        SectionTitle(stringResource(Res.string.profile_goal))
        ChipGroup(
            options = Goal.entries,
            selected = { it == state.goal },
            label = { stringResource(it.label) },
            onClick = { onEvent(ProfileEvent.GoalSelected(it)) },
        )
        SectionError(state.errors[ProfileField.GOAL])
        state.goal?.let { GoalCard(it) }

        // El tope sale del modelo, que es el del contrato: el título no puede decir otro número.
        SectionTitle(stringResource(Res.string.profile_priority_muscles, UserProfile.MAX_PRIORITY_MUSCLES))
        ChipGroup(
            options = MuscleGroup.entries,
            selected = { it in state.priorityMuscles },
            label = { stringResource(it.label) },
            onClick = { onEvent(ProfileEvent.MuscleToggled(it)) },
        )
        SectionError(state.errors[ProfileField.PRIORITY_MUSCLES])

        FreeTextInput(
            state = state,
            field = ProfileTextField.LIMITATIONS,
            value = state.limitations,
            label = Res.string.profile_limitations,
            placeholder = Res.string.profile_limitations_placeholder,
            onEvent = onEvent,
        )
        FreeTextInput(
            state = state,
            field = ProfileTextField.PREFERENCES,
            value = state.preferences,
            label = Res.string.profile_preferences,
            placeholder = Res.string.profile_preferences_placeholder,
            onEvent = onEvent,
        )
        SupportingText(stringResource(Res.string.profile_free_text_privacy))
    }
}

@Composable
private fun FreeTextInput(
    state: ProfileUiState,
    field: ProfileTextField,
    value: String,
    label: StringResource,
    placeholder: StringResource,
    onEvent: (ProfileEvent) -> Unit,
) {
    ProfileTextInput(
        state = state,
        field = field,
        value = value,
        label = label,
        onEvent = onEvent,
        modifier = Modifier.fillMaxWidth(),
        placeholder = stringResource(placeholder),
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

/**
 * Qué significa el objetivo elegido y cómo cambia el plan. Todo lo que dice son textos fijos
 * de la app ([Goal.explanation]); ninguno viene del modelo.
 *
 * El borde y el punto van en `primary`: la tarjeta marca lo que se acaba de elegir, y ahí el
 * acento sí manda. El título va en `accentText`, que dentro de una tarjeta cumple en los dos
 * temas.
 */
@Composable
private fun GoalCard(goal: Goal) {
    val colors = MaterialTheme.colorScheme
    val explanation = goal.explanation
    RepOrRunCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(RepOrRunDimens.GoalCardBorder, colors.primary, MaterialTheme.shapes.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.GoalCardSpacing)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(RepOrRunDimens.GoalCardDotSize).background(colors.primary, CircleShape))
                Text(
                    stringResource(Res.string.profile_goal_card_meaning_title, stringResource(goal.label))
                        .uppercase(),
                    modifier = Modifier.weight(1f),
                    style = RepOrRunTheme.extendedTypography.overline,
                    color = RepOrRunTheme.extendedColors.accentText,
                )
            }
            Text(stringResource(explanation.meaning), style = MaterialTheme.typography.bodySmall)
            RepOrRunDivider()
            Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.GoalCardRowSpacing)) {
                Text(
                    stringResource(Res.string.profile_goal_card_changes_title).uppercase(),
                    style = RepOrRunTheme.extendedTypography.overline,
                    color = colors.onSurfaceVariant,
                )
                GoalCardRow(Res.string.profile_goal_card_reps, explanation.reps)
                GoalCardRow(Res.string.profile_goal_card_volume, explanation.volume)
                GoalCardRow(Res.string.profile_goal_card_rest, explanation.rest)
                GoalCardRow(Res.string.profile_goal_card_cardio, explanation.cardio)
            }
            // La nota fija de «sin pautas de alimentación»: sale con cualquier objetivo.
            Text(
                stringResource(Res.string.profile_goal_card_no_nutrition),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalCardRow(label: StringResource, value: StringResource) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(value), style = MaterialTheme.typography.labelMedium)
    }
}
