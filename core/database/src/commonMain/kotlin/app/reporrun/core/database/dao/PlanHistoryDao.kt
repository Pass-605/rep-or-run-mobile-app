package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.reporrun.core.database.entity.ExportRecordEntity
import app.reporrun.core.database.entity.PlanChangeEntity
import app.reporrun.core.database.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

/** Historial: versiones de plan de un gimnasio, cambios de versión aprobados y PDFs exportados. */
@Dao
interface PlanHistoryDao {
    /** Todas las versiones del gimnasio, de la más reciente a la más antigua, sin sus días. */
    @Query("SELECT * FROM plan WHERE gymId = :gymId ORDER BY createdAt DESC, id DESC")
    fun observeHistory(gymId: String): Flow<List<PlanEntity>>

    @Insert
    suspend fun insertChange(change: PlanChangeEntity)

    /** Historial de cambios de versión del gimnasio, el más reciente primero. */
    @Query(
        """
        SELECT plan_change.* FROM plan_change
        INNER JOIN plan ON plan.id = plan_change.planId
        WHERE plan.gymId = :gymId
        ORDER BY plan_change.approvedAt DESC
        """,
    )
    fun observeChanges(gymId: String): Flow<List<PlanChangeEntity>>

    @Insert
    suspend fun insertExport(record: ExportRecordEntity)

    @Query("SELECT * FROM export_record WHERE planId = :planId ORDER BY createdAt DESC")
    fun observeExports(planId: String): Flow<List<ExportRecordEntity>>
}
