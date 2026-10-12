package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.domain.repository.SettingsRepository
import app.reporrun.core.domain.usecase.InventoryRules.MIN_EQUIPMENT_TO_GENERATE
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.Gym
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Algo que falta para poder generar un plan. */
sealed interface GenerationBlocker {
    /** No hay perfil guardado. */
    data object MissingProfile : GenerationBlocker

    /** No hay gimnasio activo. */
    data object MissingActiveGym : GenerationBlocker

    /** El gimnasio activo tiene [current] aparatos y hacen falta [required]. */
    data class NotEnoughEquipment(val current: Int, val required: Int) : GenerationBlocker

    /**
     * No hay preferencias guardadas. Incluye «ningún día disponible»: unas preferencias sin
     * días no se pueden guardar.
     */
    data object MissingPreferences : GenerationBlocker

    /** La persona no ha aceptado el envío de sus datos al LLM. */
    data object MissingConsent : GenerationBlocker
}

/**
 * Lo que se va a enviar para generar y lo que falta para poder hacerlo.
 *
 * @property preferences las guardadas, con la fecha de inicio que vale hoy: una que ya pasó
 *   llega como `null` (`BlockPreferences.effectiveStartDate`). Es lo que se envía.
 * @property equipment inventario del gimnasio activo; vacío si no hay gimnasio activo.
 * @property blockers todo lo que falta, no solo lo primero. Vacía si se puede generar.
 */
data class GenerationSummary(
    val profile: UserProfile?,
    val gym: Gym?,
    val equipment: List<Equipment>,
    val preferences: BlockPreferences?,
    val consentAccepted: Boolean,
    val blockers: List<GenerationBlocker>,
) {
    val canGenerate: Boolean
        get() = blockers.isEmpty()
}

/**
 * Resumen previo a generar, reactivo: vuelve a emitir cuando cambia el perfil, el gimnasio
 * activo, su inventario, las preferencias o el consentimiento. «Hoy» se lee del [clock] en
 * cada emisión, en la zona horaria de la persona.
 */
class BuildGenerationSummaryUseCase(
    private val profileRepository: ProfileRepository,
    private val gymRepository: GymRepository,
    private val preferencesRepository: PreferencesRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    operator fun invoke(): Flow<GenerationSummary> = combine(
        profileRepository.observeProfile(),
        activeGymWithInventory(),
        preferencesRepository.observePreferences(),
        settingsRepository.observeConsentAcceptedAt(),
    ) { profile, (gym, equipment), saved, consentAcceptedAt ->
        val consentAccepted = consentAcceptedAt != null
        val today = clock.now().toLocalDateTime(timeZone).date
        val preferences = saved?.let { it.copy(startDate = it.effectiveStartDate(today)) }
        GenerationSummary(
            profile = profile,
            gym = gym,
            equipment = equipment,
            preferences = preferences,
            consentAccepted = consentAccepted,
            blockers = buildList {
                if (profile == null) add(GenerationBlocker.MissingProfile)
                if (gym == null) {
                    add(GenerationBlocker.MissingActiveGym)
                } else if (equipment.size < MIN_EQUIPMENT_TO_GENERATE) {
                    add(GenerationBlocker.NotEnoughEquipment(equipment.size, MIN_EQUIPMENT_TO_GENERATE))
                }
                if (preferences == null) add(GenerationBlocker.MissingPreferences)
                if (!consentAccepted) add(GenerationBlocker.MissingConsent)
            },
        )
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun activeGymWithInventory(): Flow<Pair<Gym?, List<Equipment>>> =
        gymRepository.observeActiveGym().flatMapLatest { gym ->
            if (gym == null) {
                flowOf(null to emptyList())
            } else {
                gymRepository.observeEquipment(gym.id).map { gym to it }
            }
        }
}
