package app.reporrun.core.model

import kotlin.time.Instant

/** Gimnasio de la persona. Solo uno está activo, y su inventario es el que usa el plan. */
data class Gym(val id: GymId, val name: String, val isActive: Boolean, val notes: String, val createdAt: Instant) {
    init {
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) { "name vacío o demasiado largo" }
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 80
    }
}

/**
 * Aparato del inventario de un gimnasio. Todo ejercicio del plan apunta a uno por
 * [EquipmentId] (plan §4.1); nunca se cruza por nombre.
 *
 * [loadType] fija cómo se lee la carga (plan §4.5). [minKg], [maxKg] e [incrementKg] son
 * datos del aparato, no una prescripción.
 *
 * @property catalogKey entrada del catálogo de la que salió; `null` si la persona lo creó.
 * @property isCustom si lo creó la persona en vez de marcarlo desde el catálogo.
 */
data class Equipment(
    val id: EquipmentId,
    val gymId: GymId,
    val catalogKey: CatalogKey?,
    val name: String,
    val category: EquipmentCategory,
    val loadType: LoadType,
    val minKg: Double?,
    val maxKg: Double?,
    val incrementKg: Double?,
    val notes: String?,
    val isCustom: Boolean,
) {
    init {
        require(name.length in NAME_LENGTH && name.isNotBlank()) { "name fuera de $NAME_LENGTH caracteres" }
        require(minKg == null || minKg in MIN_KG_RANGE) { "minKg fuera de $MIN_KG_RANGE" }
        require(maxKg == null || maxKg in MAX_KG_RANGE) { "maxKg fuera de $MAX_KG_RANGE" }
        require(minKg == null || maxKg == null || minKg <= maxKg) { "minKg > maxKg" }
        require(incrementKg == null || (incrementKg > 0.0 && incrementKg <= MAX_INCREMENT_KG)) {
            "incrementKg debe ser > 0 y <= $MAX_INCREMENT_KG"
        }
        require(notes == null || notes.length <= MAX_NOTES_LENGTH) { "notes demasiado largo" }
    }

    /** Límites del contrato (`Equipment` en `common.schema.json`). */
    companion object {
        val NAME_LENGTH: IntRange = 2..80
        val MIN_KG_RANGE: ClosedFloatingPointRange<Double> = 0.0..500.0
        val MAX_KG_RANGE: ClosedFloatingPointRange<Double> = 0.0..1000.0
        const val MAX_INCREMENT_KG: Double = 50.0
        const val MAX_NOTES_LENGTH: Int = 200
    }
}

/**
 * Entrada del catálogo semilla (`seed/equipment_catalog.json`), de solo lectura. La persona
 * marca las que tiene su gimnasio y se convierten en [Equipment].
 *
 * @property name nombre visible, en español. Es el que toma el [Equipment] al marcarla.
 * @property aliases otros nombres por los que se busca: los alias en español y el nombre en
 *   inglés, como lo rotula el gimnasio.
 */
data class EquipmentCatalogItem(
    val key: CatalogKey,
    val name: String,
    val aliases: List<String>,
    val category: EquipmentCategory,
    val loadType: LoadType,
    val primaryMuscles: Set<MuscleGroup>,
) {
    init {
        require(name.isNotBlank()) { "name vacío" }
        require(aliases.none { it.isBlank() }) { "alias vacío" }
    }
}
