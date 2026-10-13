package app.reporrun.core.database.dao

import androidx.room.Dao
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

    /**
     * Carga idempotente de la semilla: inserta las claves nuevas y deja las que ya estaban con
     * el texto de [items]. La clave no cambia nunca; el nombre y los alias, sí.
     */
    @Upsert
    suspend fun upsertCatalog(items: List<EquipmentCatalogEntity>)

    /**
     * Pone a los aparatos marcados desde el catálogo el nombre que tiene hoy su entrada. Los
     * personalizados y los de una clave que ya no está en el catálogo no se tocan.
     */
    @Query(
        """
        UPDATE equipment
           SET name = (SELECT c.name FROM equipment_catalog c WHERE c.`key` = equipment.catalogKey)
         WHERE isCustom = 0
           AND EXISTS (
               SELECT 1 FROM equipment_catalog c
                WHERE c.`key` = equipment.catalogKey AND c.name <> equipment.name
           )
        """,
    )
    suspend fun syncCatalogNames()

    @Query("SELECT * FROM equipment_catalog ORDER BY name")
    fun observeCatalog(): Flow<List<EquipmentCatalogEntity>>

    @Query("SELECT COUNT(*) FROM equipment_catalog")
    suspend fun countCatalog(): Int
}
