package app.reporrun.core.domain.repository

import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.Flow

/**
 * Gimnasios y su inventario. Solo un gimnasio está activo, y su inventario es el que usa el
 * plan (plan §4.1).
 */
interface GymRepository {
    /** Todos los gimnasios, del más antiguo al más reciente. */
    fun observeGyms(): Flow<List<Gym>>

    /** El gimnasio activo, o `null` si no hay ninguno. */
    fun observeActiveGym(): Flow<Gym?>

    /** Crea o sustituye un gimnasio. No cambia cuál está activo: para eso, [setActiveGym]. */
    suspend fun saveGym(gym: Gym)

    /** Deja [gymId] como único gimnasio activo. */
    suspend fun setActiveGym(gymId: GymId)

    /** Borra el gimnasio con su inventario, sus planes y todo lo que cuelga de ellos. */
    suspend fun deleteGym(gymId: GymId)

    /** Inventario de [gymId], por nombre. */
    fun observeEquipment(gymId: GymId): Flow<List<Equipment>>

    /** Crea o sustituye aparatos del inventario. */
    suspend fun saveEquipment(equipment: List<Equipment>)

    /**
     * Borra un aparato si ningún plan lo usa, archivados incluidos: la versión anterior de un
     * plan se conserva entera (plan §4.12).
     */
    suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion
}

/** Resultado de [GymRepository.deleteEquipment]. */
sealed interface EquipmentDeletion {
    /** El aparato ya no está en el inventario. */
    data object Deleted : EquipmentDeletion

    /**
     * No se borró: lo usan [usages] ejercicios, alternativas o propuestas de algún plan. La
     * persona puede dejarlo fuera del plan siguiente, pero no borrarlo.
     */
    data class InUse(val usages: Int) : EquipmentDeletion
}
