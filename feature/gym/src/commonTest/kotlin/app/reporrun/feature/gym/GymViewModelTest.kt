package app.reporrun.feature.gym

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.CreateGymUseCase
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.ObserveGymsUseCase
import app.reporrun.core.domain.usecase.RenameGymUseCase
import app.reporrun.core.domain.usecase.SetActiveGymUseCase
import app.reporrun.core.model.GymId
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

/** Sigue el modelo de `ProfileViewModelTest`: casos de uso reales, repositorio fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class GymViewModelTest {
    private var repository = FakeGymRepository()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel() = GymViewModel(
        observeGyms = ObserveGymsUseCase(repository),
        createGym = CreateGymUseCase(repository, SequentialIds(), FixedClock(NOW)),
        renameGym = RenameGymUseCase(repository),
        setActiveGym = SetActiveGymUseCase(repository),
    )

    private fun GymViewModel.activeIds() = state.value.gyms.filter { it.isActive }.map { it.id }

    @Test
    fun staysLoadingUntilStorageAnswers() = runTest {
        repository = FakeGymRepository(initial = null)
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.isLoading)

        repository.answer()

        assertEquals(GymUiState(isLoading = false), viewModel.state.value)
    }

    @Test
    fun showsTheSavedGymsWithTheActiveOneMarked() = runTest {
        repository = FakeGymRepository(twoGyms())

        val viewModel = viewModel()

        assertEquals(listOf("Gimnasio Central", "Gimnasio Norte"), viewModel.state.value.gyms.map { it.name })
        assertEquals(listOf(CENTRAL), viewModel.activeIds())
    }

    @Test
    fun opensAnEmptyEditorToAddAGym() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(GymEvent.AddClicked)

            assertEquals(GymEffect.EditorOpened, awaitItem())
            expectNoEvents()
        }
        assertEquals(GymEditorState(), viewModel.state.value.editor)
    }

    @Test
    fun createsTheFirstGymActiveAndClosesTheEditor() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.AddClicked)
        viewModel.onEvent(GymEvent.NameChanged("  Gimnasio Central "))

        viewModel.effects.test {
            assertEquals(GymEffect.EditorOpened, awaitItem())

            viewModel.onEvent(GymEvent.SaveClicked)

            assertEquals(GymEffect.EditorClosed, awaitItem())
            expectNoEvents()
        }
        val gym = viewModel.state.value.gyms.single()
        assertEquals("Gimnasio Central", gym.name)
        assertEquals(GymId("gym_id1"), gym.id)
        assertTrue(gym.isActive)
    }

    @Test
    fun aSecondGymDoesNotTakeTheActiveMark() = runTest {
        repository = FakeGymRepository(listOf(twoGyms().first()))
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.AddClicked)
        viewModel.onEvent(GymEvent.NameChanged("Gimnasio Sur"))

        viewModel.onEvent(GymEvent.SaveClicked)

        assertEquals(listOf("Gimnasio Central", "Gimnasio Sur"), viewModel.state.value.gyms.map { it.name })
        assertEquals(listOf(CENTRAL), viewModel.activeIds())
    }

    @Test
    fun savesOnceWhenSaveIsClickedTwice() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.AddClicked)
        viewModel.onEvent(GymEvent.NameChanged("Gimnasio Central"))

        viewModel.onEvent(GymEvent.SaveClicked)
        viewModel.onEvent(GymEvent.SaveClicked)

        assertEquals(1, repository.saveCalls)
    }

    @Test
    fun keepsTheEditorOpenAndShowsWhyABlankNameIsRejected() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.AddClicked)
        viewModel.onEvent(GymEvent.NameChanged("   "))

        viewModel.effects.test {
            assertEquals(GymEffect.EditorOpened, awaitItem())

            viewModel.onEvent(GymEvent.SaveClicked)

            expectNoEvents()
        }
        assertEquals(FieldIssue.Required, viewModel.state.value.editor.error)
        // Se puede corregir y volver a intentar.
        assertFalse(viewModel.state.value.editor.isSaving)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun showsTheLengthLimitTheUseCaseGives() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.AddClicked)
        viewModel.onEvent(GymEvent.NameChanged("g".repeat(81)))

        viewModel.onEvent(GymEvent.SaveClicked)

        assertEquals(FieldIssue.TooLong(80), viewModel.state.value.editor.error)
    }

    @Test
    fun clearsTheNameErrorWhenTheNameChanges() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.AddClicked)
        viewModel.onEvent(GymEvent.SaveClicked)

        viewModel.onEvent(GymEvent.NameChanged("G"))

        assertNull(viewModel.state.value.editor.error)
    }

    @Test
    fun opensTheEditorWithTheNameOfTheGymToRename() = runTest {
        repository = FakeGymRepository(twoGyms())
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(GymEvent.RenameClicked(NORTE))

            assertEquals(GymEffect.EditorOpened, awaitItem())
            expectNoEvents()
        }
        assertEquals(GymEditorState(gymId = NORTE, name = "Gimnasio Norte"), viewModel.state.value.editor)
    }

    @Test
    fun renamesAGymWithoutChangingWhichOneIsActive() = runTest {
        repository = FakeGymRepository(twoGyms())
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.RenameClicked(CENTRAL))
        viewModel.onEvent(GymEvent.NameChanged("Gimnasio del centro"))

        viewModel.effects.test {
            assertEquals(GymEffect.EditorOpened, awaitItem())

            viewModel.onEvent(GymEvent.SaveClicked)

            assertEquals(GymEffect.EditorClosed, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf("Gimnasio del centro", "Gimnasio Norte"), viewModel.state.value.gyms.map { it.name })
        assertEquals(listOf(CENTRAL), viewModel.activeIds())
    }

    @Test
    fun doesNotOpenTheEditorForAGymThatIsNotInTheList() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(GymEvent.RenameClicked(NORTE))

            expectNoEvents()
        }
    }

    @Test
    fun closesTheEditorWhenTheGymToRenameIsGone() = runTest {
        repository = FakeGymRepository(twoGyms())
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.RenameClicked(NORTE))
        repository.deleteGym(NORTE)

        viewModel.effects.test {
            assertEquals(GymEffect.EditorOpened, awaitItem())

            viewModel.onEvent(GymEvent.SaveClicked)

            assertEquals(GymEffect.EditorClosed, awaitItem())
        }
        assertEquals(listOf(CENTRAL), viewModel.state.value.gyms.map { it.id })
    }

    @Test
    fun forgetsTheLastNameWhenTheEditorOpensAgain() = runTest {
        repository = FakeGymRepository(twoGyms())
        val viewModel = viewModel()
        viewModel.onEvent(GymEvent.RenameClicked(NORTE))
        viewModel.onEvent(GymEvent.NameChanged(""))
        viewModel.onEvent(GymEvent.SaveClicked)

        viewModel.onEvent(GymEvent.AddClicked)

        assertEquals(GymEditorState(), viewModel.state.value.editor)
    }

    @Test
    fun activatesAnotherGymAndDeactivatesThePreviousOne() = runTest {
        repository = FakeGymRepository(twoGyms())
        val viewModel = viewModel()

        viewModel.onEvent(GymEvent.ActivateClicked(NORTE))

        assertEquals(listOf(NORTE), viewModel.activeIds())
    }

    @Test
    fun keepsTheActiveGymWhenAskedToActivateOneThatIsGone() = runTest {
        repository = FakeGymRepository(twoGyms())
        val viewModel = viewModel()

        viewModel.onEvent(GymEvent.ActivateClicked(GymId("gym_borrado")))

        assertEquals(listOf(CENTRAL), viewModel.activeIds())
    }
}
