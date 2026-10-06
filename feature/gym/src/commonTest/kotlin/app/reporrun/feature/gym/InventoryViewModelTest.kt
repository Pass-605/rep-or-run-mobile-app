package app.reporrun.feature.gym

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.AddEquipmentUseCase
import app.reporrun.core.domain.usecase.EquipmentError
import app.reporrun.core.domain.usecase.EquipmentField
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.ObserveGymInventoryUseCase
import app.reporrun.core.domain.usecase.RemoveEquipmentUseCase
import app.reporrun.core.domain.usecase.ToggleEquipmentUseCase
import app.reporrun.core.domain.usecase.UpdateEquipmentUseCase
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.LoadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Como `GymViewModelTest`: casos de uso reales, repositorios fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {
    private var repository = FakeGymRepository(twoGyms())
    private var catalog = smallCatalog()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel(): InventoryViewModel {
        val catalogRepository = FakeEquipmentCatalogRepository(catalog)
        val ids = SequentialIds()
        return InventoryViewModel(
            observeGymInventory = ObserveGymInventoryUseCase(repository, catalogRepository),
            toggleEquipment = ToggleEquipmentUseCase(repository, catalogRepository, ids),
            addEquipment = AddEquipmentUseCase(repository, ids),
            updateEquipment = UpdateEquipmentUseCase(repository),
            removeEquipment = RemoveEquipmentUseCase(repository),
        )
    }

    /** El inventario del gimnasio central, ya abierto. */
    private fun opened() = viewModel().apply { onEvent(InventoryEvent.Opened(CENTRAL)) }

    private fun InventoryViewModel.items() = state.value.rows.filterIsInstance<InventoryRow.Item>()

    private fun InventoryViewModel.names() = items().map { it.name }

    private fun InventoryViewModel.item(name: String) = items().single { it.name == name }

    private fun InventoryViewModel.toggle(name: String) = onEvent(InventoryEvent.ItemToggled(item(name).key))

    @Test
    fun staysLoadingUntilItKnowsWhichGymToShow() = runTest {
        val viewModel = viewModel()

        assertEquals(InventoryUiState(), viewModel.state.value)
    }

    @Test
    fun staysLoadingUntilStorageAnswers() = runTest {
        repository = FakeGymRepository(initial = null)
        val viewModel = opened()
        assertTrue(viewModel.state.value.isLoading)

        repository.saveGym(twoGyms().first())

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Gimnasio Central", viewModel.state.value.gymName)
    }

    @Test
    fun groupsTheCatalogByCategoryWithAHeaderEachAndNamesInOrder() = runTest {
        val viewModel = opened()

        val shown = viewModel.state.value.rows.map { row ->
            when (row) {
                is InventoryRow.Header -> "[${row.category}]"
                is InventoryRow.Item -> row.name
            }
        }
        assertEquals(
            listOf("[MACHINE]", "Chest Press", "Pec Deck", "[CABLE]", "Cable Tower", "[CARDIO]", "Treadmill"),
            shown,
        )
        assertEquals(
            listOf(EquipmentCategory.MACHINE, EquipmentCategory.CABLE, EquipmentCategory.CARDIO),
            viewModel.state.value.categories,
        )
    }

    @Test
    fun marksWhatTheGymHasByCatalogKeyNotByName() = runTest {
        repository.saveEquipment(
            listOf(
                // La clave manda: tiene el Pec Deck aunque el nombre guardado sea otro...
                equipment("eq_1", name = "Contractor viejo", catalogKey = "pec_deck"),
                // ...y una máquina propia que se llama igual que una del catálogo no la marca.
                equipment("eq_2", name = "Treadmill", category = EquipmentCategory.CARDIO),
            ),
        )

        val viewModel = opened()

        assertEquals(EquipmentId("eq_1"), viewModel.item("Pec Deck").equipmentId)
        val treadmills = viewModel.items().filter { it.name == "Treadmill" }
        assertEquals(listOf(null, EquipmentId("eq_2")), treadmills.map { it.equipmentId }.sortedBy { it?.value })
        assertEquals(2, viewModel.state.value.selectedCount)
    }

    @Test
    fun showsOnlyTheMachinesOfTheOpenedGym() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck", gymId = NORTE)))

        val viewModel = opened()

        assertEquals(0, viewModel.state.value.selectedCount)
        assertFalse(viewModel.item("Pec Deck").isSelected)
    }

    @Test
    fun startsOverWhenAnotherGymIsOpened() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck", gymId = NORTE)))
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.QueryChanged("pec"))

        viewModel.onEvent(InventoryEvent.Opened(NORTE))

        assertEquals("Gimnasio Norte", viewModel.state.value.gymName)
        assertEquals("", viewModel.state.value.query)
        assertTrue(viewModel.item("Pec Deck").isSelected)
    }

    @Test
    fun findsByNameIgnoringCaseAndAccents() = runTest {
        val viewModel = opened()

        viewModel.onEvent(InventoryEvent.QueryChanged("PÉC Deck"))

        assertEquals(listOf("Pec Deck"), viewModel.names())
    }

    @Test
    fun findsByAnAccentedAliasTypedWithoutTheAccent() = runTest {
        val viewModel = opened()

        viewModel.onEvent(InventoryEvent.QueryChanged("maquina"))

        assertEquals(listOf("Pec Deck"), viewModel.names())
    }

    @Test
    fun findsACustomMachineByItsName() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Banco de piña")))
        val viewModel = opened()

        viewModel.onEvent(InventoryEvent.QueryChanged("piña"))
        assertEquals(listOf("Banco de piña"), viewModel.names())

        // La ñ no es una n con acento.
        viewModel.onEvent(InventoryEvent.QueryChanged("pina"))
        assertTrue(viewModel.state.value.rows.isEmpty())
    }

    @Test
    fun filtersByCategoryWithoutHeaders() = runTest {
        val viewModel = opened()

        viewModel.onEvent(InventoryEvent.CategorySelected(EquipmentCategory.MACHINE))

        assertEquals(
            listOf("Chest Press", "Pec Deck"),
            viewModel.state.value.rows.map {
                (it as InventoryRow.Item).name
            },
        )
    }

    @Test
    fun combinesTheCategoryAndTheSearch() = runTest {
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.CategorySelected(EquipmentCategory.MACHINE))

        viewModel.onEvent(InventoryEvent.QueryChanged("press"))

        assertEquals(listOf("Chest Press"), viewModel.names())
    }

    @Test
    fun togglingAddsTheMachineAndCountsIt() = runTest {
        val viewModel = opened()

        viewModel.toggle("Pec Deck")

        assertTrue(viewModel.item("Pec Deck").isSelected)
        assertEquals(1, viewModel.state.value.selectedCount)
        assertEquals("pec_deck", repository.savedEquipment.single().catalogKey?.value)
    }

    @Test
    fun togglingAgainRemovesIt() = runTest {
        val viewModel = opened()
        viewModel.toggle("Pec Deck")

        viewModel.toggle("Pec Deck")

        assertFalse(viewModel.item("Pec Deck").isSelected)
        assertEquals(0, viewModel.state.value.selectedCount)
        assertTrue(repository.savedEquipment.isEmpty())
    }

    @Test
    fun keepsTheSameRowKeysWhenTogglingAndFiltering() = runTest {
        val viewModel = opened()
        val before = viewModel.item("Pec Deck").key

        viewModel.toggle("Pec Deck")
        viewModel.onEvent(InventoryEvent.QueryChanged("pec"))

        assertEquals(before, viewModel.item("Pec Deck").key)
        val keys = viewModel.state.value.rows.map { it.key }
        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun saysThatAMachineInUseByAPlanCannotBeRemoved() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck")))
        repository.usages[EquipmentId("eq_1")] = 3
        val viewModel = opened()

        viewModel.toggle("Pec Deck")

        assertEquals(InventoryNotice.InUse(viewModel.item("Pec Deck").key, usages = 3), viewModel.state.value.notice)
        assertTrue(viewModel.item("Pec Deck").isSelected)
    }

    @Test
    fun theNoticeGoesAwayWithTheNextToggleThatWorks() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck")))
        repository.usages[EquipmentId("eq_1")] = 3
        val viewModel = opened()
        viewModel.toggle("Pec Deck")

        viewModel.toggle("Chest Press")

        assertNull(viewModel.state.value.notice)
    }

    @Test
    fun saysThatTheGymIsFullAtTheLimitOfTheContract() = runTest {
        repository.saveEquipment((1..120).map { equipment("eq_$it", name = "Aparato $it") })
        val viewModel = opened()

        viewModel.toggle("Pec Deck")

        assertEquals(InventoryNotice.Full(viewModel.item("Pec Deck").key, max = 120), viewModel.state.value.notice)
        assertEquals(120, viewModel.state.value.selectedCount)
    }

    @Test
    fun aCustomMachineIsNotRemovedByTogglingItsRow() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Polea del fondo")))
        val viewModel = opened()

        viewModel.toggle("Polea del fondo")

        assertEquals(1, repository.savedEquipment.size)
    }

    @Test
    fun opensAnEmptyEditorToAddACustomMachine() = runTest {
        val viewModel = opened()

        viewModel.effects.test {
            viewModel.onEvent(InventoryEvent.AddCustomClicked)

            assertEquals(InventoryEffect.EditorOpened, awaitItem())
            expectNoEvents()
        }
        assertEquals(EquipmentEditorState(), viewModel.state.value.editor)
    }

    @Test
    fun addsACustomMachineWithItsTypeAndRangeAndClosesTheEditor() = runTest {
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)
        viewModel.onEvent(EquipmentEditorEvent.NameChanged("  Polea del fondo "))
        viewModel.onEvent(EquipmentEditorEvent.CategoryChanged(EquipmentCategory.CABLE))
        viewModel.onEvent(EquipmentEditorEvent.LoadTypeChanged(LoadType.PLATES_TOTAL))
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MIN_KG, "5"))
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MAX_KG, "90"))
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.INCREMENT_KG, "2.5"))

        viewModel.effects.test {
            assertEquals(InventoryEffect.EditorOpened, awaitItem())

            viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

            assertEquals(InventoryEffect.EditorClosed, awaitItem())
            expectNoEvents()
        }
        val saved = repository.savedEquipment.single()
        assertEquals("Polea del fondo", saved.name)
        assertEquals(EquipmentCategory.CABLE, saved.category)
        assertEquals(LoadType.PLATES_TOTAL, saved.loadType)
        assertEquals(listOf(5.0, 90.0, 2.5), listOf(saved.minKg, saved.maxKg, saved.incrementKg))
        assertTrue(saved.isCustom)
        val row = viewModel.item("Polea del fondo")
        assertTrue(row.isSelected && row.isCustom)
        assertEquals(1, viewModel.state.value.selectedCount)
    }

    @Test
    fun addsOnceWhenSaveIsClickedTwice() = runTest {
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)
        viewModel.onEvent(EquipmentEditorEvent.NameChanged("Polea del fondo"))

        viewModel.onEvent(EquipmentEditorEvent.SaveClicked)
        viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

        assertEquals(1, repository.savedEquipment.size)
    }

    @Test
    fun keepsOnlyNumbersInTheLoadFields() = runTest {
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)

        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MAX_KG, "12a.555kg"))

        assertEquals("12,55", viewModel.state.value.editor.maxKg)
    }

    @Test
    fun keepsTheEditorOpenAndShowsEveryErrorTheUseCaseGives() = runTest {
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MIN_KG, "50"))
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MAX_KG, "10"))

        viewModel.effects.test {
            assertEquals(InventoryEffect.EditorOpened, awaitItem())

            viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

            expectNoEvents()
        }
        val editor = viewModel.state.value.editor
        assertEquals(
            listOf(EquipmentError.Field(EquipmentField.NAME, FieldIssue.Required), EquipmentError.MinAboveMax),
            editor.errors,
        )
        // Se puede corregir y volver a intentar.
        assertFalse(editor.isSaving)
        assertTrue(repository.savedEquipment.isEmpty())
    }

    @Test
    fun rejectsTheNameOfAMachineTheGymAlreadyHas() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck")))
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)
        viewModel.onEvent(EquipmentEditorEvent.NameChanged("pec deck"))

        viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

        assertEquals(listOf(EquipmentError.DuplicateName), viewModel.state.value.editor.errors)
    }

    @Test
    fun clearsTheErrorsWhenTheFormChanges() = runTest {
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)
        viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

        viewModel.onEvent(EquipmentEditorEvent.NameChanged("P"))

        assertTrue(viewModel.state.value.editor.errors.isEmpty())
    }

    @Test
    fun saysThatTheGymIsFullWhenAddingACustomMachine() = runTest {
        repository.saveEquipment((1..120).map { equipment("eq_$it", name = "Aparato $it") })
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.AddCustomClicked)
        viewModel.onEvent(EquipmentEditorEvent.NameChanged("Polea del fondo"))

        viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

        assertEquals(EditorProblem.Full(max = 120), viewModel.state.value.editor.problem)
    }

    @Test
    fun opensTheEditorWithTheDataOfTheMachineToEdit() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck", maxKg = 92.5)))
        val viewModel = opened()

        viewModel.effects.test {
            viewModel.onEvent(InventoryEvent.EditClicked(viewModel.item("Pec Deck").key))

            assertEquals(InventoryEffect.EditorOpened, awaitItem())
        }
        val expected = EquipmentEditorState(
            equipmentId = EquipmentId("eq_1"),
            isCustom = false,
            name = "Pec Deck",
            maxKg = "92,5",
        )
        assertEquals(expected, viewModel.state.value.editor)
    }

    @Test
    fun doesNotOpenTheEditorForAMachineTheGymDoesNotHave() = runTest {
        val viewModel = opened()

        viewModel.effects.test {
            viewModel.onEvent(InventoryEvent.EditClicked(viewModel.item("Pec Deck").key))

            expectNoEvents()
        }
    }

    @Test
    fun savesTheLoadRangeOfAMachineAndShowsItInItsRow() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Pec Deck", catalogKey = "pec_deck")))
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.EditClicked(viewModel.item("Pec Deck").key))
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MIN_KG, "5"))
        viewModel.onEvent(EquipmentEditorEvent.KgChanged(EquipmentField.MAX_KG, "100"))

        viewModel.effects.test {
            assertEquals(InventoryEffect.EditorOpened, awaitItem())

            viewModel.onEvent(EquipmentEditorEvent.SaveClicked)

            assertEquals(InventoryEffect.EditorClosed, awaitItem())
        }
        val row = viewModel.item("Pec Deck")
        assertEquals(EquipmentId("eq_1"), row.equipmentId)
        assertEquals(5.0 to 100.0, row.minKg to row.maxKg)
    }

    @Test
    fun removesACustomMachineFromItsEditor() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Polea del fondo")))
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.EditClicked(viewModel.item("Polea del fondo").key))

        viewModel.effects.test {
            assertEquals(InventoryEffect.EditorOpened, awaitItem())

            viewModel.onEvent(EquipmentEditorEvent.RemoveClicked)

            assertEquals(InventoryEffect.EditorClosed, awaitItem())
        }
        assertTrue(repository.savedEquipment.isEmpty())
        assertFalse("Polea del fondo" in viewModel.names())
    }

    @Test
    fun saysInTheEditorThatACustomMachineInUseCannotBeRemoved() = runTest {
        repository.saveEquipment(listOf(equipment("eq_1", name = "Polea del fondo")))
        repository.usages[EquipmentId("eq_1")] = 1
        val viewModel = opened()
        viewModel.onEvent(InventoryEvent.EditClicked(viewModel.item("Polea del fondo").key))

        viewModel.effects.test {
            assertEquals(InventoryEffect.EditorOpened, awaitItem())

            viewModel.onEvent(EquipmentEditorEvent.RemoveClicked)

            expectNoEvents()
        }
        val editor = viewModel.state.value.editor
        assertEquals(EditorProblem.InUse(usages = 1), editor.problem)
        assertFalse(editor.isSaving)
        assertEquals(1, repository.savedEquipment.size)
    }

    @Test
    fun staysFluentDataWiseWithTheWholeCatalogAndAFullGym() = runTest {
        // 62 entradas como la semilla y 120 máquinas propias, el tope: 182 filas y sus cabeceras.
        catalog = (1..62).map { catalogItem("item_$it", "Item $it") }
        repository.saveEquipment((1..120).map { equipment("eq_$it", name = "Aparato $it") })

        val viewModel = opened()

        assertEquals(182, viewModel.items().size)
        val keys = viewModel.state.value.rows.map { it.key }
        assertEquals(keys.distinct(), keys)
    }
}
