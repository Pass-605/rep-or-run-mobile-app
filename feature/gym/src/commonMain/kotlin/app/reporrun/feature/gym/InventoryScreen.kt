package app.reporrun.feature.gym

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunCheckMark
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.designsystem.component.RepOrRunSearchField
import app.reporrun.core.designsystem.component.RepOrRunTopBar
import app.reporrun.core.model.GymId
import app.reporrun.core.ui.CollectEffects
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.gym_back
import app.reporrun.feature.gym.resources.inventory_add_custom
import app.reporrun.feature.gym.resources.inventory_all
import app.reporrun.feature.gym.resources.inventory_continue
import app.reporrun.feature.gym.resources.inventory_edit_custom
import app.reporrun.feature.gym.resources.inventory_edit_range
import app.reporrun.feature.gym.resources.inventory_full
import app.reporrun.feature.gym.resources.inventory_in_use
import app.reporrun.feature.gym.resources.inventory_min_hint
import app.reporrun.feature.gym.resources.inventory_no_results
import app.reporrun.feature.gym.resources.inventory_search
import app.reporrun.feature.gym.resources.inventory_selected
import app.reporrun.feature.gym.resources.inventory_title
import app.reporrun.feature.gym.resources.inventory_unavailable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Los efectos del inventario, traducidos a navegación. Los dos destinos llaman a esta misma
 * función, como [GymEffects] y por el mismo motivo.
 */
@Composable
internal fun InventoryEffects(viewModel: InventoryViewModel, onOpenEditor: () -> Unit, onCloseEditor: () -> Unit) {
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            InventoryEffect.EditorOpened -> onOpenEditor()
            InventoryEffect.EditorClosed -> onCloseEditor()
        }
    }
}

/**
 * El inventario, con estado. El ViewModel se lo pasa el grafo y es el mismo que el del
 * formulario; [gymId] viene de la ruta y se le dice al abrirse.
 */
@Composable
internal fun InventoryRoute(
    viewModel: InventoryViewModel,
    gymId: GymId,
    onOpenEditor: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(gymId) { viewModel.onEvent(InventoryEvent.Opened(gymId)) }
    InventoryEffects(viewModel, onOpenEditor = onOpenEditor, onCloseEditor = onBack)

    InventoryScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, onContinue = onContinue)
}

/**
 * La pantalla 03 de Figma: el catálogo con lo que el gimnasio tiene marcado. La barra y el
 * buscador se quedan arriba; los filtros, el contador y la lista hacen scroll juntos en una
 * `LazyColumn` con claves estables, que es lo que la mantiene fluida con más de cien filas.
 *
 * La acción del pie es el «Continuar» de Figma: lleva al paso siguiente, las preferencias del
 * bloque. Volver a los gimnasios es cosa de la barra de arriba.
 */
@Composable
internal fun InventoryScreen(
    state: InventoryUiState,
    onEvent: (InventoryEvent) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Incluye el teclado: con él abierto la acción sube y la lista sigue haciendo scroll.
            .safeDrawingPadding()
            .padding(horizontal = RepOrRunDimens.ScreenPadding),
    ) {
        Column(
            // El hueco de abajo es del buscador y no de la lista: lo que hace scroll se corta
            // antes de llegar a él, no pegado a su borde.
            modifier = Modifier.padding(top = RepOrRunDimens.ScreenPadding, bottom = RepOrRunDimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
        ) {
            RepOrRunTopBar(
                title = stringResource(Res.string.inventory_title),
                backContentDescription = stringResource(Res.string.gym_back),
                onBack = onBack,
            )
            SearchBar(state.query, onEvent)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = RepOrRunDimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.CardListSpacing),
        ) {
            // Hasta que Room contesta no se pinta nada, para que la lista no parpadee vacía.
            if (!state.isLoading) inventoryItems(state, onEvent)
        }
        RepOrRunButton(
            text = stringResource(Res.string.inventory_continue),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RepOrRunDimens.SectionSpacing),
            icon = RepOrRunIcons.ArrowForward,
        )
    }
}

private fun LazyListScope.inventoryItems(state: InventoryUiState, onEvent: (InventoryEvent) -> Unit) {
    item(key = FILTERS_KEY, contentType = FILTERS_KEY) { CategoryFilters(state, onEvent) }
    item(key = SUMMARY_KEY, contentType = SUMMARY_KEY) { Summary(state) }
    if (state.rows.isEmpty()) {
        item(key = EMPTY_KEY, contentType = EMPTY_KEY) { NoResults() }
    }
    items(state.rows, key = { it.key }, contentType = { it::class }) { row ->
        when (row) {
            is InventoryRow.Header -> CategoryHeader(row)
            is InventoryRow.Item -> EquipmentCard(row, state.notice?.takeIf { it.rowKey == row.key }, onEvent)
        }
    }
}

