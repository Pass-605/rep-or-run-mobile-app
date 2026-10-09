package app.reporrun.core.domain.usecase

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveCardioOptionsUseCaseTest {
    private val repository = FakeGymRepository()
    private val observeCardioOptions = ObserveCardioOptionsUseCase(repository)

    private fun cardio(id: String, name: String, gymId: GymId = GYM_ID) =
        equipment(id, gymId, name).copy(category = EquipmentCategory.CARDIO)

    private suspend fun activeGymWith(vararg equipment: Equipment) {
        repository.saveGym(gym(isActive = true))
        repository.saveEquipment(equipment.toList())
    }

    @Test
    fun offersOnlyTheCardioEquipmentOfTheActiveGymInAlphabeticalOrder() = runTest {
        activeGymWith(cardio("eq_1", "Treadmill"), equipment("eq_2", name = "Pec Deck"), cardio("eq_3", "Bike"))

        assertEquals(listOf("Bike", "Treadmill"), observeCardioOptions().first())
    }

    @Test
    fun offersNothingWithoutAnActiveGym() = runTest {
        repository.saveGym(gym(isActive = false))
        repository.saveEquipment(listOf(cardio("eq_1", "Treadmill")))

        assertEquals(emptyList(), observeCardioOptions().first())
    }

    @Test
    fun ignoresTheCardioEquipmentOfAnotherGym() = runTest {
        activeGymWith(cardio("eq_1", "Treadmill"))
        repository.saveEquipment(listOf(cardio("eq_2", "Rower", gymId = GymId("gym_norte"))))

        assertEquals(listOf("Treadmill"), observeCardioOptions().first())
    }

    @Test
    fun offersARepeatedNameOnce() = runTest {
        activeGymWith(cardio("eq_1", "Treadmill"), cardio("eq_2", "treadmill"))

        assertEquals(1, observeCardioOptions().first().size)
    }

    @Test
    fun trimsANameLongerThanACardioModeAllows() = runTest {
        activeGymWith(cardio("eq_1", "A".repeat(BlockPreferences.MAX_CARDIO_MODE_LENGTH + 10)))

        assertEquals(listOf("A".repeat(BlockPreferences.MAX_CARDIO_MODE_LENGTH)), observeCardioOptions().first())
    }
}
