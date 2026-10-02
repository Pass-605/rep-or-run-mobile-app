package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.usecase.InventoryRules.MAX_EQUIPMENT
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.first

/** Resultado de [ToggleEquipmentUseCase]. */
sealed interface ToggleEquipmentResult {
    /** El aparato no estaba y ahora está en el inventario como [equipment]. */
    data class Added(val equipment: Equipment) : ToggleEquipmentResult

    /** El aparato estaba y se quitó del inventario. */
    data class Removed(val equipmentId: EquipmentId) : ToggleEquipmentResult

    /**
     * El aparato sigue en el inventario: lo usan [usages] ejercicios, alternativas o propuestas
     * de algún plan, archivados incluidos. La versión anterior de un plan se conserva entera
     * (plan §4.12), así que se puede dejar fuera del plan siguiente, pero no borrar.
     */
    data class InUse(val equipmentId: EquipmentId, val usages: Int) : ToggleEquipmentResult

    /** El gimnasio ya no existe. */
    data object GymNotFound : ToggleEquipmentResult

    /** Esa clave no está en el catálogo, o su entrada no sirve como aparato. */
    data object CatalogItemUnavailable : ToggleEquipmentResult

    /** El gimnasio ya tiene [max] aparatos y no admite más. */
    data class InventoryFull(val max: Int) : ToggleEquipmentResult
}

/**
 * Marca o desmarca un aparato del catálogo en el inventario de un gimnasio: si el gimnasio ya
 * tiene uno con esa clave lo quita, y si no lo añade con el nombre, la categoría y el
 * `loadType` del catálogo.
 */
class ToggleEquipmentUseCase(
    private val gymRepository: GymRepository,
    private val catalogRepository: EquipmentCatalogRepository,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(gymId: GymId, catalogKey: CatalogKey): ToggleEquipmentResult =
        if (gymRepository.hasGym(gymId)) toggle(gymId, catalogKey) else ToggleEquipmentResult.GymNotFound

    private suspend fun toggle(gymId: GymId, catalogKey: CatalogKey): ToggleEquipmentResult {
        val inventory = gymRepository.observeEquipment(gymId).first()
        val existing = inventory.firstOrNull { it.catalogKey == catalogKey }
        return when {
            existing != null -> remove(existing.id)
            inventory.size >= MAX_EQUIPMENT -> ToggleEquipmentResult.InventoryFull(MAX_EQUIPMENT)
            else -> add(gymId, catalogKey)
        }
    }

    /** El repositorio ya dice si el aparato está en uso; aquí no sube ninguna excepción de SQLite. */
    private suspend fun remove(equipmentId: EquipmentId): ToggleEquipmentResult =
        when (val deletion = gymRepository.deleteEquipment(equipmentId)) {
            EquipmentDeletion.Deleted -> ToggleEquipmentResult.Removed(equipmentId)
            is EquipmentDeletion.InUse -> ToggleEquipmentResult.InUse(equipmentId, deletion.usages)
        }

    /**
     * El catálogo es de la app, pero se comprueba igual que un aparato tecleado: si una entrada
     * no cupiera en los límites de [Equipment], es un resultado y no una excepción. No se mira
     * el nombre repetido: las entradas del catálogo no se repiten entre sí.
     */
    private suspend fun add(gymId: GymId, catalogKey: CatalogKey): ToggleEquipmentResult {
        val input = catalogRepository.observeCatalog().first().firstOrNull { it.key == catalogKey }?.toInput()
        return if (input != null && input.errors(inventory = emptyList()).isEmpty()) {
            val equipment = input.toEquipment(ids.newEquipmentId(), gymId, catalogKey)
            gymRepository.saveEquipment(listOf(equipment))
            ToggleEquipmentResult.Added(equipment)
        } else {
            ToggleEquipmentResult.CatalogItemUnavailable
        }
    }

    private fun EquipmentCatalogItem.toInput(): EquipmentInput =
        EquipmentInput(name = name, category = category, loadType = loadType).normalized()
}
