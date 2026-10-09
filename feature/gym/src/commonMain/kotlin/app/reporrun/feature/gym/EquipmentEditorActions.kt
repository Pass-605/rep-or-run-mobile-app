package app.reporrun.feature.gym

import app.reporrun.core.domain.usecase.AddEquipmentResult
import app.reporrun.core.domain.usecase.AddEquipmentUseCase
import app.reporrun.core.domain.usecase.EquipmentChanges
import app.reporrun.core.domain.usecase.EquipmentInput
import app.reporrun.core.domain.usecase.RemoveEquipmentResult
import app.reporrun.core.domain.usecase.RemoveEquipmentUseCase
import app.reporrun.core.domain.usecase.UpdateEquipmentResult
import app.reporrun.core.domain.usecase.UpdateEquipmentUseCase
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId

/**
 * Lo que hace el formulario de una máquina al terminar: llama al caso de uso que toca y traduce
 * su resultado. Cada función devuelve el formulario con lo que impidió terminar, o `null` si no
 * queda nada que hacer en él y se puede cerrar.
 *
 * Está aparte de [InventoryViewModel] para que este no cargue con tres traducciones de
 * resultados además de la lista.
 */
internal class EquipmentEditorActions(
    private val addEquipment: AddEquipmentUseCase,
    private val updateEquipment: UpdateEquipmentUseCase,
    private val removeEquipment: RemoveEquipmentUseCase,
) {
    /** Añade una máquina personalizada o guarda la que se edita, según cómo se abrió. */
    suspend fun save(gym: GymId, editor: EquipmentEditorState): EquipmentEditorState? =
        if (editor.equipmentId == null) add(gym, editor) else update(gym, editor.equipmentId, editor)

    /** Quita del gimnasio la máquina que se edita. Añadiendo no hay nada que quitar. */
    suspend fun remove(gym: GymId, editor: EquipmentEditorState): EquipmentEditorState? {
        val id = editor.equipmentId ?: return null
        return when (val result = removeEquipment(gym, id)) {
            RemoveEquipmentResult.Removed, RemoveEquipmentResult.EquipmentNotFound -> null

            // En uso por un plan: se dice en el formulario, no se calla.
            is RemoveEquipmentResult.InUse -> editor.copy(problem = EditorProblem.InUse(result.usages))
        }
    }

    private suspend fun add(gym: GymId, editor: EquipmentEditorState): EquipmentEditorState? {
        val input = EquipmentInput(
            name = editor.name,
            category = editor.category,
            loadType = editor.loadType,
            minKg = editor.minKg.toKgOrNull(),
            maxKg = editor.maxKg.toKgOrNull(),
            incrementKg = editor.incrementKg.toKgOrNull(),
        )
        return when (val result = addEquipment(gym, input)) {
            is AddEquipmentResult.Added -> null

            is AddEquipmentResult.Invalid -> editor.copy(errors = result.errors)

            is AddEquipmentResult.InventoryFull -> editor.copy(problem = EditorProblem.Full(result.max))

            // El gimnasio ya no existe: el formulario se cierra.
            AddEquipmentResult.GymNotFound -> null
        }
    }

    private suspend fun update(gym: GymId, id: EquipmentId, editor: EquipmentEditorState): EquipmentEditorState? {
        val changes = EquipmentChanges(
            name = editor.name,
            minKg = editor.minKg.toKgOrNull(),
            maxKg = editor.maxKg.toKgOrNull(),
            incrementKg = editor.incrementKg.toKgOrNull(),
        )
        return when (val result = updateEquipment(gym, id, changes)) {
            is UpdateEquipmentResult.Updated -> null

            is UpdateEquipmentResult.Invalid -> editor.copy(errors = result.errors)

            // La máquina ya no está: el formulario se cierra y la lista enseña lo que hay.
            UpdateEquipmentResult.EquipmentNotFound -> null
        }
    }
}
