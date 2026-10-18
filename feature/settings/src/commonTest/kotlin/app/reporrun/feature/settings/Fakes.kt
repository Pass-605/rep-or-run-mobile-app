package app.reporrun.feature.settings

import app.reporrun.core.domain.port.SampleGym
import app.reporrun.core.domain.port.SampleGymSource
import app.reporrun.core.domain.repository.DeveloperSettingsRepository
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.model.AiBackend
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.ThemeMode
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/* Fakes escritos a mano, en memoria: el ViewModel se prueba con los casos de uso reales. */

internal class FakeSettingsRepository(consentAcceptedAt: Instant? = null) : SettingsRepository {
    private val consent = MutableStateFlow(consentAcceptedAt)
    private val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    /** Cuántas veces se ha escrito el tema. */
    var themeWrites = 0
        private set

    val consentAcceptedAt: Instant?
        get() = consent.value

    override fun observeConsentAcceptedAt(): Flow<Instant?> = consent

    override suspend fun acceptConsent(acceptedAt: Instant) {
        consent.value = acceptedAt
    }

    override suspend fun revokeConsent() {
        consent.value = null
    }

    override suspend fun installId(): String = "install-test"

    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeWrites++
        themeMode.value = mode
    }
}

internal class FakeProfileRepository(profile: UserProfile? = null) : ProfileRepository {
    private val stored = MutableStateFlow(profile)

    override fun observeProfile(): Flow<UserProfile?> = stored

    override suspend fun saveProfile(profile: UserProfile) {
        stored.value = profile
    }
}

internal class FakeDeveloperSettingsRepository : DeveloperSettingsRepository {
    private val backend = MutableStateFlow(AiBackend.FAKE)

    val stored: AiBackend
        get() = backend.value

    override fun observeAiBackend(): Flow<AiBackend> = backend

    override suspend fun setAiBackend(backend: AiBackend) {
        this.backend.value = backend
    }
}

/** Lo justo para cargar el gimnasio de ejemplo: guardar, activar y leer. */
internal class FakeGymRepository : GymRepository {
    private val gyms = MutableStateFlow<List<Gym>>(emptyList())
    private val equipment = MutableStateFlow<List<Equipment>>(emptyList())

    override fun observeGyms(): Flow<List<Gym>> = gyms

    override fun observeActiveGym(): Flow<Gym?> = gyms.map { all -> all.firstOrNull { it.isActive } }

    override suspend fun saveGym(gym: Gym) {
        gyms.value = gyms.value.filterNot { it.id == gym.id } + gym
    }

    override suspend fun setActiveGym(gymId: GymId) {
        gyms.value = gyms.value.map { it.copy(isActive = it.id == gymId) }
    }

    override suspend fun deleteGym(gymId: GymId) = error("no se usa")

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> =
        equipment.map { all -> all.filter { it.gymId == gymId } }

    override suspend fun saveEquipment(equipment: List<Equipment>) {
        val ids = equipment.map { it.id }.toSet()
        this.equipment.value = this.equipment.value.filterNot { it.id in ids } + equipment
    }

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion = error("no se usa")
}

internal class FakePreferencesRepository : PreferencesRepository {
    private val stored = MutableStateFlow<BlockPreferences?>(null)

    override fun observePreferences(): Flow<BlockPreferences?> = stored

    override suspend fun savePreferences(preferences: BlockPreferences) {
        stored.value = preferences
    }
}

/** Un gimnasio de ejemplo sintético, sin aparatos: aquí solo importa que se cargue. */
internal val SampleGymFixture = SampleGymSource {
    SampleGym(
        name = "Gimnasio de ejemplo",
        equipment = emptyList(),
        preferences = BlockPreferences(
            id = BlockPreferencesId("preferences"),
            availableDays = setOf(DayOfWeek.MONDAY),
            sessionMinutes = 75,
            blockDays = BlockPreferences.BLOCK_DAYS_V1,
            startDate = null,
            cardioModes = emptyList(),
            avoidExercises = "",
            unit = LoadUnit.KG,
        ),
    )
}

/** Perfil sintético: ninguna persona real. */
internal fun profile(age: Int = 30, experience: Experience = Experience.INTERMEDIATE) = UserProfile(
    id = ProfileId("profile"),
    alias = null,
    age = age,
    heightCm = 170.0,
    weightKg = null,
    waistCm = null,
    hipCm = null,
    experience = experience,
    goal = Goal.RECOMPOSITION,
    priorityMuscles = emptySet(),
    limitations = "",
    preferences = "",
    updatedAt = Instant.parse("2026-09-21T14:03:11Z"),
)
