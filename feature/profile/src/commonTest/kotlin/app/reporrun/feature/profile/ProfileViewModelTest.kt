package app.reporrun.feature.profile

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.domain.usecase.ProfileField
import app.reporrun.core.domain.usecase.SaveProfileUseCase
import app.reporrun.core.domain.usecase.ValidateProfileUseCase
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ProfileId
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

/** Sigue el modelo de `OnboardingViewModelTest`: casos de uso reales, repositorio fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private var repository = FakeProfileRepository()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel() = ProfileViewModel(
        observeProfile = ObserveProfileUseCase(repository),
        validateProfile = ValidateProfileUseCase(),
        saveProfile = SaveProfileUseCase(repository, FixedClock(NOW)),
    )

    private fun ProfileViewModel.type(field: ProfileTextField, value: String) =
        onEvent(ProfileEvent.TextChanged(field, value))

    /** Un paso 1 que el caso de uso acepta. */
    private fun ProfileViewModel.fillBasics() {
        type(ProfileTextField.AGE, "30")
        type(ProfileTextField.HEIGHT, "170")
        onEvent(ProfileEvent.ExperienceSelected(Experience.BEGINNER))
    }

    @Test
    fun startsEmptyWhenThereIsNoProfile() = runTest {
        val state = viewModel().state.value

        assertEquals(ProfileUiState(isLoading = false), state)
    }

    @Test
    fun preloadsTheSavedProfile() = runTest {
        repository = FakeProfileRepository(savedProfile())

        val state = viewModel().state.value

        assertEquals("Sol", state.alias)
        assertEquals("30", state.age)
        assertEquals("170", state.height)
        assertEquals("72,5", state.weight)
        assertEquals("84", state.waist)
        assertEquals("", state.hip)
        assertEquals(Experience.INTERMEDIATE, state.experience)
        assertEquals(Goal.RECOMPOSITION, state.goal)
        assertEquals(setOf(MuscleGroup.BACK, MuscleGroup.GLUTES), state.priorityMuscles)
        assertEquals("Molestia ocasional en el hombro derecho.", state.limitations)
    }

    @Test
    fun keepsOnlyDigitsInTheAge() = runTest {
        val viewModel = viewModel()

        viewModel.type(ProfileTextField.AGE, "3a0,5")

        assertEquals("305", viewModel.state.value.age)
    }

    @Test
    fun acceptsACommaOrADotAsDecimalSeparator() = runTest {
        val viewModel = viewModel()

        viewModel.type(ProfileTextField.WEIGHT, "72.55")
        viewModel.type(ProfileTextField.WAIST, "84,0,3x")

        assertEquals("72,5", viewModel.state.value.weight)
        assertEquals("84,0", viewModel.state.value.waist)
    }

    @Test
    fun movesToTheGoalWhenTheBasicsAreValid() = runTest {
        val viewModel = viewModel()
        viewModel.fillBasics()

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.ContinueClicked)

            assertEquals(ProfileEffect.BasicsCompleted, awaitItem())
            expectNoEvents()
        }
        // El objetivo falta, pero todavía no se ha podido elegir: no es un error del paso 1.
        assertTrue(viewModel.state.value.errors.isEmpty())
    }

    @Test
    fun staysInTheBasicsAndShowsEveryErrorTheUseCaseFinds() = runTest {
        val viewModel = viewModel()
        viewModel.type(ProfileTextField.AGE, "15")
        viewModel.type(ProfileTextField.WEIGHT, "20")

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.ContinueClicked)

            expectNoEvents()
        }
        assertEquals(
            mapOf(
                ProfileField.AGE to FieldIssue.OutOfRange(16.0, 90.0),
                ProfileField.HEIGHT_CM to FieldIssue.Required,
                ProfileField.WEIGHT_KG to FieldIssue.OutOfRange(30.0, 300.0),
                ProfileField.EXPERIENCE to FieldIssue.Required,
            ),
            viewModel.state.value.errors,
        )
    }

    @Test
    fun clearsTheErrorOfAFieldWhenItChanges() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(ProfileEvent.ContinueClicked)

        viewModel.type(ProfileTextField.AGE, "3")
        viewModel.onEvent(ProfileEvent.ExperienceSelected(Experience.ADVANCED))

        assertEquals(setOf(ProfileField.HEIGHT_CM), viewModel.state.value.errors.keys)
    }

    @Test
    fun refusesAFifthPriorityMuscle() = runTest {
        val viewModel = viewModel()
        val four = listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS, MuscleGroup.GLUTES)
        four.forEach { viewModel.onEvent(ProfileEvent.MuscleToggled(it)) }

        viewModel.onEvent(ProfileEvent.MuscleToggled(MuscleGroup.CORE))

        assertEquals(four.toSet(), viewModel.state.value.priorityMuscles)
        assertEquals(FieldIssue.TooMany(4), viewModel.state.value.errors[ProfileField.PRIORITY_MUSCLES])
    }

    @Test
    fun forgetsThePriorityLimitWarningWhenOneIsRemoved() = runTest {
        val viewModel = viewModel()
        MuscleGroup.entries.take(5).forEach { viewModel.onEvent(ProfileEvent.MuscleToggled(it)) }

        viewModel.onEvent(ProfileEvent.MuscleToggled(MuscleGroup.entries.first()))

        assertEquals(3, viewModel.state.value.priorityMuscles.size)
        assertNull(viewModel.state.value.errors[ProfileField.PRIORITY_MUSCLES])
    }

    @Test
    fun savesTheProfileAndSaysSoOnce() = runTest {
        val viewModel = viewModel()
        viewModel.fillBasics()
        viewModel.type(ProfileTextField.WEIGHT, "72,5")
        viewModel.type(ProfileTextField.ALIAS, "  Sol ")
        viewModel.onEvent(ProfileEvent.GoalSelected(Goal.STRENGTH))
        viewModel.onEvent(ProfileEvent.MuscleToggled(MuscleGroup.BACK))

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SaveClicked)
            viewModel.onEvent(ProfileEvent.SaveClicked)

            assertEquals(ProfileEffect.ProfileSaved, awaitItem())
            expectNoEvents()
        }
        val saved = repository.saved
        assertEquals(1, repository.saveCalls)
        assertEquals("Sol", saved?.alias)
        assertEquals(30, saved?.age)
        assertEquals(72.5, saved?.weightKg)
        assertNull(saved?.waistCm)
        assertEquals(Goal.STRENGTH, saved?.goal)
        assertEquals(setOf(MuscleGroup.BACK), saved?.priorityMuscles)
        assertEquals(NOW, saved?.updatedAt)
    }

    @Test
    fun doesNotSaveWithoutAGoal() = runTest {
        val viewModel = viewModel()
        viewModel.fillBasics()

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SaveClicked)

            expectNoEvents()
        }
        assertEquals(0, repository.saveCalls)
        assertEquals(mapOf(ProfileField.GOAL to FieldIssue.Required), viewModel.state.value.errors)
        // Se puede corregir y volver a intentar.
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun sendsBackToTheBasicsWhenSavingFindsAnErrorThere() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(ProfileEvent.GoalSelected(Goal.HYPERTROPHY))

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SaveClicked)

            assertEquals(ProfileEffect.BasicsNeedReview, awaitItem())
            expectNoEvents()
        }
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun editingKeepsTheIdOfTheSavedProfile() = runTest {
        repository = FakeProfileRepository(savedProfile())
        val viewModel = viewModel()

        viewModel.type(ProfileTextField.AGE, "31")
        viewModel.onEvent(ProfileEvent.SaveClicked)

        assertEquals(ProfileId("profile_previo"), repository.saved?.id)
        assertEquals(31, repository.saved?.age)
        assertEquals(72.5, repository.saved?.weightKg)
    }
}
