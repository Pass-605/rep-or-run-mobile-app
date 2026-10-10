package app.reporrun.feature.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunButtonStyle
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunChipGroup
import app.reporrun.core.designsystem.component.RepOrRunField
import app.reporrun.core.designsystem.component.RepOrRunScreenScaffold
import app.reporrun.core.designsystem.component.RepOrRunSectionTitle
import app.reporrun.core.designsystem.component.RepOrRunSupportingText
import app.reporrun.core.domain.usecase.EquipmentError
import app.reporrun.core.domain.usecase.EquipmentField
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.equipment_add
import app.reporrun.feature.gym.resources.equipment_category
import app.reporrun.feature.gym.resources.equipment_edit_title
import app.reporrun.feature.gym.resources.equipment_error_duplicate
import app.reporrun.feature.gym.resources.equipment_error_min_above_max
import app.reporrun.feature.gym.resources.equipment_fixed_type
import app.reporrun.feature.gym.resources.equipment_increment
import app.reporrun.feature.gym.resources.equipment_kg
import app.reporrun.feature.gym.resources.equipment_load_type
import app.reporrun.feature.gym.resources.equipment_max
import app.reporrun.feature.gym.resources.equipment_min
import app.reporrun.feature.gym.resources.equipment_name
import app.reporrun.feature.gym.resources.equipment_name_placeholder
import app.reporrun.feature.gym.resources.equipment_new_subtitle
import app.reporrun.feature.gym.resources.equipment_new_title
import app.reporrun.feature.gym.resources.equipment_range
import app.reporrun.feature.gym.resources.equipment_range_hint
import app.reporrun.feature.gym.resources.equipment_remove
import app.reporrun.feature.gym.resources.equipment_save
import app.reporrun.feature.gym.resources.gym_back
import app.reporrun.feature.gym.resources.inventory_full
import app.reporrun.feature.gym.resources.inventory_in_use
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** El formulario, con estado. El ViewModel se lo pasa el grafo: es el mismo que el del inventario. */
@Composable
internal fun EquipmentEditorRoute(viewModel: InventoryViewModel, onOpenEditor: () -> Unit, onCloseEditor: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InventoryEffects(viewModel, onOpenEditor = onOpenEditor, onCloseEditor = onCloseEditor)

    EquipmentEditorScreen(state = state.editor, onEvent = viewModel::onEvent, onBack = onCloseEditor)
}

/**
 * Una máquina, para añadir una que no está en el catálogo o para editar una del gimnasio: el
 * mismo formulario con otro título y otra acción. Figma v2 no lo dibuja; se compone con las
 * piezas del design system.
 *
 * Al editar, la categoría y la forma de cargarla se leen pero no se cambian (lo decide
 * `UpdateEquipmentUseCase`), y de una máquina del catálogo tampoco el nombre.
 */
@Composable
internal fun EquipmentEditorScreen(
    state: EquipmentEditorState,
    onEvent: (InventoryEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RepOrRunScreenScaffold(
        title = stringResource(if (state.isNew) Res.string.equipment_new_title else Res.string.equipment_edit_title),
        backContentDescription = stringResource(Res.string.gym_back),
        actionText = stringResource(if (state.isNew) Res.string.equipment_add else Res.string.equipment_save),
        actionEnabled = !state.isSaving,
        onAction = { onEvent(EquipmentEditorEvent.SaveClicked) },
        modifier = modifier,
        onBack = onBack,
    ) {
        if (state.isNew) RepOrRunSupportingText(stringResource(Res.string.equipment_new_subtitle))
        RepOrRunField(
            value = state.name,
            onValueChange = { onEvent(EquipmentEditorEvent.NameChanged(it)) },
            label = stringResource(Res.string.equipment_name),
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.equipment_name_placeholder),
            errorText = state.nameError(),
            enabled = state.isCustom,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        )
        if (state.isNew) {
            TypeChoice(state, onEvent)
        } else {
            RepOrRunSupportingText(
                stringResource(Res.string.equipment_fixed_type, state.category.label(), state.loadType.label()),
            )
        }
        LoadRange(state, onEvent)
        EditorProblemCard(state.problem)
        if (!state.isNew && state.isCustom) {
            RepOrRunButton(
                text = stringResource(Res.string.equipment_remove),
                onClick = { onEvent(EquipmentEditorEvent.RemoveClicked) },
                modifier = Modifier.fillMaxWidth(),
                style = RepOrRunButtonStyle.Outline,
                enabled = !state.isSaving,
            )
        }
    }
}

