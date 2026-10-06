package app.reporrun.core.domain.usecase

import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InventoryUseCasesTest {
    private val repository = FakeGymRepository()
    private val catalog =
        FakeEquipmentCatalogRepository(listOf(catalogItem(), catalogItem("pec_deck", name = "Pec Deck")))
    private val observeGymInventory = ObserveGymInventoryUseCase(repository, catalog)
    private val updateEquipment = UpdateEquipmentUseCase(repository)
    private val removeEquipment = RemoveEquipmentUseCase(repository)

    private val custom = EquipmentId("eq_propio")
    private val fromCatalog = EquipmentId("eq_cp")

    @BeforeTest
    fun createGymWithTwoMachines() = runTest {
        repository.saveGym(gym(isActive = true))
        repository.saveEquipment(
            listOf(
                equipment("eq_propio", name = "Polea del fondo"),
                equipment("eq_cp", name = "Chest Press", catalogKey = "chest_press"),
            ),
        )
    }

    @Test
    fun observesTheGymWithTheCatalogAndItsOwnMachines() = runTest {
        repository.saveGym(gym(OTHER_GYM))
        repository.saveEquipment(listOf(equipment("eq_otro", gymId = OTHER_GYM)))

        val inventory = observeGymInventory(GYM_ID).first()

        assertEquals(GYM_ID, inventory?.gym?.id)
        assertEquals(listOf("chest_press", "pec_deck"), inventory?.catalog?.map { it.key.value })
        assertEquals(setOf(custom, fromCatalog), inventory?.equipment?.map { it.id }?.toSet())
    }

    @Test
    fun observesNothingForAGymThatDoesNotExist() = runTest {
        assertNull(observeGymInventory(GymId("gym_borrado")).first())
    }

    @Test
    fun loadingTheSeedAsksTheCatalogForIt() = runTest {
        val seeded = CountingCatalogRepository(inserted = 62)

        val inserted = LoadCatalogSeedUseCase(seeded)()

        assertEquals(62, inserted)
        assertEquals(1, seeded.loadCalls)
    }

    @Test
    fun updatesTheNameAndTheLoadRangeOfACustomMachine() = runTest {
        val changes = EquipmentChanges(name = "  Polea doble ", minKg = 5.0, maxKg = 90.0, incrementKg = 2.5)

        val updated = assertIs<UpdateEquipmentResult.Updated>(updateEquipment(GYM_ID, custom, changes)).equipment

        assertEquals("Polea doble", updated.name)
        assertEquals(5.0, updated.minKg)
        assertEquals(90.0, updated.maxKg)
        assertEquals(2.5, updated.incrementKg)
        assertEquals(updated, repository.savedEquipment.single { it.id == custom })
    }

    @Test
    fun keepsTheIdentityCategoryAndLoadTypeWhenUpdating() = runTest {
        val before = repository.savedEquipment.single { it.id == custom }

        val updated = assertIs<UpdateEquipmentResult.Updated>(
            updateEquipment(GYM_ID, custom, EquipmentChanges(name = "Polea doble")),
        ).equipment

        assertEquals(before.id, updated.id)
        assertEquals(before.gymId, updated.gymId)
        assertEquals(EquipmentCategory.MACHINE, updated.category)
        assertEquals(LoadType.SELECTOR, updated.loadType)
        assertTrue(updated.isCustom)
        assertEquals(2, repository.savedEquipment.size)
    }

    @Test
    fun aCatalogMachineKeepsTheCatalogNameAndKey() = runTest {
        val changes = EquipmentChanges(name = "Otro nombre", maxKg = 120.0)

        val updated = assertIs<UpdateEquipmentResult.Updated>(updateEquipment(GYM_ID, fromCatalog, changes)).equipment

        assertEquals("Chest Press", updated.name)
        assertEquals("chest_press", updated.catalogKey?.value)
        assertEquals(120.0, updated.maxKg)
        assertEquals(false, updated.isCustom)
    }

    @Test
    fun rejectsAMinimumAboveTheMaximumWithoutSaving() = runTest {
        val before = repository.savedEquipment

        val result =
            updateEquipment(GYM_ID, custom, EquipmentChanges(name = "Polea del fondo", minKg = 50.0, maxKg = 10.0))

        assertEquals(UpdateEquipmentResult.Invalid(listOf(EquipmentError.MinAboveMax)), result)
        assertEquals(before, repository.savedEquipment)
    }

    @Test
    fun rejectsTheNameOfAnotherMachineOfTheGym() = runTest {
        val result = updateEquipment(GYM_ID, custom, EquipmentChanges(name = "chest press"))

        assertEquals(UpdateEquipmentResult.Invalid(listOf(EquipmentError.DuplicateName)), result)
    }

    @Test
    fun keepingItsOwnNameIsNotADuplicate() = runTest {
        val result = updateEquipment(GYM_ID, custom, EquipmentChanges(name = "Polea del fondo", maxKg = 80.0))

        assertIs<UpdateEquipmentResult.Updated>(result)
    }

    @Test
    fun doesNotUpdateAMachineOfAnotherGym() = runTest {
        repository.saveGym(gym(OTHER_GYM))

        val result = updateEquipment(OTHER_GYM, custom, EquipmentChanges(name = "Polea doble"))

        assertEquals(UpdateEquipmentResult.EquipmentNotFound, result)
    }

    @Test
    fun removesAMachineThatNoPlanUses() = runTest {
        val result = removeEquipment(GYM_ID, custom)

        assertEquals(RemoveEquipmentResult.Removed, result)
        assertEquals(listOf(fromCatalog), repository.savedEquipment.map { it.id })
    }

    @Test
    fun keepsAMachineThatSomePlanUsesAndSaysHowManyTimes() = runTest {
        repository.usages[custom] = 2

        val result = removeEquipment(GYM_ID, custom)

        assertEquals(RemoveEquipmentResult.InUse(usages = 2), result)
        assertEquals(2, repository.savedEquipment.size)
    }

    @Test
    fun doesNotRemoveAMachineOfAnotherGym() = runTest {
        repository.saveGym(gym(OTHER_GYM))

        val result = removeEquipment(OTHER_GYM, custom)

        assertEquals(RemoveEquipmentResult.EquipmentNotFound, result)
        assertEquals(2, repository.savedEquipment.size)
    }

    private companion object {
        val OTHER_GYM = GymId("gym_norte")
    }
}
