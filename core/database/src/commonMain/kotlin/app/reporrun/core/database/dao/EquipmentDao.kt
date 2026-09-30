package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import app.reporrun.core.database.entity.EquipmentCatalogEntity
import app.reporrun.core.database.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

/** Inventario de cada gimnasio y catálogo semilla. */
@Dao
interface EquipmentDao {
    @Upsert
    suspend fun upsertEquipment(equipment: List<EquipmentEntity>)

    /** Inventario de un gimnasio. */
    @Query("SELECT * FROM equipment WHERE gymId = :gymId ORDER BY name")
    fun observeEquipment(gymId: String): Flow<List<EquipmentEntity>>

    /**
     * Cuántos ejercicios y alternativas de cualquier plan, archivados incluidos, usan el
     * aparato. Con más de cero, [deleteEquipment] falla por clave foránea.
     */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM plan_exercise WHERE equipmentId = :equipmentId)
             + (SELECT COUNT(*) FROM plan_exercise_alternative WHERE equipmentId = :equipmentId)
             + (SELECT COUNT(*) FROM change_proposal WHERE replacementEquipmentId = :equipmentId)
        """,
    )
    suspend fun countEquipmentUsages(equipmentId: String): Int

    @Query("DELETE FROM equipment WHERE id = :equipmentId")
    suspend fun deleteEquipment(equipmentId: String)

    /** Carga idempotente de la semilla: una clave que ya existe no se toca. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCatalog(items: List<EquipmentCatalogEntity>)

    @Query("SELECT * FROM equipment_catalog ORDER BY name")
    fun observeCatalog(): Flow<List<EquipmentCatalogEntity>>

    @Query("SELECT COUNT(*) FROM equipment_catalog")
    suspend fun countCatalog(): Int
}
