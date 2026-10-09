package app.reporrun.feature.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.AddEquipmentUseCase
import app.reporrun.core.domain.usecase.EquipmentField
import app.reporrun.core.domain.usecase.GymInventory
import app.reporrun.core.domain.usecase.ObserveGymInventoryUseCase
import app.reporrun.core.domain.usecase.RemoveEquipmentUseCase
import app.reporrun.core.domain.usecase.ToggleEquipmentResult
import app.reporrun.core.domain.usecase.ToggleEquipmentUseCase
import app.reporrun.core.domain.usecase.UpdateEquipmentUseCase
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.GymId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * El inventario de un gimnasio y el formulario de una máquina. Un solo ViewModel para los dos
 * destinos, como [GymViewModel], y vive lo mismo: el grafo del feature. Por eso el gimnasio no
 * llega por constructor sino con [InventoryEvent.Opened], que la pantalla manda al abrirse.
 *
 * **No decide qué entra en el inventario.** El tope de máquinas, que una en uso no se quita y
 * qué datos valen los dicen los casos de uso; aquí sus resultados se traducen a estado.
 *
 * La lista **se observa**: marcar, añadir, editar o quitar no tocan las filas a mano; llegan
 * solas desde Room. Lo que sí es de aquí es qué se ve de lo guardado: la búsqueda y el filtro.
 */
