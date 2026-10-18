package app.reporrun.feature.settings

import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.ThemeMode
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
