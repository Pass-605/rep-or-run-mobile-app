package app.reporrun.core.ai.mapper

import app.reporrun.core.ai.contract.BlockPreferencesDto
import app.reporrun.core.ai.contract.CONTRACT_SCHEMA_VERSION
import app.reporrun.core.ai.contract.DayOfWeekDto
import app.reporrun.core.ai.contract.EquipmentCategoryDto
import app.reporrun.core.ai.contract.EquipmentDto
import app.reporrun.core.ai.contract.ExperienceDto
import app.reporrun.core.ai.contract.GeneratePlanRequest
import app.reporrun.core.ai.contract.GoalDto
import app.reporrun.core.ai.contract.GymDto
import app.reporrun.core.ai.contract.LoadTypeDto
import app.reporrun.core.ai.contract.MuscleGroupDto
import app.reporrun.core.ai.contract.ProfileDto
import app.reporrun.core.ai.contract.UnitDto
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.UserProfile

/*
 * Dominio → petición del contrato. Los enums se cruzan por nombre: los de `:core:model`
 * coinciden a propósito con los del contrato, y un test recorre todos los valores para que un
 * renombrado no pase desapercibido.
 */

/**
 * El cuerpo de `POST /v1/plans:generate` para esta petición. Lo que en el dominio es un
 * conjunto viaja ordenado, así la misma petición da siempre el mismo cuerpo.
 */
internal fun PlanGenerationRequest.toDto(promptVersion: String, locale: String) = GeneratePlanRequest(
    schemaVersion = CONTRACT_SCHEMA_VERSION,
    promptVersion = promptVersion,
    locale = locale,
    profile = profile.toDto(),
    gym = GymDto(name = gym.name, equipment = equipment.map { it.toDto() }),
    preferences = preferences.toDto(),
)

private fun UserProfile.toDto() = ProfileDto(
    alias = alias,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    waistCm = waistCm,
    hipCm = hipCm,
    experience = ExperienceDto.valueOf(experience.name),
    goal = GoalDto.valueOf(goal.name),
    priorityMuscles = priorityMuscles.sorted().map { MuscleGroupDto.valueOf(it.name) },
    limitations = limitations,
    preferences = preferences,
)

private fun Equipment.toDto() = EquipmentDto(
    id = id.value,
    name = name,
    category = EquipmentCategoryDto.valueOf(category.name),
    loadType = LoadTypeDto.valueOf(loadType.name),
    minKg = minKg,
    maxKg = maxKg,
    incrementKg = incrementKg,
    notes = notes,
)

private fun BlockPreferences.toDto() = BlockPreferencesDto(
    availableDays = availableDays.sorted().map { DayOfWeekDto.valueOf(it.name) },
    sessionMinutes = sessionMinutes,
    blockDays = blockDays,
    startDate = startDate?.toString(),
    cardioModes = cardioModes,
    avoidExercises = avoidExercises,
    unit = UnitDto.valueOf(unit.name),
)
