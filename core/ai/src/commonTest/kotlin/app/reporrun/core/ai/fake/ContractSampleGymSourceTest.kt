package app.reporrun.core.ai.fake

import app.reporrun.core.ai.mapper.toDto
import app.reporrun.core.domain.port.PlanGenerationRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContractSampleGymSourceTest {
    @Test
    fun sendsTheInventoryDaysMinutesAndCardioOfTheContractExample() {
        val sample = ContractSampleGymSource.sampleGym(GYM_ID)
        val example = FakeExamples.generateRequest

        val sent = PlanGenerationRequest(profile(), gym(), sample.equipment, sample.preferences)
            .toDto(promptVersion = example.promptVersion, locale = example.locale)

        assertEquals(example.gym.name, sample.name)
        assertEquals(example.gym.equipment, sent.gym.equipment)
        assertEquals(example.preferences.copy(startDate = null), sent.preferences)
    }

    @Test
    fun theInventoryHangsFromTheGivenGym() {
        val sample = ContractSampleGymSource.sampleGym(GYM_ID)

        assertEquals(setOf(GYM_ID), sample.equipment.map { it.gymId }.toSet())
        assertEquals(11, sample.equipment.size)
    }
}
