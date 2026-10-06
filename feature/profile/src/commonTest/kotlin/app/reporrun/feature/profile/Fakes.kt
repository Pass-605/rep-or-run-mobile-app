package app.reporrun.feature.profile

import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Fakes escritos a mano, en memoria. El test del ViewModel usa los casos de uso reales sobre
 * un repositorio fake: así se prueba también que el ViewModel entiende sus resultados.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

internal class FakeProfileRepository(initial: UserProfile? = null) : ProfileRepository {
    private val profile = MutableStateFlow(initial)

    /** Cuántas veces se ha escrito el perfil. */
    var saveCalls = 0
        private set

    val saved: UserProfile?
        get() = profile.value

    override fun observeProfile(): Flow<UserProfile?> = profile

    override suspend fun saveProfile(profile: UserProfile) {
        saveCalls++
        this.profile.value = profile
    }
}

internal val NOW: Instant = Instant.parse("2026-10-05T10:00:00Z")

/** Perfil sintético: ninguna persona real. */
internal fun savedProfile() = UserProfile(
    id = ProfileId("profile_previo"),
    alias = "Sol",
    age = 30,
    heightCm = 170.0,
    weightKg = 72.5,
    waistCm = 84.0,
    hipCm = null,
    experience = Experience.INTERMEDIATE,
    goal = Goal.RECOMPOSITION,
    priorityMuscles = setOf(MuscleGroup.BACK, MuscleGroup.GLUTES),
    limitations = "Molestia ocasional en el hombro derecho.",
    preferences = "",
    updatedAt = Instant.parse("2026-09-21T14:03:11Z"),
)