/** La categoría y cómo se carga. Solo al añadir: después ya no se cambian. */
@Composable
private fun TypeChoice(state: EquipmentEditorState, onEvent: (InventoryEvent) -> Unit) {
    RepOrRunSectionTitle(stringResource(Res.string.equipment_category))
    RepOrRunChipGroup(
        options = EquipmentCategory.entries,
        selected = { it == state.category },
        label = { it.label() },
        onClick = { onEvent(EquipmentEditorEvent.CategoryChanged(it)) },
    )
    RepOrRunSectionTitle(stringResource(Res.string.equipment_load_type))
    RepOrRunChipGroup(
        options = LoadType.entries,
        selected = { it == state.loadType },
        label = { it.chipLabel() },
        onClick = { onEvent(EquipmentEditorEvent.LoadTypeChanged(it)) },
    )
}

/** Mínimo, máximo y salto, los tres opcionales. Son datos de la máquina, no una prescripción. */
@Composable
private fun LoadRange(state: EquipmentEditorState, onEvent: (InventoryEvent) -> Unit) {
    RepOrRunSectionTitle(stringResource(Res.string.equipment_range))
    RepOrRunSupportingText(stringResource(Res.string.equipment_range_hint))
    Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing)) {
        KgField(
            state.minKg,
            EquipmentField.MIN_KG,
            Res.string.equipment_min,
            state.fieldError(EquipmentField.MIN_KG),
            onEvent,
        )
        KgField(state.maxKg, EquipmentField.MAX_KG, Res.string.equipment_max, state.maxError(), onEvent)
    }
    Row {
        KgField(
            value = state.incrementKg,
            field = EquipmentField.INCREMENT_KG,
            label = Res.string.equipment_increment,
            error = state.fieldError(EquipmentField.INCREMENT_KG),
            onEvent = onEvent,
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun RowScope.KgField(
    value: String,
    field: EquipmentField,
    label: StringResource,
    error: String?,
    onEvent: (InventoryEvent) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    RepOrRunField(
        value = value,
        onValueChange = { onEvent(EquipmentEditorEvent.KgChanged(field, it)) },
        label = stringResource(label),
        modifier = Modifier.weight(1f),
        unit = stringResource(Res.string.equipment_kg),
        placeholder = "0",
        errorText = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
    )
}

/**
 * Lo que impide guardar o quitar. Va **dentro de una tarjeta**: `accentText` sobre el fondo de
 * la pantalla se queda en 4,07:1 en claro (Anexo A), y sobre `surface` llega a 4,61:1.
 */
@Composable
private fun EditorProblemCard(problem: EditorProblem?) {
    if (problem == null) return
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            when (problem) {
                is EditorProblem.InUse -> pluralStringResource(
                    Res.plurals.inventory_in_use,
                    problem.usages,
                    problem.usages,
                )

                is EditorProblem.Full -> stringResource(Res.string.inventory_full, problem.max)
            },
            style = MaterialTheme.typography.bodySmall,
            color = RepOrRunTheme.extendedColors.accentText,
        )
    }
}

/* Qué error va en qué campo. El primero de cada campo: el caso de uso los da todos juntos. */

@Composable
private fun EquipmentEditorState.fieldError(field: EquipmentField): String? =
    errors.filterIsInstance<EquipmentError.Field>().firstOrNull { it.field == field }?.issue?.message()

@Composable
private fun EquipmentEditorState.nameError(): String? = fieldError(EquipmentField.NAME)
    ?: if (EquipmentError.DuplicateName in errors) stringResource(Res.string.equipment_error_duplicate) else null

/** «El mínimo es mayor que el máximo» se dice en el máximo, que es el campo que se acaba de escribir. */
@Composable
private fun EquipmentEditorState.maxError(): String? = fieldError(EquipmentField.MAX_KG)
    ?: if (EquipmentError.MinAboveMax in errors) stringResource(Res.string.equipment_error_min_above_max) else null
