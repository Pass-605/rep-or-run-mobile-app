package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Lo que hace falta para pintar el inventario de un gimnasio: el gimnasio, el catálogo del que
 * se marca y los aparatos que ya tiene.
 *
 * Un aparato de [equipment] corresponde a una entrada de [catalog] cuando su
 * [Equipment.catalogKey] es la [EquipmentCatalogItem.key] de esa entrada. **Nunca por el
 * nombre**: el nombre se puede repetir o cambiar, y la clave es la que cruza el inventario con
 * los ejercicios de un plan.
 */
data class GymInventory(val gym: Gym, val catalog: List<EquipmentCatalogItem>, val equipment: List<Equipment>)

/** El inventario de un gimnasio, al día. Emite `null` si ese gimnasio no existe o deja de existir. */
class ObserveGymInventoryUseCase(
    private val gymRepository: GymRepository,
    private val catalogRepository: EquipmentCatalogRepository,
) {
    operator fun invoke(gymId: GymId): Flow<GymInventory?> = combine(
        gymRepository.observeGyms(),
        catalogRepository.observeCatalog(),
        gymRepository.observeEquipment(gymId),
    ) { gyms, catalog, equipment ->
        gyms.firstOrNull { it.id == gymId }?.let { gym -> GymInventory(gym, catalog, equipment) }
    }
}

/**
 * Carga el catálogo semilla. Se llama en cada arranque: es idempotente, inserta las claves que
 * falten y pone al día el texto de las demás. Sin esta llamada el catálogo está vacío y el
 * inventario no tiene de dónde marcar.
 */
class LoadCatalogSeedUseCase(private val repository: EquipmentCatalogRepository) {
    /** @return cuántas entradas nuevas se insertaron. */
    suspend operator fun invoke(): Int = repository.loadSeed()
}

/**
 * Lo que se puede cambiar de un aparato que ya está en el inventario. [minKg], [maxKg] e
 * [incrementKg] son datos del aparato, no una prescripción (plan §4.4).
 *
 * No están la categoría ni el `loadType`: ver [UpdateEquipmentUseCase].
 */
data class EquipmentChanges(
    val name: String,
    val minKg: Double? = null,
    val maxKg: Double? = null,
    val incrementKg: Double? = null,
)

/** Resultado de [UpdateEquipmentUseCase]. */
sealed interface UpdateEquipmentResult {
    /** El aparato quedó guardado como [equipment], con el mismo id. */
    data class Updated(val equipment: Equipment) : UpdateEquipmentResult

    /** No se guardó nada. [errors] trae todo lo que falla. */
    data class Invalid(val errors: List<EquipmentError>) : UpdateEquipmentResult

    /** Ese aparato ya no está en el inventario de ese gimnasio. */
    data object EquipmentNotFound : UpdateEquipmentResult
}

/**
 * Cambia el rango de carga y el incremento de un aparato del inventario y, si es uno
 * personalizado, también su nombre. Un aparato del catálogo conserva el nombre del catálogo.
 *
 * **La categoría y el `loadType` no se cambian.** El `loadType` decide cómo se leen los kilos
 * de un plan (plan §4.5), y el aparato puede estar en planes ya guardados: cambiarlo
 * reinterpretaría sus cargas sin que nadie lo aprobara. Para corregirlo hay que quitar el
 * aparato y añadirlo de nuevo, y quitarlo ya está protegido si algún plan lo usa.
 *
 * El id, el gimnasio y la clave del catálogo se conservan: los ejercicios siguen apuntando al
 * mismo aparato.
 */
class UpdateEquipmentUseCase(private val repository: GymRepository) {
    suspend operator fun invoke(
        gymId: GymId,
        equipmentId: EquipmentId,
        changes: EquipmentChanges,
    ): UpdateEquipmentResult {
        val inventory = repository.observeEquipment(gymId).first()
        val current = inventory.firstOrNull { it.id == equipmentId }
        return if (current == null) UpdateEquipmentResult.EquipmentNotFound else update(current, changes, inventory)
    }

    private suspend fun update(
        current: Equipment,
        changes: EquipmentChanges,
        inventory: List<Equipment>,
    ): UpdateEquipmentResult {
        val input = EquipmentInput(
            name = if (current.isCustom) changes.name else current.name,
            category = current.category,
            loadType = current.loadType,
            minKg = changes.minKg,
            maxKg = changes.maxKg,
            incrementKg = changes.incrementKg,
            notes = current.notes,
        ).normalized()
        // El propio aparato no cuenta como nombre repetido.
        val errors = input.errors(inventory - current)
        return if (errors.isEmpty()) {
            val updated = input.toEquipment(current.id, current.gymId, current.catalogKey)
            repository.saveEquipment(listOf(updated))
            UpdateEquipmentResult.Updated(updated)
        } else {
            UpdateEquipmentResult.Invalid(errors)
        }
    }
}

/** Resultado de [RemoveEquipmentUseCase]. */
sealed interface RemoveEquipmentResult {
    /** El aparato ya no está en el inventario. */
    data object Removed : RemoveEquipmentResult

    /**
     * El aparato sigue en el inventario: lo usan [usages] ejercicios, alternativas o propuestas
     * de algún plan, archivados incluidos (plan §4.12).
     */
    data class InUse(val usages: Int) : RemoveEquipmentResult

    /** Ese aparato ya no está en el inventario de ese gimnasio. */
    data object EquipmentNotFound : RemoveEquipmentResult
}

/**
 * Quita un aparato del inventario por su id. Es la baja de los aparatos personalizados, que no
 * tienen clave de catálogo; los del catálogo se desmarcan con [ToggleEquipmentUseCase].
 */
class RemoveEquipmentUseCase(private val repository: GymRepository) {
    suspend operator fun invoke(gymId: GymId, equipmentId: EquipmentId): RemoveEquipmentResult {
        val exists = repository.observeEquipment(gymId).first().any { it.id == equipmentId }
        return if (exists) remove(equipmentId) else RemoveEquipmentResult.EquipmentNotFound
    }

    private suspend fun remove(equipmentId: EquipmentId): RemoveEquipmentResult =
        when (val deletion = repository.deleteEquipment(equipmentId)) {
            EquipmentDeletion.Deleted -> RemoveEquipmentResult.Removed
            is EquipmentDeletion.InUse -> RemoveEquipmentResult.InUse(deletion.usages)
        }
}
