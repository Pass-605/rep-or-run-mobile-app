package app.reporrun.feature.plan

import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.BlockPreferencesId
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.Experience
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.Goal
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.MetricRange
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanExercise
import app.reporrun.core.model.PlanExerciseId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import app.reporrun.core.model.PlanSummary
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.RirRange
import app.reporrun.core.model.ThemeMode
import app.reporrun.core.model.TrialLoad
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/*
 * Fakes escritos a mano, en memoria. El test del ViewModel usa el caso de uso real sobre ellos:
 * así se prueba también que el ViewModel entiende sus resultados. Todo es sintético.
 */

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/** Un reloj que avanza con el tiempo virtual del test: [NOW] más lo que haya corrido. */
internal class VirtualClock(private val elapsedMillis: () -> Long) : Clock {
    override fun now(): Instant = NOW + elapsedMillis().milliseconds
}

/**
 * Un generador como el fake de `:core:ai`, que un feature no puede usar: espera [latency] —un
 * `delay`, así que es cancelable y corre en tiempo virtual— y contesta con lo siguiente de
 * [answers]; cuando se acaban, repite la última.
 */
internal class ScriptedPlanGenerator(private val latency: Duration = 3.seconds) : PlanGenerator {
    private val answers = ArrayDeque<PlanGenerationResult>()
    private var last: PlanGenerationResult = PlanGenerationResult.Success(generatedPlan())

    /** Cuántas veces se ha pedido un plan: cada una es una llamada que costaría dinero. */
    var calls = 0
        private set

    fun thenSucceeds() = then(PlanGenerationResult.Success(generatedPlan()))

    fun thenFails(error: AiError) = then(PlanGenerationResult.Failure(error))

    /** Un plan con forma correcta que usa un aparato que no está en el inventario. */
    fun thenReturnsAPlanWithForeignEquipment() = then(
        PlanGenerationResult.Success(
            generatedPlan(days = listOf(planDay(planExercise("L01", 1, equipmentId = "eq_ajeno")))),
        ),
    )

    private fun then(answer: PlanGenerationResult) {
        answers += answer
    }

    override suspend fun generate(request: PlanGenerationRequest): PlanGenerationResult {
        calls++
        delay(latency)
        last = answers.removeFirstOrNull() ?: last
        return last
    }
}

internal class FakePlanRepository : PlanRepository {
    private val plans = MutableStateFlow<List<Plan>>(emptyList())

    val saved: List<Plan>
        get() = plans.value

    override fun observePlan(planId: PlanId): Flow<Plan?> = plans.map { all -> all.firstOrNull { it.id == planId } }

    override fun observeActivePlan(gymId: GymId): Flow<Plan?> =
        plans.map { all -> all.firstOrNull { it.gymId == gymId && it.status == PlanStatus.ACTIVE } }

    override fun observeHistory(gymId: GymId): Flow<List<PlanSummary>> = MutableStateFlow(emptyList())

    override suspend fun planIdsOn(date: LocalDate): List<PlanId> =
        plans.value.map { it.id }.filter { it.value.startsWith(PlanId.prefixOf(date)) }

    override suspend fun savePlan(plan: Plan) {
        plans.value += plan
    }

    /** Como `PlanDao.activate`: archiva el que estuviera activo en el mismo gimnasio. */
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

    override fun observeChanges(gymId: GymId): Flow<List<PlanChange>> = MutableStateFlow(emptyList())

    override suspend fun saveChange(change: PlanChange) = Unit

    override fun observeExports(planId: PlanId): Flow<List<ExportRecord>> = MutableStateFlow(emptyList())

    override suspend fun saveExport(record: ExportRecord) = Unit
}

/** Un gimnasio activo con su inventario, que es lo único que mira la generación. */
internal class FakeGymRepository(equipment: List<Equipment>) : GymRepository {
    private val gyms = MutableStateFlow(
        listOf(Gym(CENTRAL, "Gimnasio Central", isActive = true, notes = "", createdAt = NOW)),
    )
    private val equipment = MutableStateFlow(equipment)

    override fun observeGyms(): Flow<List<Gym>> = gyms

    override fun observeActiveGym(): Flow<Gym?> = gyms.map { all -> all.firstOrNull { it.isActive } }

    override suspend fun saveGym(gym: Gym) = Unit

    override suspend fun setActiveGym(gymId: GymId) = Unit

    override suspend fun deleteGym(gymId: GymId) = Unit

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> =
        equipment.map { all -> all.filter { it.gymId == gymId } }

