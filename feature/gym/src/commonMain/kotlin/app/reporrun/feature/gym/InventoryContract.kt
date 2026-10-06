// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.gym

import app.reporrun.core.domain.usecase.EquipmentError
import app.reporrun.core.domain.usecase.EquipmentField
import app.reporrun.core.domain.usecase.InventoryRules
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType

/**
 * El inventario de un gimnasio y el formulario de una máquina: los dos destinos comparten estado
 * porque comparten ViewModel.
 *
 * - [rows] es lo que se pinta, ya filtrado por [query] y [category] y con las cabeceras de
 *   categoría puestas. La pantalla no filtra ni agrupa.
 * - [categories] son las que tienen algo en este gimnasio o en el catálogo: un chip por cada una.
 * - [selectedCount] cuenta todo el inventario, no solo lo que deja ver el filtro.
 * - [isLoading] empieza en `true`: hasta que Room contesta no se pinta la lista.
 */
data class InventoryUiState(
    val isLoading: Boolean = true,
    val gymName: String = "",
    val query: String = "",
    val category: EquipmentCategory? = null,
    val categories: List<EquipmentCategory> = emptyList(),
    val rows: List<InventoryRow> = emptyList(),
    val selectedCount: Int = 0,
    val minToGenerate: Int = InventoryRules.MIN_EQUIPMENT_TO_GENERATE,
    val notice: InventoryNotice? = null,
    val editor: EquipmentEditorState = EquipmentEditorState(),
)

/** Una fila de la lista. [key] es estable: no cambia al marcar, desmarcar ni filtrar. */
sealed interface InventoryRow {
    val key: String

    /** Cabecera de un grupo. */
    data class Header(val category: EquipmentCategory) : InventoryRow {
        override val key: String = "h:${category.name}"
    }

    /**
     * Una máquina: una entrada del catálogo, esté o no en el gimnasio, o una máquina propia.
     *
     * @property catalogKey la clave del catálogo; `null` en una máquina personalizada. Es lo que
     *   cruza la fila con el inventario, nunca el nombre.
     * @property equipmentId el aparato del gimnasio, o `null` si la entrada no está marcada.
     */
    data class Item(
        override val key: String,
        val catalogKey: CatalogKey?,
        val equipmentId: EquipmentId?,
        val name: String,
        val category: EquipmentCategory,
        val loadType: LoadType,
        val minKg: Double? = null,
        val maxKg: Double? = null,
        val isCustom: Boolean = false,
    ) : InventoryRow {
        val isSelected: Boolean
            get() = equipmentId != null
    }
}

/** Por qué tocar una fila no hizo lo que se pedía. Se enseña en la propia fila [rowKey]. */
sealed interface InventoryNotice {
    val rowKey: String

    /** No se desmarcó: la usan [usages] ejercicios de planes guardados. */
    data class InUse(override val rowKey: String, val usages: Int) : InventoryNotice

    /** No se marcó: el gimnasio ya tiene [max] máquinas. */
    data class Full(override val rowKey: String, val max: Int) : InventoryNotice

    /** No se marcó: esa entrada del catálogo ya no está disponible. */
    data class Unavailable(override val rowKey: String) : InventoryNotice
}

/**
 * El formulario de una máquina. Sirve para añadir una personalizada y para editar una del
 * inventario.
 *
 * @property equipmentId la máquina que se edita, o `null` si se está añadiendo una.
 * @property isCustom `false` al editar una del catálogo: su nombre no se cambia.
 * @property minKg los kilos son texto: lo que hay en el campo, con coma decimal.
 * @property errors lo que dio el caso de uso, con sus límites. Las frases las pone la pantalla.
 */
data class EquipmentEditorState(
    val equipmentId: EquipmentId? = null,
    val isCustom: Boolean = true,
    val name: String = "",
    val category: EquipmentCategory = EquipmentCategory.MACHINE,
    val loadType: LoadType = LoadType.SELECTOR,
    val minKg: String = "",
    val maxKg: String = "",
    val incrementKg: String = "",
    val errors: List<EquipmentError> = emptyList(),
    val problem: EditorProblem? = null,
    val isSaving: Boolean = false,
) {
    val isNew: Boolean
        get() = equipmentId == null
}

/** Lo que impide guardar o quitar y no es culpa de ningún campo. */
sealed interface EditorProblem {
    /** No se quitó: la usan [usages] ejercicios de planes guardados. */
    data class InUse(val usages: Int) : EditorProblem

    /** No se añadió: el gimnasio ya tiene [max] máquinas. */
    data class Full(val max: Int) : EditorProblem
}

sealed interface InventoryEvent {
    /** La pantalla se abrió para [gymId]. Repetirlo con el mismo gimnasio no hace nada. */
    data class Opened(val gymId: GymId) : InventoryEvent

    data class QueryChanged(val value: String) : InventoryEvent

    /** Filtrar por [category]; `null` es «Todas». */
    data class CategorySelected(val category: EquipmentCategory?) : InventoryEvent

    /** Marcar o desmarcar la entrada del catálogo de la fila [rowKey]. */
    data class ItemToggled(val rowKey: String) : InventoryEvent

    /** Añadir una máquina que no está en el catálogo: abre el formulario vacío. */
    data object AddCustomClicked : InventoryEvent

    /** Editar la máquina de la fila [rowKey]: abre el formulario con sus datos. */
    data class EditClicked(val rowKey: String) : InventoryEvent
}

/** Lo que pasa dentro del formulario de una máquina. */
sealed interface EquipmentEditorEvent : InventoryEvent {
    data class NameChanged(val value: String) : EquipmentEditorEvent

    data class CategoryChanged(val category: EquipmentCategory) : EquipmentEditorEvent

    data class LoadTypeChanged(val loadType: LoadType) : EquipmentEditorEvent

    /** [field] es `MIN_KG`, `MAX_KG` o `INCREMENT_KG`. */
    data class KgChanged(val field: EquipmentField, val value: String) : EquipmentEditorEvent

    /** Fin del formulario: añade o guarda, según cómo se abrió. */
    data object SaveClicked : EquipmentEditorEvent

    /** Quitar del gimnasio la máquina personalizada que se está editando. */
    data object RemoveClicked : EquipmentEditorEvent
}

sealed interface InventoryEffect {
    /** El formulario está listo para mostrarse. */
    data object EditorOpened : InventoryEffect

    /** El formulario terminó: se vuelve al inventario. */
    data object EditorClosed : InventoryEffect
}
