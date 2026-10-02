package app.reporrun.core.domain.usecase

import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.LoadUnit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SavePreferencesUseCaseTest {
    private val repository = FakePreferencesRepository()
    private val savePreferences = SavePreferencesUseCase(repository)

    private suspend fun errorsOf(input: PreferencesInput): List<FieldError<PreferencesField>> =
        assertIs<SavePreferencesResult.Invalid>(savePreferences(input)).errors

    @Test
    fun savesValidPreferencesInKilos() = runTest {
        val input = preferencesInput().copy(startDate = LocalDate(2026, 10, 5))

        val result = assertIs<SavePreferencesResult.Saved>(savePreferences(input))

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), result.preferences.availableDays)
        assertEquals(LocalDate(2026, 10, 5), result.preferences.startDate)
        assertEquals(LoadUnit.KG, result.preferences.unit)
        assertEquals(result.preferences, repository.saved)
    }

    @Test
    fun keepsTheIdOfThePreferencesItReplaces() = runTest {
        val existing = FakePreferencesRepository(preferences(id = "prefs_previas"))

        SavePreferencesUseCase(existing)(preferencesInput())

        assertEquals(BlockPreferencesId("prefs_previas"), existing.saved?.id)
        assertEquals(75, existing.saved?.sessionMinutes)
    }

    @Test
    fun rejectsPreferencesWithoutAnyDay() = runTest {
        val errors = errorsOf(preferencesInput().copy(availableDays = emptySet()))

        assertEquals(listOf(FieldError(PreferencesField.AVAILABLE_DAYS, FieldIssue.Required)), errors)
        assertNull(repository.saved)
    }

    @Test
    fun acceptsASingleDay() = runTest {
        val input = preferencesInput().copy(availableDays = setOf(DayOfWeek.SUNDAY))

        assertIs<SavePreferencesResult.Saved>(savePreferences(input))
    }

    @Test
    fun acceptsTheSessionMinutesLimits() = runTest {
        assertIs<SavePreferencesResult.Saved>(savePreferences(preferencesInput().copy(sessionMinutes = 20)))
        assertIs<SavePreferencesResult.Saved>(savePreferences(preferencesInput().copy(sessionMinutes = 180)))
    }

    @Test
    fun rejectsSessionMinutesOutOfRange() = runTest {
        val errors = errorsOf(preferencesInput().copy(sessionMinutes = 19))

        assertEquals(listOf(FieldError(PreferencesField.SESSION_MINUTES, FieldIssue.OutOfRange(20.0, 180.0))), errors)
    }

    @Test
    fun rejectsMissingSessionMinutes() = runTest {
        val errors = errorsOf(preferencesInput().copy(sessionMinutes = null))

        assertEquals(listOf(FieldError(PreferencesField.SESSION_MINUTES, FieldIssue.Required)), errors)
    }

    @Test
    fun rejectsABlockThatIsNotFourteenDays() = runTest {
        val errors = errorsOf(preferencesInput().copy(blockDays = 28))

        assertEquals(listOf(FieldError(PreferencesField.BLOCK_DAYS, FieldIssue.OutOfRange(14.0, 14.0))), errors)
    }

    @Test
    fun trimsAndDeduplicatesCardioModesIgnoringCase() = runTest {
        val input = preferencesInput().copy(cardioModes = listOf("Caminadora", " caminadora ", "  ", "Elíptica"))

        val result = assertIs<SavePreferencesResult.Saved>(savePreferences(input))

        assertEquals(listOf("Caminadora", "Elíptica"), result.preferences.cardioModes)
    }

    @Test
    fun rejectsTooManyCardioModes() = runTest {
        val errors = errorsOf(preferencesInput().copy(cardioModes = (1..7).map { "Modo $it" }))

        assertEquals(listOf(FieldError(PreferencesField.CARDIO_MODES, FieldIssue.TooMany(6))), errors)
    }

    @Test
    fun rejectsACardioModeThatIsTooLong() = runTest {
        val errors = errorsOf(preferencesInput().copy(cardioModes = listOf("x".repeat(41))))

        assertEquals(listOf(FieldError(PreferencesField.CARDIO_MODES, FieldIssue.TooLong(40))), errors)
    }

    @Test
    fun rejectsAnAvoidTextThatIsTooLong() = runTest {
        val errors = errorsOf(preferencesInput().copy(avoidExercises = "x".repeat(401)))

        assertEquals(listOf(FieldError(PreferencesField.AVOID_EXERCISES, FieldIssue.TooLong(400))), errors)
    }

    @Test
    fun reportsEveryErrorAtOnce() = runTest {
        val errors = errorsOf(preferencesInput().copy(availableDays = emptySet(), sessionMinutes = 500))

        assertEquals(listOf(PreferencesField.AVAILABLE_DAYS, PreferencesField.SESSION_MINUTES), errors.map { it.field })
    }
}
