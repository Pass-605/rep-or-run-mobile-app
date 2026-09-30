package app.reporrun.core.data.mapper

import app.reporrun.core.database.entity.BlockPreferencesEntity
import app.reporrun.core.database.entity.UserProfileEntity
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile

/*
 * Perfil y preferencias ↔ filas. Solo los escribe la app desde modelos ya válidos, así que la
 * lectura construye directamente: si una fila no cumple el modelo es un fallo nuestro y debe
 * notarse, no taparse.
 */

internal fun UserProfile.toEntity() = UserProfileEntity(
    id = id.value,
    alias = alias,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    waistCm = waistCm,
    hipCm = hipCm,
    experience = experience,
    goal = goal,
    priorityMuscles = priorityMuscles,
    limitations = limitations,
    preferences = preferences,
    updatedAt = updatedAt,
)

internal fun UserProfileEntity.toModel() = UserProfile(
    id = ProfileId(id),
    alias = alias,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    waistCm = waistCm,
    hipCm = hipCm,
    experience = experience,
    goal = goal,
    priorityMuscles = priorityMuscles,
    limitations = limitations,
    preferences = preferences,
    updatedAt = updatedAt,
)

internal fun BlockPreferences.toEntity() = BlockPreferencesEntity(
    id = id.value,
    availableDays = availableDays,
    sessionMinutes = sessionMinutes,
    blockDays = blockDays,
    startDate = startDate,
    cardioModes = cardioModes,
    avoidExercises = avoidExercises,
    unit = unit,
)

internal fun BlockPreferencesEntity.toModel() = BlockPreferences(
    id = BlockPreferencesId(id),
    availableDays = availableDays,
    sessionMinutes = sessionMinutes,
    blockDays = blockDays,
    startDate = startDate,
    cardioModes = cardioModes,
    avoidExercises = avoidExercises,
    unit = unit,
)
