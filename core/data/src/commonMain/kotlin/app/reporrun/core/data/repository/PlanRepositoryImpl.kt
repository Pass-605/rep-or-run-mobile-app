package app.reporrun.core.data.repository

import app.reporrun.core.data.mapper.toEntity
import app.reporrun.core.data.mapper.toModel
import app.reporrun.core.data.mapper.toPlan
import app.reporrun.core.data.mapper.toRows
import app.reporrun.core.data.mapper.toSummary
import app.reporrun.core.database.dao.PlanDao
import app.reporrun.core.database.dao.PlanHistoryDao
import app.reporrun.core.database.entity.PlanWithDays
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.domain.validation.Mapped
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.GymId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanRepositoryImpl(private val planDao: PlanDao, private val historyDao: PlanHistoryDao) : PlanRepository {
    override fun observePlan(planId: PlanId): Flow<Plan?> =
        planDao.observePlan(planId.value).map { it?.toPlanOrThrow() }

    override fun observeActivePlan(gymId: GymId): Flow<Plan?> =
        planDao.observeActivePlan(gymId.value).map { it?.toPlanOrThrow() }

    override fun observeHistory(gymId: GymId): Flow<List<PlanSummary>> =
        historyDao.observeHistory(gymId.value).map { rows -> rows.map { it.toSummary() } }

    override suspend fun savePlan(plan: Plan) {
        val rows = plan.toRows()
        planDao.insertPlan(rows.plan, rows.days, rows.exercises, rows.alternatives)
    }

    override suspend fun activatePlan(planId: PlanId) = planDao.activate(planId.value)

    override suspend fun setDayStatus(dayId: PlanDayId, status: DayStatus) =
        planDao.updateDayStatus(dayId.value, status)

    override fun observeChanges(gymId: GymId): Flow<List<PlanChange>> =
        historyDao.observeChanges(gymId.value).map { rows -> rows.map { it.toModel() } }

    override suspend fun saveChange(change: PlanChange) = historyDao.insertChange(change.toEntity())

    override fun observeExports(planId: PlanId): Flow<List<ExportRecord>> =
        historyDao.observeExports(planId.value).map { rows -> rows.map { it.toModel() } }

    override suspend fun saveExport(record: ExportRecord) = historyDao.insertExport(record.toEntity())

    /**
     * Un plan guardado salió de un [Plan] válido: si ya no se puede leer, la base está dañada
     * o hay un fallo en el mapper, y no es algo que la persona pueda arreglar. Se lanza con
     * todos los errores (rutas y reglas, sin contenido del plan) para que el fallo se vea.
     */
    private fun PlanWithDays.toPlanOrThrow(): Plan = when (val mapped = toPlan()) {
        is Mapped.Valid -> mapped.value
        is Mapped.Invalid -> error("Plan ${plan.id} ilegible: ${mapped.errors.joinToString("; ")}")
    }
}
