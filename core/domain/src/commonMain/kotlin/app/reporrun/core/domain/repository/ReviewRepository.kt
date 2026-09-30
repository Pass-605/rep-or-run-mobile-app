package app.reporrun.core.domain.repository

import app.reporrun.core.model.ChangeProposal
import app.reporrun.core.model.ChangeProposalId
import app.reporrun.core.model.CheckInRecord
import app.reporrun.core.model.Decision
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.Review
import app.reporrun.core.model.ReviewId
import app.reporrun.core.model.ReviewSchedule
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Calendario de revisión, revisiones, check-ins y propuestas. Una revisión produce propuestas,
 * nunca cambios: aprobar una no toca el plan (plan §4.12).
 */
interface ReviewRepository {
    /** Calendario de revisión de [planId], o `null` si no tiene. */
    fun observeSchedule(planId: PlanId): Flow<ReviewSchedule?>

    /** Crea o sustituye el calendario. */
    suspend fun saveSchedule(schedule: ReviewSchedule)

    /** Revisiones de [planId], la más reciente primero. */
    fun observeReviews(planId: PlanId): Flow<List<Review>>

    /** Crea o sustituye una revisión. */
    suspend fun saveReview(review: Review)

    /** El check-in de [reviewId] con su feedback, o `null` si no se ha hecho. */
    fun observeCheckIn(reviewId: ReviewId): Flow<CheckInRecord?>

    /** Guarda el check-in con su feedback, o nada. Reescribirlo sustituye el feedback anterior. */
    suspend fun saveCheckIn(record: CheckInRecord)

    /** Propuestas de [reviewId]. */
    fun observeProposals(reviewId: ReviewId): Flow<List<ChangeProposal>>

    /** Guarda propuestas nuevas, pendientes de decisión. */
    suspend fun saveProposals(proposals: List<ChangeProposal>)

    /**
     * Registra la decisión de la persona. [decidedAt] va si y solo si [decision] no es
     * [Decision.PENDING], igual que en [ChangeProposal].
     */
    suspend fun decide(proposalId: ChangeProposalId, decision: Decision, decidedAt: Instant?)
}
