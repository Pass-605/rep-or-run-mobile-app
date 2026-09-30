package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.reporrun.core.database.entity.CheckInEntity
import app.reporrun.core.database.entity.CheckInWithFeedback
import app.reporrun.core.database.entity.ExerciseFeedbackEntity
import kotlinx.coroutines.flow.Flow

/** Check-in de una revisión y el feedback de cada ejercicio. */
@Dao
interface CheckInDao {
    /** Guarda el check-in con su feedback, o nada. Reescribirlo sustituye el feedback anterior. */
    @Transaction
    suspend fun saveCheckIn(checkIn: CheckInEntity, feedback: List<ExerciseFeedbackEntity>) {
        upsertCheckIn(checkIn)
        deleteFeedback(checkIn.id)
        insertFeedback(feedback)
    }

    @Upsert
    suspend fun upsertCheckIn(checkIn: CheckInEntity)

    @Query("DELETE FROM exercise_feedback WHERE checkInId = :checkInId")
    suspend fun deleteFeedback(checkInId: String)

    @Insert
    suspend fun insertFeedback(feedback: List<ExerciseFeedbackEntity>)

    @Transaction
    @Query("SELECT * FROM check_in WHERE reviewId = :reviewId LIMIT 1")
    fun observeCheckIn(reviewId: String): Flow<CheckInWithFeedback?>
}
