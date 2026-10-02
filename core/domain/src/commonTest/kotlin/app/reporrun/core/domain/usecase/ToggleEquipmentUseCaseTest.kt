package app.reporrun.core.domain.usecase

import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ToggleEquipmentUseCaseTest {
    private val repository = FakeGymRepository()
    private val catalog = FakeEquipmentCatalogRepository(listOf(catalogItem(), catalogItem("x", name = "X")))
    private val toggleEquipment = ToggleEquipmentUseCase(repository, catalog, SequentialIds())

    private val chestPress = CatalogKey("chest_press")

    @BeforeTest
    fun createGym() = runTest {
        repository.saveGym(gym(isActive = true))
    }

    @Test
    fun addsTheCatalogItemWhenTheGymDoesNotHaveIt() = runTest {
        val added = assertIs<ToggleEquipmentResult.Added>(toggleEquipment(GYM_ID, chestPress)).equipment

        assertEquals(EquipmentId("eq_id1"), added.id)
        assertEquals(chestPress, added.catalogKey)
        assertEquals("Chest Press", added.name)
        assertEquals(EquipmentCategory.MACHINE, added.category)
        assertEquals(LoadType.SELECTOR, added.loadType)
        assertEquals(false, added.isCustom)
        assertEquals(listOf(added), repository.savedEquipment)
    }

    @Test
    fun removesTheMachineWhenTheGymAlreadyHasIt() = runTest {
        repository.saveEquipment(listOf(equipment("eq_cp", catalogKey = "chest_press")))

        val result = toggleEquipment(GYM_ID, chestPress)

        assertEquals(ToggleEquipmentResult.Removed(EquipmentId("eq_cp")), result)
        assertTrue(repository.savedEquipment.isEmpty())
    }

    @Test
    fun keepsAMachineThatSomePlanUsesAndSaysHowManyTimes() = runTest {
        repository.saveEquipment(listOf(equipment("eq_cp", catalogKey = "chest_press")))
        repository.usages[EquipmentId("eq_cp")] = 3

        val result = toggleEquipment(GYM_ID, chestPress)

        assertEquals(ToggleEquipmentResult.InUse(EquipmentId("eq_cp"), usages = 3), result)
        assertEquals(1, repository.savedEquipment.size)
    }

    @Test
    fun togglesOnlyTheGymItIsAskedFor() = runTest {
        repository.saveGym(gym(GymId("gym_norte")))
        repository.saveEquipment(listOf(equipment("eq_cp", GymId("gym_norte"), catalogKey = "chest_press")))

        val result = toggleEquipment(GYM_ID, chestPress)

        assertIs<ToggleEquipmentResult.Added>(result)
        assertEquals(2, repository.savedEquipment.size)
    }

    @Test
    fun rejectsAKeyThatIsNotInTheCatalog() = runTest {
        val result = toggleEquipment(GYM_ID, CatalogKey("leg_press"))

        assertEquals(ToggleEquipmentResult.CatalogItemUnavailable, result)
        assertTrue(repository.savedEquipment.isEmpty())
    }

    @Test
    fun rejectsACatalogItemThatDoesNotFitTheMachineLimitsWithoutThrowing() = runTest {
        val result = toggleEquipment(GYM_ID, CatalogKey("x"))

        assertEquals(ToggleEquipmentResult.CatalogItemUnavailable, result)
    }

    @Test
    fun rejectsAGymThatDoesNotExist() = runTest {
        val result = toggleEquipment(GymId("gym_borrado"), chestPress)

        assertEquals(ToggleEquipmentResult.GymNotFound, result)
    }

    @Test
    fun rejectsOneMoreThanTheInventoryLimit() = runTest {
        repository.saveEquipment(inventory(InventoryRules.MAX_EQUIPMENT))

        val result = toggleEquipment(GYM_ID, chestPress)

        assertEquals(ToggleEquipmentResult.InventoryFull(120), result)
    }

    @Test
    fun stillRemovesFromAFullInventory() = runTest {
        repository.saveEquipment(
            inventory(InventoryRules.MAX_EQUIPMENT - 1) + equipment("eq_cp", catalogKey = "chest_press"),
        )

        val result = toggleEquipment(GYM_ID, chestPress)

        assertEquals(ToggleEquipmentResult.Removed(EquipmentId("eq_cp")), result)
    }
}
