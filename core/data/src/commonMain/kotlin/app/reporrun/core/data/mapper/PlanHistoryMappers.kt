package app.reporrun.core.data.mapper

import app.reporrun.core.database.entity.ExportRecordEntity
import app.reporrun.core.database.entity.PlanChangeEntity
import app.reporrun.core.database.entity.PlanEntity
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.ExportRecordId
import app.reporrun.core.model.GymId
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanChangeId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanSummary
import app.reporrun.core.model.ReviewId

/* Historial de versiones ↔ filas: resúmenes de plan, cambios de versión y PDFs exportados. */

internal fun PlanEntity.toSummary() = PlanSummary(
    id = PlanId(id),
    gymId = GymId(gymId),
    createdAt = createdAt,
    status = status,
    startDate = startDate,
    reviewDate = reviewDate,
    focus = focus,
)

internal fun PlanChange.toEntity() = PlanChangeEntity(
    id = id.value,
    planId = planId.value,
    previousPlanId = previousPlanId?.value,
    reviewId = reviewId?.value,
    date = date,
    summary = summary,
    reason = reason,
    approvedAt = approvedAt,
)

internal fun PlanChangeEntity.toModel() = PlanChange(
    id = PlanChangeId(id),
    planId = PlanId(planId),
    previousPlanId = previousPlanId?.let(::PlanId),
    reviewId = reviewId?.let(::ReviewId),
    date = date,
    summary = summary,
    reason = reason,
    approvedAt = approvedAt,
)

internal fun ExportRecord.toEntity() = ExportRecordEntity(
    id = id.value,
    planId = planId.value,
    createdAt = createdAt,
    fileName = fileName,
    pages = pages,
)

internal fun ExportRecordEntity.toModel() = ExportRecord(
    id = ExportRecordId(id),
    planId = PlanId(planId),
    createdAt = createdAt,
    fileName = fileName,
    pages = pages,
)
