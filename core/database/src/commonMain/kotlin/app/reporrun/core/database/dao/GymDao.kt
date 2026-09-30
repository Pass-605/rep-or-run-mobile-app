package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.reporrun.core.database.entity.GymEntity
import kotlinx.coroutines.flow.Flow

/** Gimnasios. El inventario y el catálogo están en [EquipmentDao]. */
@Dao
interface GymDao {
    @Upsert
    suspend fun upsertGym(gym: GymEntity)

    @Query("SELECT * FROM gym ORDER BY createdAt")
    fun observeGyms(): Flow<List<GymEntity>>

    @Query("SELECT * FROM gym WHERE isActive = 1 LIMIT 1")
    fun observeActiveGym(): Flow<GymEntity?>

    /** Deja [gymId] como único gimnasio activo. */
    @Transaction
    suspend fun setActive(gymId: String) {
        clearActive()
        markActive(gymId)
    }

    @Query("UPDATE gym SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActive()

    @Query("UPDATE gym SET isActive = 1 WHERE id = :gymId")
    suspend fun markActive(gymId: String)

    /** Borra el gimnasio y, en cascada, su inventario, sus planes y todo lo que cuelga de ellos. */
    @Query("DELETE FROM gym WHERE id = :gymId")
    suspend fun deleteGym(gymId: String)
}
