package app.reporrun.scaffold

import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Fakes escritos a mano, en memoria. El test de un ViewModel usa los casos de uso reales
 * sobre repositorios fake: así se prueba también que el ViewModel entiende sus resultados.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/** Devuelve `id1`, `id2`... */
internal class SequentialIds : IdGenerator {
    private var next = 1

    override fun newId(): String = "id${next++}"
}

internal class FakeGymRepository : GymRepository {
    private val gyms = MutableStateFlow<List<Gym>>(emptyList())

    val savedGyms: List<Gym>
        get() = gyms.value

    override fun observeGyms(): Flow<List<Gym>> = gyms

    override fun observeActiveGym(): Flow<Gym?> = gyms.map { all -> all.firstOrNull { it.isActive } }

    override suspend fun saveGym(gym: Gym) {
        gyms.value = gyms.value.filterNot { it.id == gym.id } + gym
    }

    override suspend fun setActiveGym(gymId: GymId) {
        gyms.value = gyms.value.map { it.copy(isActive = it.id == gymId) }
    }

    override suspend fun deleteGym(gymId: GymId) {
        gyms.value = gyms.value.filterNot { it.id == gymId }
    }

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> = flowOf(emptyList())

    override suspend fun saveEquipment(equipment: List<Equipment>) = Unit

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion = EquipmentDeletion.Deleted
}

internal class FakeProfileRepository : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = flowOf(null)

    override suspend fun saveProfile(profile: UserProfile) = Unit
}

internal class FakePreferencesRepository : PreferencesRepository {
    override fun observePreferences(): Flow<BlockPreferences?> = flowOf(null)

    override suspend fun savePreferences(preferences: BlockPreferences) = Unit
}

internal class FakeSettingsRepository : SettingsRepository {
    override fun observeConsentAcceptedAt(): Flow<Instant?> = flowOf(null)

    override suspend fun acceptConsent(acceptedAt: Instant) = Unit

    override suspend fun revokeConsent() = Unit

    override suspend fun installId(): String = "install-test"
}
