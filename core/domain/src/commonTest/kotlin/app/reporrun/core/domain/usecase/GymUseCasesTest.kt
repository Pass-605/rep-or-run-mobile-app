package app.reporrun.core.domain.usecase

import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GymUseCasesTest {
    private val repository = FakeGymRepository()
    private val createGym = CreateGymUseCase(repository, SequentialIds(), FixedClock(NOW))
    private val setActiveGym = SetActiveGymUseCase(repository)

    @Test
    fun createsAGymWithATrimmedNameAndTheClockDate() = runTest {
        val result = assertIs<CreateGymResult.Created>(createGym("  Gimnasio Central  ", notes = " Segundo piso "))

        assertEquals(GymId("gym_id1"), result.gym.id)
        assertEquals("Gimnasio Central", result.gym.name)
        assertEquals("Segundo piso", result.gym.notes)
        assertEquals(NOW, result.gym.createdAt)
    }

    @Test
    fun makesTheFirstGymActive() = runTest {
        val result = assertIs<CreateGymResult.Created>(createGym("Gimnasio Central"))

        assertTrue(result.gym.isActive)
        assertEquals(result.gym.id, repository.observeActiveGym().first()?.id)
    }

    @Test
    fun leavesTheActiveGymAloneWhenCreatingASecondOne() = runTest {
        val first = assertIs<CreateGymResult.Created>(createGym("Gimnasio Central")).gym

        val second = assertIs<CreateGymResult.Created>(createGym("Gimnasio Norte")).gym

        assertEquals(false, second.isActive)
        assertEquals(first.id, repository.observeActiveGym().first()?.id)
        assertEquals(1, repository.savedGyms.count { it.isActive })
    }

    @Test
    fun rejectsABlankName() = runTest {
        val result = createGym("   ")

        assertEquals(CreateGymResult.InvalidName(FieldIssue.Required), result)
        assertTrue(repository.savedGyms.isEmpty())
    }

    @Test
    fun rejectsANameThatIsTooLong() = runTest {
        val result = createGym("g".repeat(81))

        assertEquals(CreateGymResult.InvalidName(FieldIssue.TooLong(80)), result)
    }

    @Test
    fun acceptsANameAtTheLengthLimit() = runTest {
        assertIs<CreateGymResult.Created>(createGym("g".repeat(80)))
    }

    @Test
    fun activatesAnExistingGymAndDeactivatesTheOther() = runTest {
        repository.saveGym(gym(isActive = true))
        repository.saveGym(gym(GymId("gym_norte")))

        val result = setActiveGym(GymId("gym_norte"))

        assertEquals(SetActiveGymResult.Activated, result)
        assertEquals(listOf(GymId("gym_norte")), repository.savedGyms.filter { it.isActive }.map { it.id })
    }

    @Test
    fun keepsTheActiveGymWhenAskedForOneThatDoesNotExist() = runTest {
        repository.saveGym(gym(isActive = true))

        val result = setActiveGym(GymId("gym_borrado"))

        assertEquals(SetActiveGymResult.GymNotFound, result)
        assertEquals(GYM_ID, repository.observeActiveGym().first()?.id)
    }
}
