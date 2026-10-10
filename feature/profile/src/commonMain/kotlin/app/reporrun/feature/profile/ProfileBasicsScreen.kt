package app.reporrun.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.component.RepOrRunChipGroup
import app.reporrun.core.designsystem.component.RepOrRunField
import app.reporrun.core.designsystem.component.RepOrRunSectionError
import app.reporrun.core.designsystem.component.RepOrRunSectionTitle
import app.reporrun.core.designsystem.component.RepOrRunSupportingText
import app.reporrun.core.domain.usecase.ProfileField
import app.reporrun.core.model.Experience
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.profile.resources.Res
import app.reporrun.feature.profile.resources.profile_age
import app.reporrun.feature.profile.resources.profile_alias
import app.reporrun.feature.profile.resources.profile_continue
import app.reporrun.feature.profile.resources.profile_experience
import app.reporrun.feature.profile.resources.profile_height
import app.reporrun.feature.profile.resources.profile_hip
import app.reporrun.feature.profile.resources.profile_subtitle
import app.reporrun.feature.profile.resources.profile_unit_cm
import app.reporrun.feature.profile.resources.profile_unit_kg
import app.reporrun.feature.profile.resources.profile_unit_years
import app.reporrun.feature.profile.resources.profile_waist
import app.reporrun.feature.profile.resources.profile_weight
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Paso 1, con estado. El ViewModel se lo pasa el grafo: es el mismo que el del paso 2. */
@Composable
internal fun ProfileBasicsRoute(
    viewModel: ProfileViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileEffects(viewModel, onContinue = onContinue, onBack = onBack, onProfileSaved = onProfileSaved)

    ProfileBasicsScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * Los efectos del perfil, traducidos a navegación. Los dos pasos llaman a esta misma función:
 * comparten ViewModel y, durante la transición entre uno y otro, cualquiera de los dos puede
 * ser quien recoja el efecto. Así da igual cuál sea.
 */
@Composable
internal fun ProfileEffects(
    viewModel: ProfileViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
) {
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            ProfileEffect.BasicsCompleted -> onContinue()
            ProfileEffect.BasicsNeedReview -> onBack()
            ProfileEffect.ProfileSaved -> onProfileSaved()
        }
    }
}

/**
 * Paso 1 de la pantalla 02 de Figma: quién es la persona. El diseño lo dibuja todo en una
 * pantalla; aquí se parte en dos (plan §3) con las mismas piezas, más la cadera, que el
 * diseño no trae y el perfil sí.
 */
@Composable
internal fun ProfileBasicsScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileStepScaffold(
        step = STEP_BASICS,
        actionText = stringResource(Res.string.profile_continue),
        actionEnabled = !state.isLoading,
        onAction = { onEvent(ProfileEvent.ContinueClicked) },
        modifier = modifier,
        actionIcon = RepOrRunIcons.ArrowForward,
    ) {
        // Hasta saber si hay un perfil guardado no se pinta el formulario.
        if (state.isLoading) return@ProfileStepScaffold

        RepOrRunSupportingText(stringResource(Res.string.profile_subtitle))
        ProfileTextInput(
            state = state,
            field = ProfileTextField.ALIAS,
            value = state.alias,
            label = Res.string.profile_alias,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        FieldRow {
            NumberInput(state, ProfileTextField.AGE, state.age, Res.string.profile_age, YEARS, onEvent)
            NumberInput(state, ProfileTextField.HEIGHT, state.height, Res.string.profile_height, CM, onEvent)
        }
        FieldRow {
            NumberInput(state, ProfileTextField.WEIGHT, state.weight, Res.string.profile_weight, KG, onEvent)
            NumberInput(state, ProfileTextField.WAIST, state.waist, Res.string.profile_waist, CM, onEvent)
        }
        FieldRow {
            NumberInput(state, ProfileTextField.HIP, state.hip, Res.string.profile_hip, CM, onEvent)
            // La otra mitad queda libre: la cadera mide lo mismo que los demás campos.
            Spacer(Modifier.weight(1f))
        }
        RepOrRunSectionTitle(stringResource(Res.string.profile_experience))
        RepOrRunChipGroup(
            options = Experience.entries,
            selected = { it == state.experience },
            label = { stringResource(it.label) },
            onClick = { onEvent(ProfileEvent.ExperienceSelected(it)) },
        )
        RepOrRunSectionError(state.errors[ProfileField.EXPERIENCE]?.message())
    }
}

private val YEARS = Res.string.profile_unit_years
private val CM = Res.string.profile_unit_cm
private val KG = Res.string.profile_unit_kg

/** Dos campos a la misma altura, cada uno con la mitad del ancho. */
@Composable
private fun FieldRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        content = content,
    )
}

/** Campo numérico con su unidad. La edad pide teclado de enteros; el resto, con decimales. */
@Composable
private fun RowScope.NumberInput(
    state: ProfileUiState,
    field: ProfileTextField,
    value: String,
    label: StringResource,
    unit: StringResource,
    onEvent: (ProfileEvent) -> Unit,
) {
    ProfileTextInput(
        state = state,
        field = field,
        value = value,
        label = label,
        onEvent = onEvent,
        modifier = Modifier.weight(1f),
        unit = stringResource(unit),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (field == ProfileTextField.AGE) KeyboardType.Number else KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
    )
}

/** Un `RepOrRunField` enchufado al estado: su valor, su error y su evento. */
@Composable
internal fun ProfileTextInput(
    state: ProfileUiState,
    field: ProfileTextField,
    value: String,
    label: StringResource,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    RepOrRunField(
        value = value,
        onValueChange = { onEvent(ProfileEvent.TextChanged(field, it)) },
        label = stringResource(label),
        modifier = modifier,
        unit = unit,
        placeholder = placeholder,
        errorText = state.errors[field.field]?.message(),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
    )
}
