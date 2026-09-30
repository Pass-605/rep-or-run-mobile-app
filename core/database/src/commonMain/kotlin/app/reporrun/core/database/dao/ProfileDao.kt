package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.reporrun.core.database.entity.BlockPreferencesEntity
import app.reporrun.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/** Perfil y preferencias del bloque: una fila de cada por instalación. */
@Dao
interface ProfileDao {
    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Upsert
    suspend fun upsertPreferences(preferences: BlockPreferencesEntity)

    @Query("SELECT * FROM block_preferences LIMIT 1")
    fun observePreferences(): Flow<BlockPreferencesEntity?>
}
