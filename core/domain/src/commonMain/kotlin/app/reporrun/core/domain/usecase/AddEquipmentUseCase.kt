package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.usecase.InventoryRules.MAX_EQUIPMENT
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.first

/** Resultado de [AddEquipmentUseCase]. */
sealed interface AddEquipmentResult {
    /** El aparato está en el inventario como [equipment]. */
    data class Added(val equipment: Equipment) : AddEquipmentResult

    /** No se añadió nada. [errors] trae todo lo que falla. */
    data class Invalid(val errors: List<EquipmentError>) : AddEquipmentResult

    /** El gimnasio ya no existe. */
    data object GymNotFound : AddEquipmentResult

    /** El gimnasio ya tiene [max] aparatos y no admite más. */
    data class InventoryFull(val max: Int) : AddEquipmentResult
}

/**
 * Añade al inventario de un gimnasio un aparato que no está en el catálogo. Para los del
 * catálogo, [ToggleEquipmentUseCase].
 */
class AddEquipmentUseCase(private val repository: GymRepository, private val ids: IdGenerator) {
    suspend operator fun invoke(gymId: GymId, input: EquipmentInput): AddEquipmentResult =
        if (repository.hasGym(gymId)) add(gymId, input.normalized()) else AddEquipmentResult.GymNotFound

    private suspend fun add(gymId: GymId, input: EquipmentInput): AddEquipmentResult {
        val inventory = repository.observeEquipment(gymId).first()
        val errors = input.errors(inventory)
        return when {
            errors.isNotEmpty() -> AddEquipmentResult.Invalid(errors)

            inventory.size >= MAX_EQUIPMENT -> AddEquipmentResult.InventoryFull(MAX_EQUIPMENT)

            else -> {
                val equipment = input.toEquipment(ids.newEquipmentId(), gymId, catalogKey = null)
                repository.saveEquipment(listOf(equipment))
                AddEquipmentResult.Added(equipment)
            }
        }
    }
}