    override suspend fun saveEquipment(equipment: List<Equipment>) = Unit

    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion = EquipmentDeletion.Deleted
}

internal class FakeProfileRepository(initial: UserProfile?) : ProfileRepository {
    private val profile = MutableStateFlow(initial)

    override fun observeProfile(): Flow<UserProfile?> = profile

    override suspend fun saveProfile(profile: UserProfile) = Unit
}

internal class FakePreferencesRepository(initial: BlockPreferences?) : PreferencesRepository {
    private val preferences = MutableStateFlow(initial)

    override fun observePreferences(): Flow<BlockPreferences?> = preferences

    override suspend fun savePreferences(preferences: BlockPreferences) = Unit
}

internal class FakeSettingsRepository(consentAcceptedAt: Instant?) : SettingsRepository {
    private val consent = MutableStateFlow(consentAcceptedAt)
    private val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    override fun observeConsentAcceptedAt(): Flow<Instant?> = consent

    override suspend fun acceptConsent(acceptedAt: Instant) = Unit

    override suspend fun revokeConsent() = Unit

    override suspend fun installId(): String = "install-test"

    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

/** Lunes 21 de septiembre de 2026. */
internal val NOW: Instant = Instant.parse("2026-09-21T14:03:11Z")

internal val CENTRAL = GymId("gym_central")

internal fun profile() = UserProfile(
    id = ProfileId("profile"),
    alias = null,
    age = 30,
    heightCm = 170.0,
    weightKg = null,
    waistCm = null,
    hipCm = null,
    experience = Experience.BEGINNER,
    goal = Goal.GENERAL_FITNESS,
    priorityMuscles = emptySet(),
    limitations = "",
    preferences = "",
    updatedAt = NOW,
)

/** Solo el lunes, que es el día de [planDay]. */
internal fun preferences() = BlockPreferences(
    id = BlockPreferencesId("preferences"),
    availableDays = setOf(DayOfWeek.MONDAY),
    sessionMinutes = 60,
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    startDate = null,
    cardioModes = emptyList(),
    avoidExercises = "",
    unit = LoadUnit.KG,
)

/** [count] aparatos del gimnasio activo: `eq_1`, `eq_2`... */
internal fun inventory(count: Int): List<Equipment> = (1..count).map {
    Equipment(
        id = EquipmentId("eq_$it"),
        gymId = CENTRAL,
        catalogKey = null,
        name = "Aparato $it",
        category = EquipmentCategory.MACHINE,
        loadType = LoadType.SELECTOR,
        minKg = null,
        maxKg = null,
        incrementKg = null,
        notes = null,
        isCustom = true,
    )
}

internal fun planExercise(code: String, order: Int, equipmentId: String = "eq_1") = PlanExercise(
    id = PlanExerciseId("exercise_$code"),
    code = ExerciseCode(code),
    order = order,
    name = "Ejercicio $code",
    equipmentId = EquipmentId(equipmentId),
    sets = 3,
    dose = ExerciseDose.Reps(MetricRange(low = 8, high = 12), RirRange(low = 2, high = 2)),
    rest = "2 min",
    trial = TrialLoad("Carga de ensayo"),
    target = "Pecho",
    unilateral = false,
    alternatives = emptyList(),
)

internal fun planDay(vararg exercises: PlanExercise) = PlanDay(
    id = PlanDayId("day_monday"),
    dayOfWeek = DayOfWeek.MONDAY,
    dayIndex = 0,
    session = "Sesión",
    priority = "Prioridad",
    cardio = null,
    warmup = "Calentamiento",
    mobility = "Movilidad",
    cooldown = "Vuelta a la calma",
    exercises = exercises.toList(),
)

/** Lo que devuelve un generador: por defecto, un plan válido para [preferences] e [inventory]. */
internal fun generatedPlan(
    days: List<PlanDay> = listOf(planDay(planExercise("L01", 1), planExercise("L02", 2, "eq_2"))),
) = GeneratedPlan(
    blockDays = BlockPreferences.BLOCK_DAYS_V1,
    focus = listOf("Fuerza básica"),
    notes = listOf("Las cargas son de ensayo"),
    days = days,
    model = "modelo-de-prueba",
    promptVersion = "generate-v1",
    schemaVersion = "1.0",
    generatedAt = NOW,
    inputSnapshotJson = """{"schemaVersion":"1.0"}""",
)
