package app.reporrun.core.ai.fake

import app.reporrun.core.ai.contract.EquipmentDto
import app.reporrun.core.domain.port.SampleGym
import app.reporrun.core.domain.port.SampleGymSource
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit

/**
 * El gimnasio de `generate-request.small-gym.json`, la copia del contrato del repo: es una
 * **petición** de ejemplo, sintética. Con ella el runner de la Lambda en modo fake responde
 * `200`, porque su plan de ejemplo nombra estos aparatos, estos días y estos minutos.
 *
 * Los aparatos conservan su `id` del ejemplo. La fecha de inicio no se copia: la del ejemplo
 * ya pasó. Las preferencias usan el id de la única fila que hay (`SavePreferencesUseCase`).
 */
object ContractSampleGymSource : SampleGymSource {
    override fun sampleGym(gymId: GymId): SampleGym {
        val example = FakeExamples.generateRequest
        val preferences = example.preferences
        return SampleGym(
            name = example.gym.name,
            equipment = example.gym.equipment.map { it.toEquipment(gymId) },
            preferences = BlockPreferences(
                id = BlockPreferencesId(SINGLE_PREFERENCES_ID),
                availableDays = preferences.availableDays.map { DayOfWeek.valueOf(it.name) }.toSet(),
                sessionMinutes = preferences.sessionMinutes,
                blockDays = preferences.blockDays,
                startDate = null,
                cardioModes = preferences.cardioModes,
                avoidExercises = preferences.avoidExercises,
                unit = LoadUnit.valueOf(preferences.unit.name),
            ),
        )
    }

    private fun EquipmentDto.toEquipment(gymId: GymId) = Equipment(
        id = EquipmentId(id),
        gymId = gymId,
        catalogKey = null,
        name = name,
        category = EquipmentCategory.valueOf(category.name),
        loadType = LoadType.valueOf(loadType.name),
        minKg = minKg,
        maxKg = maxKg,
        incrementKg = incrementKg,
        notes = notes,
        isCustom = true,
    )

    private const val SINGLE_PREFERENCES_ID = "preferences"
}
