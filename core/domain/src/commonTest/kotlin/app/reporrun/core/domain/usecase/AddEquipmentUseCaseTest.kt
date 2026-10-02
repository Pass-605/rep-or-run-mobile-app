package app.reporrun.core.domain.usecase

import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddEquipmentUseCaseTest {
    private val repository = FakeGymRepository()
    private val addEquipment = AddEquipmentUseCase(repository, SequentialIds())

    @BeforeTest
    fun createGym() = runTest {
        repository.saveGym(gym(isActive = true))
    }

    private suspend fun errorsOf(input: EquipmentInput): List<EquipmentError> =
        assertIs<AddEquipmentResult.Invalid>(addEquipment(GYM_ID, input)).errors

    @Test
    fun addsACustomMachineToTheInventory() = runTest {
        val input = equipmentInput("  Polea alta  ").copy(minKg = 5.0, maxKg = 100.0, incrementKg = 2.5, notes = "  ")

        val added = assertIs<AddEquipmentResult.Added>(addEquipment(GYM_ID, input)).equipment

        assertEquals(EquipmentId("eq_id1"), added.id)
        assertEquals("Polea alta", added.name)
        assertTrue(added.isCustom)
        assertNull(added.catalogKey)
        assertNull(added.notes)
        assertEquals(listOf(added), repository.savedEquipment)
    }

    @Test
    fun rejectsABlankName() = runTest {
        val errors = errorsOf(equipmentInput(" "))

        assertEquals(listOf(EquipmentError.Field(EquipmentField.NAME, FieldIssue.Required)), errors)
    }

    @Test
    fun rejectsANameThatIsTooShort() = runTest {
        val errors = errorsOf(equipmentInput("P"))

        assertEquals(listOf(EquipmentError.Field(EquipmentField.NAME, FieldIssue.TooShort(2))), errors)
    }

    @Test
    fun rejectsANameThatIsTooLong() = runTest {
        val errors = errorsOf(equipmentInput("p".repeat(81)))

        assertEquals(listOf(EquipmentError.Field(EquipmentField.NAME, FieldIssue.TooLong(80))), errors)
    }

    @Test
    fun rejectsLoadsOutOfRange() = runTest {
        val errors = errorsOf(equipmentInput().copy(minKg = -1.0, maxKg = 1000.5))

        assertEquals(
            listOf(
                EquipmentError.Field(EquipmentField.MIN_KG, FieldIssue.OutOfRange(0.0, 500.0)),
                EquipmentError.Field(EquipmentField.MAX_KG, FieldIssue.OutOfRange(0.0, 1000.0)),
            ),
            errors,
        )
    }

    @Test
    fun rejectsAMinimumAboveTheMaximum() = runTest {
        val errors = errorsOf(equipmentInput().copy(minKg = 50.0, maxKg = 40.0))

        assertEquals(listOf<EquipmentError>(EquipmentError.MinAboveMax), errors)
    }

    @Test
    fun rejectsAnIncrementThatIsZeroOrTooBig() = runTest {
        val expected = EquipmentError.Field(
            EquipmentField.INCREMENT_KG,
            FieldIssue.OutOfRange(min = 0.0, max = 50.0, minExclusive = true),
        )

        assertEquals(listOf<EquipmentError>(expected), errorsOf(equipmentInput().copy(incrementKg = 0.0)))
        assertEquals(listOf<EquipmentError>(expected), errorsOf(equipmentInput().copy(incrementKg = 50.5)))
        assertEquals(listOf<EquipmentError>(expected), errorsOf(equipmentInput().copy(incrementKg = Double.NaN)))
    }

    @Test
    fun rejectsNotesThatAreTooLong() = runTest {
        val errors = errorsOf(equipmentInput().copy(notes = "n".repeat(201)))

        assertEquals(listOf(EquipmentError.Field(EquipmentField.NOTES, FieldIssue.TooLong(200))), errors)
    }

    @Test
    fun rejectsANameAlreadyInTheGymIgnoringCase() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Polea Alta")))

        val errors = errorsOf(equipmentInput("polea alta"))

        assertEquals(listOf<EquipmentError>(EquipmentError.DuplicateName), errors)
    }

    @Test
    fun allowsTheSameNameInAnotherGym() = runTest {
        repository.saveGym(gym(GymId("gym_norte")))
        repository.saveEquipment(listOf(equipment("eq_1", GymId("gym_norte"), name = "Polea alta")))

        assertIs<AddEquipmentResult.Added>(addEquipment(GYM_ID, equipmentInput("Polea alta")))
    }

    @Test
    fun reportsEveryErrorAtOnce() = runTest {
        val errors = errorsOf(equipmentInput("P").copy(incrementKg = -1.0, notes = "n".repeat(201)))

        assertEquals(3, errors.size)
    }

    @Test
    fun rejectsAGymThatDoesNotExist() = runTest {
        val result = addEquipment(GymId("gym_borrado"), equipmentInput())

        assertEquals(AddEquipmentResult.GymNotFound, result)
        assertTrue(repository.savedEquipment.isEmpty())
    }

    @Test
    fun rejectsOneMoreThanTheInventoryLimit() = runTest {
        repository.saveEquipment(inventory(InventoryRules.MAX_EQUIPMENT))

        val result = addEquipment(GYM_ID, equipmentInput())

        assertEquals(AddEquipmentResult.InventoryFull(120), result)
        assertEquals(120, repository.savedEquipment.size)
    }
}
