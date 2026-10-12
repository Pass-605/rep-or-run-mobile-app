package app.reporrun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import app.reporrun.core.database.entity.PlanDayEntity
import app.reporrun.core.database.entity.PlanEntity
import app.reporrun.core.database.entity.PlanExerciseAlternativeEntity
import app.reporrun.core.database.entity.PlanExerciseEntity
import app.reporrun.core.database.entity.PlanWithDays
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.flow.Flow

/**
 * Planes: el árbol de cada versión y su estado. El historial está en [PlanHistoryDao].
 *
 * Un plan se inserta entero ([insertPlan]) y nunca se reescribe: los `@Insert` fallan si el id
 * ya existe. Lo único que cambia después es el estado del plan y de sus días.
 */
@Dao
interface PlanDao {
    /** Guarda un plan con todos sus niveles en una transacción, o nada si alguna fila falla. */
    @Insert
    suspend fun insertPlan(
        plan: PlanEntity,
        days: List<PlanDayEntity>,
        exercises: List<PlanExerciseEntity>,
        alternatives: List<PlanExerciseAlternativeEntity>,
    )

    @Transaction
    @Query("SELECT * FROM plan WHERE id = :planId")
    fun observePlan(planId: String): Flow<PlanWithDays?>

    /**
     * Los ids que empiezan por [prefix], de cualquier gimnasio y en cualquier estado. Con
     * `ROR-AAAA-MM-DD-v`, los planes de ese día. `substr` y no `LIKE`: compara tal cual, sin
     * comodines ni mayúsculas equivalentes.
     */
    @Query("SELECT id FROM plan WHERE substr(id, 1, length(:prefix)) = :prefix")
    suspend fun idsStartingWith(prefix: String): List<String>

    /** El plan activo del gimnasio, entero; `null` si no hay ninguno. */
    @Transaction
    @Query("SELECT * FROM plan WHERE gymId = :gymId AND status = 'ACTIVE' LIMIT 1")
    fun observeActivePlan(gymId: String): Flow<PlanWithDays?>

    /** Activa [planId] y archiva el plan que estuviera activo en su gimnasio. */
    @Transaction
    suspend fun activate(planId: String) {
        archiveActiveOfGymOf(planId)
        updateStatus(planId, PlanStatus.ACTIVE)
    }

    @Query(
        """
        UPDATE plan SET status = 'ARCHIVED'
        WHERE status = 'ACTIVE' AND id != :planId AND gymId = (SELECT gymId FROM plan WHERE id = :planId)
        """,
    )
    suspend fun archiveActiveOfGymOf(planId: String)

    @Query("UPDATE plan SET status = :status WHERE id = :planId")
    suspend fun updateStatus(planId: String, status: PlanStatus)

    @Query("UPDATE plan_day SET status = :status WHERE id = :dayId")
    suspend fun updateDayStatus(dayId: String, status: DayStatus)

    /**
     * Borra el plan y en cascada sus días, ejercicios, alternativas, revisiones y
     * exportaciones. Falla si otra versión lo tiene como anterior.
     */
    @Query("DELETE FROM plan WHERE id = :planId")
    suspend fun deletePlan(planId: String)
}
