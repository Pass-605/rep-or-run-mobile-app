package app.reporrun.core.data.repository

import app.reporrun.core.data.mapper.toEntity
import app.reporrun.core.data.mapper.toModel
import app.reporrun.core.database.dao.EquipmentDao
import app.reporrun.core.database.dao.GymDao
import app.reporrun.core.domain.repository.EquipmentDeletion
import app.reporrun.core.domain.repository.GymRepository
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.Gym
import app.reporrun.core.model.GymId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * El gimnasio activo es la columna `gym.isActive` de Room, no una clave de DataStore: así hay
 * una sola fuente y [GymDao.setActive] la cambia en una transacción.
 */
class GymRepositoryImpl(private val gymDao: GymDao, private val equipmentDao: EquipmentDao) : GymRepository {
    override fun observeGyms(): Flow<List<Gym>> = gymDao.observeGyms().map { rows -> rows.map { it.toModel() } }

    override fun observeActiveGym(): Flow<Gym?> = gymDao.observeActiveGym().map { it?.toModel() }

    override suspend fun saveGym(gym: Gym) = gymDao.upsertGym(gym.toEntity())

    override suspend fun setActiveGym(gymId: GymId) = gymDao.setActive(gymId.value)

    override suspend fun deleteGym(gymId: GymId) = gymDao.deleteGym(gymId.value)

    override fun observeEquipment(gymId: GymId): Flow<List<Equipment>> =
        equipmentDao.observeEquipment(gymId.value).map { rows -> rows.map { it.toModel() } }

    override suspend fun saveEquipment(equipment: List<Equipment>) =
        equipmentDao.upsertEquipment(equipment.map { it.toEntity() })

    /**
     * Consulta los usos antes de borrar: con alguno, el `DELETE` fallaría por clave foránea y
     * la excepción de SQLite no debe salir de la capa de datos.
     */
    override suspend fun deleteEquipment(equipmentId: EquipmentId): EquipmentDeletion {
        val usages = equipmentDao.countEquipmentUsages(equipmentId.value)
        if (usages > 0) return EquipmentDeletion.InUse(usages)
        equipmentDao.deleteEquipment(equipmentId.value)
        return EquipmentDeletion.Deleted
    }
}
