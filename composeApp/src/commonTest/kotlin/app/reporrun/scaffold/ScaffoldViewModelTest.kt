package app.reporrun.scaffold

import app.cash.turbine.test
import app.reporrun.core.domain.usecase.BuildGenerationSummaryUseCase
import app.reporrun.core.domain.usecase.CreateGymUseCase
import app.reporrun.core.domain.usecase.FieldIssue
import app.reporrun.core.model.Gym
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
import kotlin.time.Instant

/**
 * Test de referencia de un ViewModel: se construye a mano (sin Koin ni Compose), con los casos
 * de uso reales sobre repositorios fake, y se le habla solo por `onEvent`.
 *
 * `viewModelScope` corre en `Dispatchers.Main`, que en un test no existe: se sustituye por un
 * `UnconfinedTestDispatcher`, que ejecuta cada corrutina en el acto, así que tras un `onEvent`
 * el estado ya está al día.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScaffoldViewModelTest {
    private val gyms = FakeGymRepository()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun viewModel() = ScaffoldViewModel(
        observeSummary = BuildGenerationSummaryUseCase(
            FakeProfileRepository(),
            gyms,
            FakePreferencesRepository(),
            FakeSettingsRepository(),
        ),
        createGym = CreateGymUseCase(gyms, SequentialIds(), FixedClock(NOW)),
    )

    @Test
    fun showsWhatIsMissingWhenNothingIsSaved() = runTest {
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertNull(state.activeGymName)
        // Perfil, gimnasio activo, preferencias y consentimiento.
        assertEquals(4, state.pendingToGenerate)
    }

    @Test
    fun keepsWhatThePersonTypes() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ScaffoldEvent.GymNameChanged("Gimnasio del barrio"))

        assertEquals("Gimnasio del barrio", viewModel.state.value.gymName)
    }

    @Test
    fun rejectsBlankNameInTheFieldWithoutNavigating() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(ScaffoldEvent.GymNameChanged("   "))
            viewModel.onEvent(ScaffoldEvent.CreateGymClicked)

            assertEquals(FieldIssue.Required, viewModel.state.value.gymNameIssue)
            assertFalse(viewModel.state.value.isSaving)
            expectNoEvents()
        }
        assertEquals(emptyList(), gyms.savedGyms)
    }

    @Test
    fun rejectsTooLongNameWithItsLimit() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ScaffoldEvent.GymNameChanged("a".repeat(Gym.MAX_NAME_LENGTH + 1)))
        viewModel.onEvent(ScaffoldEvent.CreateGymClicked)

        assertEquals(FieldIssue.TooLong(Gym.MAX_NAME_LENGTH), viewModel.state.value.gymNameIssue)
    }

    @Test
    fun clearsTheIssueWhenThePersonEditsTheField() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(ScaffoldEvent.CreateGymClicked)

        viewModel.onEvent(ScaffoldEvent.GymNameChanged("G"))

        assertNull(viewModel.state.value.gymNameIssue)
    }

    @Test
    fun emitsGymCreatedOnceAndClearsTheForm() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(ScaffoldEvent.GymNameChanged("Gimnasio del barrio"))
            viewModel.onEvent(ScaffoldEvent.CreateGymClicked)

            assertEquals(ScaffoldEffect.GymCreated(GymId("gym_id1")), awaitItem())
            expectNoEvents()
        }
        assertEquals("", viewModel.state.value.gymName)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun followsTheDomainAfterCreatingTheGym() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertNull(awaitItem().activeGymName)

            viewModel.onEvent(ScaffoldEvent.GymNameChanged("Gimnasio del barrio"))
            viewModel.onEvent(ScaffoldEvent.CreateGymClicked)

            // El primer gimnasio queda activo, y la pantalla se entera por el Flow del dominio,
            // no porque el ViewModel lo apunte a mano.
            val settled = expectMostRecentItem()
            assertEquals("Gimnasio del barrio", settled.activeGymName)
            // Sigue faltando lo mismo en número: desaparece «sin gimnasio» y aparece «pocos aparatos».
            assertEquals(4, settled.pendingToGenerate)
        }
    }

    @Test
    fun keepsAnEffectUntilSomeoneCollectsIt() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(ScaffoldEvent.GymNameChanged("Gimnasio del barrio"))
        viewModel.onEvent(ScaffoldEvent.CreateGymClicked)

        // Nadie escuchaba cuando se envió (la app estaba en segundo plano): no se pierde.
        viewModel.effects.test {
            assertEquals(ScaffoldEffect.GymCreated(GymId("gym_id1")), awaitItem())
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-02T10:00:00Z")
    }
}
