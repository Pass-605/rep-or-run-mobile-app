package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import app.reporrun.core.database.entity.ChangeProposalEntity
import app.reporrun.core.database.entity.ReviewEntity
import app.reporrun.core.database.entity.ReviewScheduleEntity
import app.reporrun.core.model.Decision
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** Calendario de revisión, revisiones y propuestas. El check-in está en [CheckInDao]. */
@Dao
interface ReviewDao {
    @Upsert
    suspend fun upsertSchedule(schedule: ReviewScheduleEntity)

    @Query("SELECT * FROM review_schedule WHERE planId = :planId LIMIT 1")
    fun observeSchedule(planId: String): Flow<ReviewScheduleEntity?>

    @Upsert
    suspend fun upsertReview(review: ReviewEntity)

    @Query("SELECT * FROM review WHERE planId = :planId ORDER BY dueAt DESC")
    fun observeReviews(planId: String): Flow<List<ReviewEntity>>

    @Insert
    suspend fun insertProposals(proposals: List<ChangeProposalEntity>)

    @Query("SELECT * FROM change_proposal WHERE reviewId = :reviewId ORDER BY id")
    fun observeProposals(reviewId: String): Flow<List<ChangeProposalEntity>>

    /** Registra la decisión de la persona. Aprobar no cambia el plan (plan §4.12). */
    @Query("UPDATE change_proposal SET decision = :decision, decidedAt = :decidedAt WHERE id = :proposalId")
    suspend fun decide(proposalId: String, decision: Decision, decidedAt: Instant?)
}
