package app.reporrun.core.data.mapper

import app.reporrun.core.database.entity.EquipmentCatalogEntity
import app.reporrun.core.database.entity.EquipmentEntity
import app.reporrun.core.database.entity.GymEntity
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId

/* Gimnasio, inventario y catálogo ↔ filas. */

internal fun Gym.toEntity() = GymEntity(
    id = id.value,
    name = name,
    isActive = isActive,
    notes = notes,
    createdAt = createdAt,
)

internal fun GymEntity.toModel() = Gym(
    id = GymId(id),
    name = name,
    isActive = isActive,
    notes = notes,
    createdAt = createdAt,
)

internal fun Equipment.toEntity() = EquipmentEntity(
    id = id.value,
    gymId = gymId.value,
    catalogKey = catalogKey?.value,
    name = name,
    category = category,
    loadType = loadType,
    minKg = minKg,
    maxKg = maxKg,
    incrementKg = incrementKg,
    notes = notes,
    isCustom = isCustom,
)

internal fun EquipmentEntity.toModel() = Equipment(
    id = EquipmentId(id),
    gymId = GymId(gymId),
    catalogKey = catalogKey?.let(::CatalogKey),
    name = name,
    category = category,
    loadType = loadType,
    minKg = minKg,
    maxKg = maxKg,
    incrementKg = incrementKg,
    notes = notes,
    isCustom = isCustom,
)

internal fun EquipmentCatalogItem.toEntity() = EquipmentCatalogEntity(
    key = key.value,
    name = name,
    aliases = aliases,
    category = category,
    loadType = loadType,
    primaryMuscles = primaryMuscles,
)

internal fun EquipmentCatalogEntity.toModel() = EquipmentCatalogItem(
    key = CatalogKey(key),
    name = name,
    aliases = aliases,
    category = category,
    loadType = loadType,
    primaryMuscles = primaryMuscles,
)
