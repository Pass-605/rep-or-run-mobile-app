package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import app.reporrun.core.model.PlanSummary
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Repositorios fake escritos a mano: guardan en memoria y se comportan como los de :core:data
 * en lo que los casos de uso pueden notar.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/** Devuelve `id1`, `id2`... */
internal class SequentialIds : IdGenerator {
    private var next = 1

    override fun newId(): String = "id${next++}"
}

internal class FakeProfileRepository(initial: UserProfile? = null) : ProfileRepository {
    private val profile = MutableStateFlow(initial)

    val saved: UserProfile?
        get() = profile.value

    override fun observeProfile(): Flow<UserProfile?> = profile

    override suspend fun saveProfile(profile: UserProfile) {
        this.profile.value = profile
    }
}

internal class FakePreferencesRepository(initial: BlockPreferences? = null) : PreferencesRepository {
    private val preferences = MutableStateFlow(initial)

    val saved: BlockPreferences?
        get() = preferences.value

    override fun observePreferences(): Flow<BlockPreferences?> = preferences

    override suspend fun savePreferences(preferences: BlockPreferences) {
        this.preferences.value = preferences
    }
}

internal class FakeGymRepository : GymRepository {
    private val gyms = MutableStateFlow<List<Gym>>(emptyList())
    private val equipment = MutableStateFlow<List<Equipment>>(emptyList())

    /** Cuántos usos en planes tiene cada aparato; con alguno, no se puede borrar. */
    val usages = mutableMapOf<EquipmentId, Int>()

    val savedGyms: List<Gym>
        get() = gyms.value

    val savedEquipment: List<Equipment>
        get() = equipment.value

    override fun observeGyms(): Flow<List<Gym>> = gyms.map { all -> all.sortedBy { it.createdAt } }

    override fun observeActiveGym(): Flow<Gym?> = gyms.map { all -> all.firstOrNull { it.isActive } }

    override suspend fun saveGym(gym: Gym) {
        gyms.value = gyms.value.filterNot { it.id == gym.id } + gym
    }

    /** Como `GymDao.setActive`: desactiva todos y marca uno, exista o no. */
    override suspend fun setActiveGym(gymId: GymId) {
        gyms.value = gyms.value.map { it.copy(isActive = it.id == gymId) }
    }

    override suspend fun deleteGym(gymId: GymId) {
        gyms.value = gyms.value.filterNot { it.id == gymId }
        equipment.value = equipment.value.filterNot { it.gymId == gymId }
    }

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> =
        equipment.map { all -> all.filter { it.gymId == gymId }.sortedBy { it.name } }

    override suspend fun saveEquipment(equipment: List<Equipment>) {
        val replaced = equipment.map { it.id }.toSet()
        this.equipment.value = this.equipment.value.filterNot { it.id in replaced } + equipment
    }

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion {
        val inUse = usages[equipmentId] ?: 0
        if (inUse > 0) return EquipmentDeletion.InUse(inUse)
        equipment.value = equipment.value.filterNot { it.id == equipmentId }
        return EquipmentDeletion.Deleted
    }
}

internal class FakeEquipmentCatalogRepository(items: List<EquipmentCatalogItem>) : EquipmentCatalogRepository {
    private val catalog = MutableStateFlow(items)

    override fun observeCatalog(): Flow<List<EquipmentCatalogItem>> = catalog

    override suspend fun loadSeed(): Int = 0
}

/** Cuenta las cargas de la semilla y devuelve [inserted] en cada una. */
internal class CountingCatalogRepository(private val inserted: Int) : EquipmentCatalogRepository {
    var loadCalls = 0
        private set

    override fun observeCatalog(): Flow<List<EquipmentCatalogItem>> = MutableStateFlow(emptyList())

    override suspend fun loadSeed(): Int {
        loadCalls++
        return inserted
    }
}

internal class FakeSettingsRepository(consentAcceptedAt: Instant? = null) : SettingsRepository {
    private val consent = MutableStateFlow(consentAcceptedAt)

    override fun observeConsentAcceptedAt(): Flow<Instant?> = consent

    override suspend fun acceptConsent(acceptedAt: Instant) {
        consent.value = acceptedAt
    }

    override suspend fun revokeConsent() {
        consent.value = null
    }

    override suspend fun installId(): String = "install-test"
}

/** Como `PlanRepositoryImpl`: un plan se inserta entero y un id repetido falla. */
internal class FakePlanRepository : PlanRepository {
    private val plans = MutableStateFlow<List<Plan>>(emptyList())
    private val changes = MutableStateFlow<List<PlanChange>>(emptyList())
    private val exports = MutableStateFlow<List<ExportRecord>>(emptyList())

    val saved: List<Plan>
        get() = plans.value

    override fun observePlan(planId: PlanId): Flow<Plan?> = plans.map { all -> all.firstOrNull { it.id == planId } }

    override fun observeActivePlan(gymId: GymId): Flow<Plan?> =
        plans.map { all -> all.firstOrNull { it.gymId == gymId && it.status == PlanStatus.ACTIVE } }

    override fun observeHistory(gymId: GymId): Flow<List<PlanSummary>> = plans.map { all ->
        all.filter { it.gymId == gymId }.sortedByDescending { it.createdAt }.map {
            PlanSummary(it.id, it.gymId, it.createdAt, it.status, it.startDate, it.reviewDate, it.focus)
        }
    }

    override suspend fun planIdsOn(date: LocalDate): List<PlanId> =
        plans.value.map { it.id }.filter { it.value.startsWith(PlanId.prefixOf(date)) }

    override suspend fun savePlan(plan: Plan) {
        check(plans.value.none { it.id == plan.id }) { "ya existe ${plan.id.value}" }
        plans.value += plan
    }

    override suspend fun activatePlan(planId: PlanId) {
        val gymId = plans.value.firstOrNull { it.id == planId }?.gymId
        plans.value = plans.value.map {
            when {
                it.id == planId -> it.copy(status = PlanStatus.ACTIVE)
                it.gymId == gymId && it.status == PlanStatus.ACTIVE -> it.copy(status = PlanStatus.ARCHIVED)
                else -> it
            }
        }
    }

    override suspend fun setDayStatus(dayId: PlanDayId, status: DayStatus) {
        plans.value = plans.value.map { plan ->
            plan.copy(days = plan.days.map { if (it.id == dayId) it.copy(status = status) else it })
        }
    }

    override fun observeChanges(gymId: GymId): Flow<List<PlanChange>> = changes

    override suspend fun saveChange(change: PlanChange) {
        changes.value += change
    }

    override fun observeExports(planId: PlanId): Flow<List<ExportRecord>> =
        exports.map { all -> all.filter { it.planId == planId } }

    override suspend fun saveExport(record: ExportRecord) {
        exports.value += record
    }
}

/** Guarda lo que se le pide y contesta con [answer]; por defecto, [generatedPlan]. */
internal class RecordingPlanGenerator : PlanGenerator {
    val requests = mutableListOf<PlanGenerationRequest>()

    var answer: suspend (PlanGenerationRequest) -> PlanGenerationResult = {
        PlanGenerationResult.Success(generatedPlan())
    }

    fun fails(error: AiError) {
        answer = { PlanGenerationResult.Failure(error) }
    }

    override suspend fun generate(request: PlanGenerationRequest): PlanGenerationResult {
        requests += request
        return answer(request)
    }
}