/** El buscador y, a su lado, añadir una máquina que no está en el catálogo. */
@Composable
private fun SearchBar(query: String, onEvent: (InventoryEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.SearchFieldSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RepOrRunSearchField(
            value = query,
            onValueChange = { onEvent(InventoryEvent.QueryChanged(it)) },
            placeholder = stringResource(Res.string.inventory_search),
            modifier = Modifier.weight(1f),
        )
        RepOrRunIconButton(
            icon = RepOrRunIcons.Add,
            contentDescription = stringResource(Res.string.inventory_add_custom),
            onClick = { onEvent(InventoryEvent.AddCustomClicked) },
            emphasis = true,
        )
    }
}

/** «Todas» y un chip por categoría con máquinas. No caben en una línea: se deslizan. */
@Composable
private fun CategoryFilters(state: InventoryUiState, onEvent: (InventoryEvent) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
    ) {
        RepOrRunChip(
            label = stringResource(Res.string.inventory_all),
            selected = state.category == null,
            onClick = { onEvent(InventoryEvent.CategorySelected(null)) },
        )
        state.categories.forEach { category ->
            RepOrRunChip(
                label = category.label(),
                selected = state.category == category,
                onClick = { onEvent(InventoryEvent.CategorySelected(category)) },
            )
        }
    }
}

/** El gimnasio y cuántas máquinas tiene; mientras no lleguen para generar, cuántas hacen falta. */
@Composable
private fun Summary(state: InventoryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowTextSpacing)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.gymName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(
                pluralStringResource(Res.plurals.inventory_selected, state.selectedCount, state.selectedCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.selectedCount < state.minToGenerate) {
            SupportingText(stringResource(Res.string.inventory_min_hint, state.minToGenerate))
        }
    }
}

@Composable
private fun CategoryHeader(header: InventoryRow.Header) {
    Text(
        header.category.label(),
        modifier = Modifier
            .padding(top = RepOrRunDimens.FieldLabelSpacing)
            .semantics { heading() },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NoResults() {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.inventory_no_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Una máquina. Tocar una del catálogo la marca o la desmarca. Una que ya está en el gimnasio
 * tiene debajo la fila que abre su formulario; una personalizada no se desmarca aquí, porque
 * quitarla borra lo que la persona escribió: se quita desde ese formulario.
 *
 * La marca es [RepOrRunCheckMark] y no el círculo de Figma, que sin marcar mide 1,13:1.
 */
@Composable
private fun EquipmentCard(item: InventoryRow.Item, notice: InventoryNotice?, onEvent: (InventoryEvent) -> Unit) {
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RepOrRunDimens.CardHorizontalPadding),
    ) {
        RepOrRunListRow(
            title = item.name,
            // La marca es decorativa: el estado se anuncia como selección de la fila.
            modifier = Modifier.semantics { selected = item.isSelected },
            subtitle = item.subtitle(),
            onClick = if (item.catalogKey != null) ({ onEvent(InventoryEvent.ItemToggled(item.key)) }) else null,
            trailing = { RepOrRunCheckMark(checked = item.isSelected) },
        )
        if (notice != null) {
            // Dentro de la tarjeta: `accentText` no llega a 4,5:1 sobre el fondo de la pantalla.
            Text(
                notice.message(),
                modifier = Modifier.padding(bottom = RepOrRunDimens.ListRowVerticalPadding),
                style = MaterialTheme.typography.bodySmall,
                color = RepOrRunTheme.extendedColors.accentText,
            )
        }
        if (item.isSelected) {
            RepOrRunDivider()
            RepOrRunListRow(
                title = stringResource(
                    if (item.isCustom) Res.string.inventory_edit_custom else Res.string.inventory_edit_range,
                ),
                onClick = { onEvent(InventoryEvent.EditClicked(item.key)) },
            )
        }
    }
}

@Composable
private fun InventoryNotice.message(): String = when (this) {
    is InventoryNotice.InUse -> pluralStringResource(Res.plurals.inventory_in_use, usages, usages)
    is InventoryNotice.Full -> stringResource(Res.string.inventory_full, max)
    is InventoryNotice.Unavailable -> stringResource(Res.string.inventory_unavailable)
}

private const val FILTERS_KEY = "filters"
private const val SUMMARY_KEY = "summary"
private const val EMPTY_KEY = "empty"
