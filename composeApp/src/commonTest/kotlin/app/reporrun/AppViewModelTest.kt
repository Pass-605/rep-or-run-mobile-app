package app.reporrun

import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.domain.usecase.LoadCatalogSeedUseCase
import app.reporrun.core.domain.usecase.ObserveConsentUseCase
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private val catalog = FakeCatalogRepository()

    private fun viewModel(
        settings: SettingsRepository,
        profiles: ProfileRepository = FakeProfileRepository(Stored(null)),
    ) = AppViewModel(
        observeConsent = ObserveConsentUseCase(settings),
        observeProfile = ObserveProfileUseCase(profiles),
        loadCatalogSeed = LoadCatalogSeedUseCase(catalog),
        dispatchers = AppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun startsInOnboardingWithoutConsent() = runTest {
        val viewModel = viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = null)))

        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    @Test
    fun startsInTheProfileWhenConsentWasGivenAndThereIsNoProfile() = runTest {
        val viewModel = viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)))

        assertEquals(StartDestination.Profile, viewModel.state.value.start)
    }

    @Test
    fun startsInTheGymsWhenTheProfileIsAlreadySaved() = runTest {
        val viewModel = viewModel(
            settings = FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)),
            profiles = FakeProfileRepository(Stored(savedProfile())),
        )

        assertEquals(StartDestination.Gyms, viewModel.state.value.start)
    }

    @Test
    fun asksForConsentFirstEvenWithASavedProfile() = runTest {
        val viewModel = viewModel(
            settings = FakeSettingsRepository(Loaded(consentAcceptedAt = null)),
            profiles = FakeProfileRepository(Stored(savedProfile())),
        )

        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    @Test
    fun staysLoadingUntilStorageAnswers() = runTest {
        val settings = FakeSettingsRepository(stored = null)
        val viewModel = viewModel(settings)
        assertEquals(StartDestination.Loading, viewModel.state.value.start)

        settings.stored.value = Loaded(consentAcceptedAt = null)

        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    @Test
    fun staysLoadingUntilTheProfileIsRead() = runTest {
        val profiles = FakeProfileRepository(stored = null)
        val viewModel = viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)), profiles)
        assertEquals(StartDestination.Loading, viewModel.state.value.start)

        profiles.stored.value = Stored(savedProfile())

        assertEquals(StartDestination.Gyms, viewModel.state.value.start)
    }

    @Test
    fun doesNotChangeTheStartWhenConsentIsGivenLater() = runTest {
        val settings = FakeSettingsRepository(Loaded(consentAcceptedAt = null))
        val viewModel = viewModel(settings)

        settings.acceptConsent(NOW)

        // A dónde se va al aceptar lo decide la navegación, no el destino inicial.
        assertEquals(StartDestination.Onboarding, viewModel.state.value.start)
    }

    @Test
    fun doesNotChangeTheStartWhenTheProfileIsSavedLater() = runTest {
        val profiles = FakeProfileRepository(Stored(null))
        val viewModel = viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)), profiles)

        profiles.saveProfile(savedProfile())

        // A dónde se va al guardar lo decide la navegación, no el destino inicial.
        assertEquals(StartDestination.Profile, viewModel.state.value.start)
    }

    @Test
    fun loadsTheCatalogSeedOnEveryStart() = runTest {
        viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)))
        viewModel(FakeSettingsRepository(Loaded(consentAcceptedAt = NOW)))

        assertEquals(2, catalog.loadCalls)
    }

    @Test
    fun loadsTheCatalogSeedWithoutWaitingForTheStartDestination() = runTest {
        val viewModel = viewModel(FakeSettingsRepository(stored = null))

        assertEquals(StartDestination.Loading, viewModel.state.value.start)
        assertEquals(1, catalog.loadCalls)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-05T10:00:00Z")
    }
}

/** Lo que hay guardado, una vez leído. Distingue «sin consentimiento» de «aún no se ha leído». */
private class Loaded(val consentAcceptedAt: Instant?)

/** [stored] en `null` imita a un DataStore que todavía no ha contestado. */
private class FakeSettingsRepository(stored: Loaded?) : SettingsRepository {
    val stored = MutableStateFlow(stored)

    override fun observeConsentAcceptedAt(): Flow<Instant?> = stored.filterNotNull().map { it.consentAcceptedAt }

    override suspend fun acceptConsent(acceptedAt: Instant) {
        stored.value = Loaded(acceptedAt)
    }

    override suspend fun revokeConsent() {
        stored.value = Loaded(null)
    }

    override suspend fun installId(): String = "install-test"
}

/** Cuenta las cargas de la semilla; el catálogo en sí no le importa a la raíz. */
private class FakeCatalogRepository : EquipmentCatalogRepository {
    var loadCalls = 0
        private set

    override fun observeCatalog(): Flow<List<EquipmentCatalogItem>> = MutableStateFlow(emptyList())

    override suspend fun loadSeed(): Int {
        loadCalls++
        return 0
    }
}

/** El perfil guardado, una vez leído. Distingue «sin perfil» de «aún no se ha leído». */
private class Stored(val profile: UserProfile?)

/** [stored] en `null` imita a una base de datos que todavía no ha contestado. */
private class FakeProfileRepository(stored: Stored?) : ProfileRepository {
    val stored = MutableStateFlow(stored)

    override fun observeProfile(): Flow<UserProfile?> = stored.filterNotNull().map { it.profile }

    override suspend fun saveProfile(profile: UserProfile) {
        stored.value = Stored(profile)
    }
}

/** Perfil sintético: ninguna persona real. */
private fun savedProfile() = UserProfile(
    id = ProfileId("profile_previo"),
    alias = "Sol",
    age = 30,
    heightCm = 170.0,
    weightKg = null,
    waistCm = null,
    hipCm = null,
    experience = Experience.INTERMEDIATE,
    goal = Goal.RECOMPOSITION,
    priorityMuscles = emptySet(),
    limitations = "",
    preferences = "",
    updatedAt = Instant.parse("2026-09-21T14:03:11Z"),
)
