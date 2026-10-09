package app.reporrun.feature.plansetup

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.domain.usecase.ObserveCardioOptionsUseCase
import app.reporrun.core.domain.usecase.ObservePreferencesUseCase
import app.reporrun.core.domain.usecase.PreferencesField
import app.reporrun.core.domain.usecase.SavePreferencesUseCase
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.EquipmentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sigue el modelo de `ProfileViewModelTest`: casos de uso reales, repositorios fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {
    private var preferences = FakePreferencesRepository()
    private var gyms = FakeGymRepository(inventoryWithCardio())

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel() = PreferencesViewModel(
        observePreferences = ObservePreferencesUseCase(preferences),
        observeCardioOptions = ObserveCardioOptionsUseCase(gyms),
        savePreferences = SavePreferencesUseCase(preferences),
        clock = FixedClock(NOW),
        timeZone = TimeZone.UTC,
    )

    /** Un formulario que el caso de uso acepta: dos días y 60 minutos. */
    private fun PreferencesViewModel.fillValid() {
        onEvent(PreferencesEvent.DayToggled(DayOfWeek.MONDAY))
        onEvent(PreferencesEvent.DayToggled(DayOfWeek.WEDNESDAY))
        onEvent(PreferencesEvent.SessionMinutesChanged("60"))
    }

    @Test
    fun staysLoadingUntilStorageAnswers() = runTest {
        preferences = FakePreferencesRepository(answered = false)
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.isLoading)

        preferences.answer()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun startsWithNoDaysNoStartDateAndTheBlockLengthOfTheModel() = runTest {
        val state = viewModel().state.value

        assertEquals(emptySet(), state.availableDays)
        assertEquals("", state.sessionMinutes)
        assertNull(state.startDate)
        assertEquals(BlockPreferences.BLOCK_DAYS_V1, state.blockDays)
    }

    @Test
    fun offersStartDatesFromTodayOnwards() = runTest {
        val options = viewModel().state.value.startDateOptions

        assertEquals(TODAY, options.first())
        assertEquals(LocalDate(2026, 10, 21), options.last())
    }

    @Test
    fun preloadsTheSavedPreferences() = runTest {
        preferences = FakePreferencesRepository(savedPreferences(startDate = LocalDate(2026, 10, 12)))

        val state = viewModel().state.value

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), state.availableDays)
        assertEquals("75", state.sessionMinutes)
        assertEquals(LocalDate(2026, 10, 12), state.startDate)
        assertEquals(setOf("Treadmill"), state.cardioModes)
        assertEquals("Sentadilla búlgara", state.avoidExercises)
    }

    @Test
    fun doesNotPreloadAStartDateThatAlreadyPassed() = runTest {
        preferences = FakePreferencesRepository(savedPreferences(startDate = LocalDate(2026, 10, 7)))

        assertNull(viewModel().state.value.startDate)
    }

    @Test
    fun offersTheCardioEquipmentOfTheActiveGymAsCardioModes() = runTest {
        assertEquals(listOf("Bike", "Treadmill"), viewModel().state.value.cardioOptions)
    }

    @Test
    fun offersNoCardioModesWhenTheGymHasNoCardioEquipment() = runTest {
        gyms = FakeGymRepository()

        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals(emptyList(), state.cardioOptions)
    }

    @Test
    fun unselectsASavedCardioModeThatIsNoLongerInTheInventory() = runTest {
        preferences = FakePreferencesRepository(savedPreferences(cardioModes = listOf("Treadmill", "Rower")))

        assertEquals(setOf("Treadmill"), viewModel().state.value.cardioModes)
    }

    @Test
    fun unselectsACardioModeWhenItsEquipmentLeavesTheInventory() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(PreferencesEvent.CardioModeToggled("Bike"))

        gyms.deleteEquipment(EquipmentId("eq_bike"))

        assertEquals(listOf("Treadmill"), viewModel.state.value.cardioOptions)
        assertEquals(emptySet(), viewModel.state.value.cardioModes)
    }

    @Test
    fun togglesADayOnAndOff() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(PreferencesEvent.DayToggled(DayOfWeek.FRIDAY))
        assertEquals(setOf(DayOfWeek.FRIDAY), viewModel.state.value.availableDays)

        viewModel.onEvent(PreferencesEvent.DayToggled(DayOfWeek.FRIDAY))
        assertEquals(emptySet(), viewModel.state.value.availableDays)
    }

    @Test
    fun keepsOnlyDigitsInTheSessionMinutes() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(PreferencesEvent.SessionMinutesChanged("7a5,0 9"))

        assertEquals("750", viewModel.state.value.sessionMinutes)
    }

    @Test
    fun savesTheFormAndSaysSo() = runTest {
        val viewModel = viewModel()
        viewModel.fillValid()
        viewModel.onEvent(PreferencesEvent.StartDateSelected(LocalDate(2026, 10, 12)))
        viewModel.onEvent(PreferencesEvent.AvoidExercisesChanged(" Peso muerto "))

        viewModel.effects.test {
            viewModel.onEvent(PreferencesEvent.SaveClicked)

            assertEquals(PreferencesEffect.PreferencesSaved, awaitItem())
            expectNoEvents()
        }
        val saved = preferences.saved
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), saved?.availableDays)
        assertEquals(60, saved?.sessionMinutes)
        assertEquals(BlockPreferences.BLOCK_DAYS_V1, saved?.blockDays)
        assertEquals(LocalDate(2026, 10, 12), saved?.startDate)
        assertEquals("Peso muerto", saved?.avoidExercises)
    }

    @Test
    fun savesTheCardioModesInTheOrderTheyAreOffered() = runTest {
        val viewModel = viewModel()
        viewModel.fillValid()
        viewModel.onEvent(PreferencesEvent.CardioModeToggled("Treadmill"))
        viewModel.onEvent(PreferencesEvent.CardioModeToggled("Bike"))

        viewModel.onEvent(PreferencesEvent.SaveClicked)

        assertEquals(listOf("Bike", "Treadmill"), preferences.saved?.cardioModes)
    }

    @Test
    fun savingReplacesThePreviousPreferencesKeepingTheirId() = runTest {
        preferences = FakePreferencesRepository(savedPreferences())
        val viewModel = viewModel()
        viewModel.onEvent(PreferencesEvent.SessionMinutesChanged("45"))

        viewModel.onEvent(PreferencesEvent.SaveClicked)

        assertEquals(BlockPreferencesId("prefs_previas"), preferences.saved?.id)
        assertEquals(45, preferences.saved?.sessionMinutes)
    }

    @Test
    fun withoutAnAvailableDayShowsTheErrorOfTheUseCaseAndSavesNothing() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(PreferencesEvent.SessionMinutesChanged("60"))

        viewModel.effects.test {
            viewModel.onEvent(PreferencesEvent.SaveClicked)

            expectNoEvents()
        }
        assertEquals(mapOf(PreferencesField.AVAILABLE_DAYS to FieldIssue.Required), viewModel.state.value.errors)
        assertFalse(viewModel.state.value.isSaving)
        assertNull(preferences.saved)
    }

    @Test
    fun showsTheRangeOfTheUseCaseWhenTheMinutesAreOutOfIt() = runTest {
        val viewModel = viewModel()
        viewModel.fillValid()
        viewModel.onEvent(PreferencesEvent.SessionMinutesChanged("5"))

        viewModel.onEvent(PreferencesEvent.SaveClicked)

        val range = BlockPreferences.SESSION_MINUTES_RANGE
        assertEquals(
            FieldIssue.OutOfRange(range.first.toDouble(), range.last.toDouble()),
            viewModel.state.value.errors[PreferencesField.SESSION_MINUTES],
        )
        assertNull(preferences.saved)
    }

    @Test
    fun asksForTheMinutesWhenTheFieldIsEmpty() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(PreferencesEvent.DayToggled(DayOfWeek.MONDAY))

        viewModel.onEvent(PreferencesEvent.SaveClicked)

        assertEquals(FieldIssue.Required, viewModel.state.value.errors[PreferencesField.SESSION_MINUTES])
    }

    @Test
    fun clearsTheErrorOfAFieldWhenItIsTouched() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(PreferencesEvent.SaveClicked)
        assertEquals(
            setOf(PreferencesField.AVAILABLE_DAYS, PreferencesField.SESSION_MINUTES),
            viewModel.state.value.errors.keys,
        )

        viewModel.onEvent(PreferencesEvent.DayToggled(DayOfWeek.MONDAY))

        assertEquals(setOf(PreferencesField.SESSION_MINUTES), viewModel.state.value.errors.keys)
    }

    @Test
    fun rejectsAnAvoidTextLongerThanTheUseCaseAllows() = runTest {
        val viewModel = viewModel()
        viewModel.fillValid()
        viewModel.onEvent(PreferencesEvent.AvoidExercisesChanged("a".repeat(BlockPreferences.MAX_AVOID_LENGTH + 1)))

        viewModel.onEvent(PreferencesEvent.SaveClicked)

        assertEquals(
            FieldIssue.TooLong(BlockPreferences.MAX_AVOID_LENGTH),
            viewModel.state.value.errors[PreferencesField.AVOID_EXERCISES],
        )
    }
}
