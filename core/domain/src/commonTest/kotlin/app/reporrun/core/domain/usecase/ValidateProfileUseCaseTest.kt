package app.reporrun.core.domain.usecase

import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValidateProfileUseCaseTest {
    private val validateProfile = ValidateProfileUseCase()

    @Test
    fun findsNothingInAValidProfile() {
        assertTrue(validateProfile(profileInput()).isEmpty())
    }

    @Test
    fun reportsEveryFieldThatFailsAtOnce() {
        val errors = validateProfile(profileInput().copy(age = null, heightCm = 250.0, goal = null))

        assertEquals(
            listOf(
                FieldError(ProfileField.AGE, FieldIssue.Required),
                FieldError(ProfileField.HEIGHT_CM, FieldIssue.OutOfRange(120.0, 230.0)),
                FieldError(ProfileField.GOAL, FieldIssue.Required),
            ),
            errors,
        )
    }

    @Test
    fun rejectsAFifthPriorityMuscle() {
        val five = MuscleGroup.entries.take(UserProfile.MAX_PRIORITY_MUSCLES + 1).toSet()

        val errors = validateProfile(profileInput().copy(priorityMuscles = five))

        assertEquals(listOf(FieldError(ProfileField.PRIORITY_MUSCLES, FieldIssue.TooMany(4))), errors)
    }

    @Test
    fun agreesWithSavingOnWhatIsInvalid() = runTest {
        val input = profileInput().copy(age = 15, weightKg = 20.0, alias = "   ")
        val saveProfile = SaveProfileUseCase(FakeProfileRepository(), FixedClock(NOW))

        val saved = assertIs<SaveProfileResult.Invalid>(saveProfile(input))

        assertEquals(saved.errors, validateProfile(input))
    }
}
