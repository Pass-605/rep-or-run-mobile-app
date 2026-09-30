package app.reporrun.core.data.repository

import app.reporrun.core.data.mapper.toEntity
import app.reporrun.core.data.mapper.toModel
import app.reporrun.core.database.dao.CheckInDao
import app.reporrun.core.database.dao.ReviewDao
import app.reporrun.core.domain.repository.ReviewRepository
import app.reporrun.core.model.ChangeProposal
import app.reporrun.core.model.ChangeProposalId
import app.reporrun.core.model.CheckInRecord
import app.reporrun.core.model.Decision
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.Review
import app.reporrun.core.model.ReviewId
import app.reporrun.core.model.ReviewSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class ReviewRepositoryImpl(private val reviewDao: ReviewDao, private val checkInDao: CheckInDao) : ReviewRepository {
    override fun observeSchedule(planId: PlanId): Flow<ReviewSchedule?> =
        reviewDao.observeSchedule(planId.value).map { it?.toModel() }

    override suspend fun saveSchedule(schedule: ReviewSchedule) = reviewDao.upsertSchedule(schedule.toEntity())

    override fun observeReviews(planId: PlanId): Flow<List<Review>> =
        reviewDao.observeReviews(planId.value).map { rows -> rows.map { it.toModel() } }

    override suspend fun saveReview(review: Review) = reviewDao.upsertReview(review.toEntity())

    override fun observeCheckIn(reviewId: ReviewId): Flow<CheckInRecord?> =
        checkInDao.observeCheckIn(reviewId.value).map { it?.toModel() }

    override suspend fun saveCheckIn(record: CheckInRecord) =
        checkInDao.saveCheckIn(record.checkIn.toEntity(), record.feedback.map { it.toEntity() })

    override fun observeProposals(reviewId: ReviewId): Flow<List<ChangeProposal>> =
        reviewDao.observeProposals(reviewId.value).map { rows -> rows.map { it.toModel() } }

    override suspend fun saveProposals(proposals: List<ChangeProposal>) =
        reviewDao.insertProposals(proposals.map { it.toEntity() })

    override suspend fun decide(proposalId: ChangeProposalId, decision: Decision, decidedAt: Instant?) {
        require((decision == Decision.PENDING) == (decidedAt == null)) { "decidedAt solo con una decisión tomada" }
        reviewDao.decide(proposalId.value, decision, decidedAt)
    }
}
