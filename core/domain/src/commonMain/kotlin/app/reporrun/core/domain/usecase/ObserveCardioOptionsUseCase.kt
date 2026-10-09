package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Los modos de cardio que se pueden preferir: los nombres de los aparatos de categoría
 * [EquipmentCategory.CARDIO] del gimnasio activo, sin repetir y por orden alfabético. Vacía si
 * no hay gimnasio activo o si no tiene ninguno.
 *
 * El contrato no tiene un enum de modos (`cardioModes` es texto), así que la lista no se escribe
 * a mano: sale del inventario. De paso, la persona no puede preferir un cardio para el que su
 * gimnasio no tiene aparato (plan §4.1).
 *
 * Un nombre más largo que [BlockPreferences.MAX_CARDIO_MODE_LENGTH] se recorta a ese límite: el
 * nombre de un aparato admite más caracteres que un modo de cardio.
 */
class ObserveCardioOptionsUseCase(private val repository: GymRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<String>> = repository.observeActiveGym()
        .flatMapLatest { gym ->
            if (gym == null) flowOf(emptyList()) else repository.observeEquipment(gym.id).map { it.cardioOptions() }
        }
        .distinctUntilChanged()
}

/** Misma limpieza que la de guardar: recortados y sin repetidos, ignorando mayúsculas. */
internal fun List<Equipment>.cardioOptions(): List<String> = filter { it.category == EquipmentCategory.CARDIO }
    .map { it.name.trim().take(BlockPreferences.MAX_CARDIO_MODE_LENGTH).trim() }
    .distinctBy { it.lowercase() }
    .sortedBy { it.lowercase() }