class InventoryViewModel(
    private val observeGymInventory: ObserveGymInventoryUseCase,
    private val toggleEquipment: ToggleEquipmentUseCase,
    addEquipment: AddEquipmentUseCase,
    updateEquipment: UpdateEquipmentUseCase,
    removeEquipment: RemoveEquipmentUseCase,
) : ViewModel() {
    private val editorActions = EquipmentEditorActions(addEquipment, updateEquipment, removeEquipment)

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    private val _effects = Channel<InventoryEffect>(Channel.BUFFERED)
    val effects: Flow<InventoryEffect> = _effects.receiveAsFlow()

    private val gymId = MutableStateFlow<GymId?>(null)

    /** Lo guardado, sin filtrar. Solo se toca desde el hilo principal. */
    private var equipment: List<Equipment> = emptyList()
    private var entries: List<InventoryEntry> = emptyList()

    /**
     * Las escrituras van de una en una. Marcar es leer y luego escribir: dos toques seguidos
     * sobre la misma fila, a la vez, la añadirían dos veces.
     */
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            // `collectLatest`: al cambiar de gimnasio se deja de observar el anterior.
            gymId.filterNotNull().collectLatest { id -> observeGymInventory(id).collect(::show) }
        }
    }

    fun onEvent(event: InventoryEvent) {
        when (event) {
            is InventoryEvent.Opened -> open(event.gymId)

            is InventoryEvent.QueryChanged -> _state.update {
                it.copy(query = event.value, rows = entries.toRows(event.value, it.category))
            }

            is InventoryEvent.CategorySelected -> _state.update {
                it.copy(category = event.category, rows = entries.toRows(it.query, event.category))
            }

            is InventoryEvent.ItemToggled -> toggle(event.rowKey)

            InventoryEvent.AddCustomClicked -> openEditor(EquipmentEditorState())

            is InventoryEvent.EditClicked -> openEdit(event.rowKey)

            is EquipmentEditorEvent -> onEditorEvent(event)
        }
    }

    private fun onEditorEvent(event: EquipmentEditorEvent) {
        when (event) {
            is EquipmentEditorEvent.NameChanged -> edit { it.copy(name = event.value) }
            is EquipmentEditorEvent.CategoryChanged -> edit { it.copy(category = event.category) }
            is EquipmentEditorEvent.LoadTypeChanged -> edit { it.copy(loadType = event.loadType) }
            is EquipmentEditorEvent.KgChanged -> edit { it.withKg(event.field, event.value.asKgInput()) }
            EquipmentEditorEvent.SaveClicked -> finishEditor(editorActions::save)
            EquipmentEditorEvent.RemoveClicked -> finishEditor(editorActions::remove)
        }
    }

    /** Otro gimnasio empieza de cero: ni la búsqueda ni el aviso del anterior valen para este. */
    private fun open(id: GymId) {
        if (gymId.value == id) return
        equipment = emptyList()
        entries = emptyList()
        _state.value = InventoryUiState()
        gymId.value = id
    }

    private fun show(inventory: GymInventory?) {
        equipment = inventory?.equipment.orEmpty()
        entries = inventory?.toEntries().orEmpty()
        _state.update { state ->
            val categories = entries.categories()
            // Si la categoría elegida se quedó sin máquinas, el filtro vuelve a «Todas».
            val category = state.category?.takeIf { it in categories }
            state.copy(
                isLoading = false,
                gymName = inventory?.gym?.name.orEmpty(),
                category = category,
                categories = categories,
                rows = entries.toRows(state.query, category),
                selectedCount = equipment.size,
            )
        }
    }

    private fun toggle(rowKey: String) {
        val gym = gymId.value
        val catalogKey = entries.firstOrNull { it.item.key == rowKey }?.item?.catalogKey
        // Una máquina propia no se desmarca desde la lista: se quita desde su formulario.
        if (gym == null || catalogKey == null) return

        viewModelScope.launch {
            val notice = when (val result = writes.withLock { toggleEquipment(gym, catalogKey) }) {
                // La fila cambia sola cuando Room avisa.
                is ToggleEquipmentResult.Added, is ToggleEquipmentResult.Removed -> null

                // En uso por un plan: se dice en la fila, no se calla.
                is ToggleEquipmentResult.InUse -> InventoryNotice.InUse(rowKey, result.usages)

                is ToggleEquipmentResult.InventoryFull -> InventoryNotice.Full(rowKey, result.max)

                ToggleEquipmentResult.CatalogItemUnavailable -> InventoryNotice.Unavailable(rowKey)

                // El gimnasio ya no existe: la lista se vacía sola.
                ToggleEquipmentResult.GymNotFound -> null
            }
            _state.update { it.copy(notice = notice) }
        }
    }

    private fun openEdit(rowKey: String) {
        val id = entries.firstOrNull { it.item.key == rowKey }?.item?.equipmentId
        // Si ya no está en el inventario no hay nada que editar: el toque llegó tarde.
        val current = equipment.firstOrNull { it.id == id } ?: return
        openEditor(
            EquipmentEditorState(
                equipmentId = current.id,
                isCustom = current.isCustom,
                name = current.name,
                category = current.category,
                loadType = current.loadType,
                minKg = current.minKg?.toKgText().orEmpty(),
                maxKg = current.maxKg?.toKgText().orEmpty(),
                incrementKg = current.incrementKg?.toKgText().orEmpty(),
            ),
        )
    }

    /** Abre el formulario siempre desde cero: no arrastra datos ni errores de la vez anterior. */
    private fun openEditor(editor: EquipmentEditorState) {
        _state.update { it.copy(editor = editor, notice = null) }
        _effects.trySend(InventoryEffect.EditorOpened)
    }

    /** Los errores se van en cuanto se toca el formulario: vuelven, si sigue mal, al guardar. */
    private fun edit(change: (EquipmentEditorState) -> EquipmentEditorState) {
        _state.update { it.copy(editor = change(it.editor).copy(errors = emptyList(), problem = null)) }
    }

    /**
     * Ejecuta [action] una sola vez aunque se pulse dos. [action] devuelve el formulario con lo
     * que impidió terminar, o `null` si no queda nada que hacer en él.
     */
    private fun finishEditor(action: suspend (GymId, EquipmentEditorState) -> EquipmentEditorState?) {
        val gym = gymId.value
        val editor = _state.value.editor
        if (gym == null || editor.isSaving) return
        _state.update { it.copy(editor = editor.copy(isSaving = true)) }

        viewModelScope.launch {
            val pending = writes.withLock { action(gym, editor) }
            if (pending == null) {
                // `isSaving` no vuelve a `false`: el formulario se va.
                _effects.send(InventoryEffect.EditorClosed)
            } else {
                _state.update { it.copy(editor = pending.copy(isSaving = false)) }
            }
        }
    }
}

/** Los campos de kilos comparten evento. Los demás campos de [EquipmentField] no son de kilos. */
private fun EquipmentEditorState.withKg(field: EquipmentField, value: String): EquipmentEditorState = when (field) {
    EquipmentField.MIN_KG -> copy(minKg = value)
    EquipmentField.MAX_KG -> copy(maxKg = value)
    EquipmentField.INCREMENT_KG -> copy(incrementKg = value)
    EquipmentField.NAME, EquipmentField.NOTES -> this
}
