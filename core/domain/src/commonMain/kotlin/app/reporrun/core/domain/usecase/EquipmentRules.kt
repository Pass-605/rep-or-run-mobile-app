package app.reporrun.core.domain.usecase

import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType

/**
 * Un aparato tal como sale del formulario, sin validar. [minKg], [maxKg] e [incrementKg] son
 * datos del aparato, no una prescripción (plan §4.4).
 */
data class EquipmentInput(
    val name: String,
    val category: EquipmentCategory,
    val loadType: LoadType,
    val minKg: Double? = null,
    val maxKg: Double? = null,
    val incrementKg: Double? = null,
    val notes: String? = null,
)

/** Campos de [EquipmentInput] que pueden llevar un error. */
enum class EquipmentField { NAME, MIN_KG, MAX_KG, INCREMENT_KG, NOTES }

/** Por qué no se puede añadir un aparato. */
sealed interface EquipmentError {
    /** Un campo fuera de los límites del contrato. */
    data class Field(val field: EquipmentField, val issue: FieldIssue) : EquipmentError

    /** La carga mínima es mayor que la máxima. */
    data object MinAboveMax : EquipmentError

    /** El gimnasio ya tiene un aparato con ese nombre, ignorando mayúsculas. */
    data object DuplicateName : EquipmentError
}

/** Límites del inventario de un gimnasio. */
object InventoryRules {
    /** Con menos aparatos no se puede generar un plan. */
    const val MIN_EQUIPMENT_TO_GENERATE: Int = 5

    /** Tope del inventario: el `maxItems` de `Gym.equipment` en el contrato. */
    const val MAX_EQUIPMENT: Int = 120
}

/** Recorta el nombre y las notas; unas notas en blanco son «sin notas». */
internal fun EquipmentInput.normalized(): EquipmentInput =
    copy(name = name.trim(), notes = notes?.trim()?.ifEmpty { null })

/**
 * Todos los errores de un aparato ya [normalized] que se quiere añadir a un gimnasio con ese
 * [inventory]. Vacía si se puede construir el modelo.
 */
internal fun EquipmentInput.errors(inventory: List<Equipment>): List<EquipmentError> {
    val fields = FieldChecks<EquipmentField>().apply {
        when {
            name.isEmpty() -> add(EquipmentField.NAME, FieldIssue.Required)

            name.length < Equipment.NAME_LENGTH.first ->
                add(EquipmentField.NAME, FieldIssue.TooShort(Equipment.NAME_LENGTH.first))

            else -> maxLength(EquipmentField.NAME, name, Equipment.NAME_LENGTH.last)
        }
        inRange(EquipmentField.MIN_KG, minKg, Equipment.MIN_KG_RANGE)
        inRange(EquipmentField.MAX_KG, maxKg, Equipment.MAX_KG_RANGE)
        if (incrementKg != null && !(incrementKg > 0.0 && incrementKg <= Equipment.MAX_INCREMENT_KG)) {
            val issue = FieldIssue.OutOfRange(min = 0.0, max = Equipment.MAX_INCREMENT_KG, minExclusive = true)
            add(EquipmentField.INCREMENT_KG, issue)
        }
        maxLength(EquipmentField.NOTES, notes, Equipment.MAX_NOTES_LENGTH)
    }.errors
    return buildList {
        fields.mapTo(this) { EquipmentError.Field(it.field, it.issue) }
        if (minKg != null && maxKg != null && minKg > maxKg) add(EquipmentError.MinAboveMax)
        if (inventory.any { it.name.equals(name, ignoreCase = true) }) add(EquipmentError.DuplicateName)
    }
}

/** Construye el modelo. Solo se llama con [errors] vacía. */
internal fun EquipmentInput.toEquipment(id: EquipmentId, gymId: GymId, catalogKey: CatalogKey?): Equipment = Equipment(
    id = id,
    gymId = gymId,
    catalogKey = catalogKey,
    name = name,
    category = category,
    loadType = loadType,
    minKg = minKg,
    maxKg = maxKg,
    incrementKg = incrementKg,
    notes = notes,
    isCustom = catalogKey == null,
)

internal fun IdGenerator.newEquipmentId(): EquipmentId = EquipmentId("eq_${newId()}")
