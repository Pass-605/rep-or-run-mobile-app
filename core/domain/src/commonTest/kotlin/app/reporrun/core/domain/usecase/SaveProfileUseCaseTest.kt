package app.reporrun.core.domain.usecase

import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveProfileUseCaseTest {
    private val repository = FakeProfileRepository()
    private val saveProfile = SaveProfileUseCase(repository, FixedClock(NOW))

    private suspend fun errorsOf(input: ProfileInput): List<FieldError<ProfileField>> =
        assertIs<SaveProfileResult.Invalid>(saveProfile(input)).errors

    @Test
    fun savesAValidProfileStampedWithTheClock() = runTest {
        val result = assertIs<SaveProfileResult.Saved>(saveProfile(profileInput()))

        assertEquals(NOW, result.profile.updatedAt)
        assertEquals(result.profile, repository.saved)
    }

    @Test
    fun keepsTheIdOfTheProfileItReplaces() = runTest {
        val existing = FakeProfileRepository(profile(id = "profile_previo"))

        SaveProfileUseCase(existing, FixedClock(NOW))(profileInput())

        assertEquals(ProfileId("profile_previo"), existing.saved?.id)
        assertEquals(30, existing.saved?.age)
    }

    @Test
    fun acceptsTheAgeLimits() = runTest {
        assertIs<SaveProfileResult.Saved>(saveProfile(profileInput().copy(age = ProfileRules.MIN_AGE)))
        assertIs<SaveProfileResult.Saved>(saveProfile(profileInput().copy(age = ProfileRules.MAX_AGE)))
    }

    @Test
    fun rejectsAnAgeBelowTheMinimum() = runTest {
        val errors = errorsOf(profileInput().copy(age = 15))

        assertEquals(listOf(FieldError(ProfileField.AGE, FieldIssue.OutOfRange(16.0, 90.0))), errors)
    }

    @Test
    fun rejectsAnAgeAboveTheMaximumEvenIfTheContractAllowsIt() = runTest {
        val errors = errorsOf(profileInput().copy(age = 91))

        assertEquals(listOf(FieldError(ProfileField.AGE, FieldIssue.OutOfRange(16.0, 90.0))), errors)
    }

    @Test
    fun ageRuleFitsInsideWhatTheModelAccepts() {
        assertTrue(ProfileRules.MIN_AGE in UserProfile.AGE_RANGE)
        assertTrue(ProfileRules.MAX_AGE in UserProfile.AGE_RANGE)
    }

    @Test
    fun rejectsAMissingAge() = runTest {
        val errors = errorsOf(profileInput().copy(age = null))

        assertEquals(listOf(FieldError(ProfileField.AGE, FieldIssue.Required)), errors)
    }

    @Test
    fun rejectsAHeightOutOfRange() = runTest {
        val errors = errorsOf(profileInput().copy(heightCm = 119.9))

        assertEquals(listOf(FieldError(ProfileField.HEIGHT_CM, FieldIssue.OutOfRange(120.0, 230.0))), errors)
    }

    @Test
    fun rejectsAWeightOutOfRange() = runTest {
        val errors = errorsOf(profileInput().copy(weightKg = 300.5))

        assertEquals(listOf(FieldError(ProfileField.WEIGHT_KG, FieldIssue.OutOfRange(30.0, 300.0))), errors)
    }

    @Test
    fun rejectsWaistAndHipOutOfRange() = runTest {
        val errors = errorsOf(profileInput().copy(waistCm = 39.0, hipCm = 201.0))

        assertEquals(listOf(ProfileField.WAIST_CM, ProfileField.HIP_CM), errors.map { it.field })
    }

    @Test
    fun savesWithoutTheOptionalMeasures() = runTest {
        val input = profileInput().copy(weightKg = null, waistCm = null, hipCm = null)

        val result = assertIs<SaveProfileResult.Saved>(saveProfile(input))

        assertNull(result.profile.weightKg)
    }

    @Test
    fun acceptsTheLimitsOfEveryMeasure() = runTest {
        val lowest = profileInput().copy(heightCm = 120.0, weightKg = 30.0, waistCm = 40.0, hipCm = 40.0)
        val highest = profileInput().copy(heightCm = 230.0, weightKg = 300.0, waistCm = 200.0, hipCm = 200.0)

        assertIs<SaveProfileResult.Saved>(saveProfile(lowest))
        assertIs<SaveProfileResult.Saved>(saveProfile(highest))
    }

    @Test
    fun rejectsMissingExperienceAndGoal() = runTest {
        val errors = errorsOf(profileInput().copy(experience = null, goal = null))

        assertEquals(
            listOf(
                FieldError(ProfileField.EXPERIENCE, FieldIssue.Required),
                FieldError(ProfileField.GOAL, FieldIssue.Required),
            ),
            errors,
        )
    }

    @Test
    fun rejectsTooManyPriorityMuscles() = runTest {
        val five = MuscleGroup.entries.take(5).toSet()

        val errors = errorsOf(profileInput().copy(priorityMuscles = five))

        assertEquals(listOf(FieldError(ProfileField.PRIORITY_MUSCLES, FieldIssue.TooMany(4))), errors)
    }

    @Test
    fun rejectsTextBeyondItsLength() = runTest {
        val input = profileInput().copy(
            alias = "a".repeat(41),
            limitations = "b".repeat(601),
            preferences = "c".repeat(601),
        )

        val errors = errorsOf(input)

        assertEquals(
            listOf(
                FieldError(ProfileField.ALIAS, FieldIssue.TooLong(40)),
                FieldError(ProfileField.LIMITATIONS, FieldIssue.TooLong(600)),
                FieldError(ProfileField.PREFERENCES, FieldIssue.TooLong(600)),
            ),
            errors,
        )
    }

    @Test
    fun reportsEveryErrorAtOnce() = runTest {
        val errors = errorsOf(profileInput().copy(heightCm = null, weightKg = 20.0))

        assertEquals(listOf(ProfileField.HEIGHT_CM, ProfileField.WEIGHT_KG), errors.map { it.field })
    }

    @Test
    fun trimsTextAndTreatsABlankAliasAsNone() = runTest {
        val input = profileInput().copy(alias = "   ", limitations = "  Hombro derecho  ")

        val result = assertIs<SaveProfileResult.Saved>(saveProfile(input))

        assertNull(result.profile.alias)
        assertEquals("Hombro derecho", result.profile.limitations)
    }

    @Test
    fun rejectsNumbersThatAreNotNumbersWithoutThrowing() = runTest {
        val input = profileInput().copy(
            age = Int.MIN_VALUE,
            heightCm = Double.NaN,
            weightKg = Double.POSITIVE_INFINITY,
            waistCm = Double.NEGATIVE_INFINITY,
        )

        val errors = errorsOf(input)

        assertEquals(
            listOf(ProfileField.AGE, ProfileField.HEIGHT_CM, ProfileField.WEIGHT_KG, ProfileField.WAIST_CM),
            errors.map { it.field },
        )
    }

    @Test
    fun leavesTheSavedProfileUntouchedWhenInvalid() = runTest {
        saveProfile(profileInput())
        val before = repository.saved

        saveProfile(profileInput().copy(age = 5))

        assertEquals(before, repository.saved)
    }

    @Test
    fun observesTheSavedProfile() = runTest {
        val observeProfile = ObserveProfileUseCase(repository)
        assertNull(observeProfile().first())

        saveProfile(profileInput())

        assertEquals(repository.saved, observeProfile().first())
    }
}
