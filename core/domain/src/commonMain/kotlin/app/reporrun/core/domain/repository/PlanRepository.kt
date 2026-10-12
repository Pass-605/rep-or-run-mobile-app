package app.reporrun.core.domain.repository

import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.GymId
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanDayId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Versiones de plan y su historial. Un plan no se edita en el sitio (plan §4.12): una versión
 * nueva es un [Plan] nuevo enlazado con la anterior por un [PlanChange]. Lo único que cambia
 * de un plan guardado es su estado y el de sus días.
 */
interface PlanRepository {
    /** El plan entero, o `null` si no existe. */
    fun observePlan(planId: PlanId): Flow<Plan?>

    /** El plan activo de [gymId], entero, o `null` si no hay ninguno. */
    fun observeActivePlan(gymId: GymId): Flow<Plan?>

    /** Versiones de [gymId] sin sus días, de la más reciente a la más antigua. */
    fun observeHistory(gymId: GymId): Flow<List<PlanSummary>>

    /**
     * Los ids de los planes generados el día [date], en cualquier gimnasio y en cualquier
     * estado: el id es único en toda la app, no por gimnasio. De aquí sale el `N` de la versión
     * siguiente.
     */
    suspend fun planIdsOn(date: LocalDate): List<PlanId>

    /**
     * Guarda un plan nuevo con todos sus días, ejercicios y alternativas, o nada. Tiene que
     * haber pasado `PlanValidator`: aquí no se comprueba el cruce con el inventario.
     */
    suspend fun savePlan(plan: Plan)

    /** Activa [planId] y archiva el plan que estuviera activo en su gimnasio. */
    suspend fun activatePlan(planId: PlanId)

    /** Marca un día como propuesto o confirmado (plan §4.11). */
    suspend fun setDayStatus(dayId: PlanDayId, status: DayStatus)

    /** Cambios de versión aprobados de [gymId], el más reciente primero. */
    fun observeChanges(gymId: GymId): Flow<List<PlanChange>>

    /** Registra un cambio de versión aprobado. */
    suspend fun saveChange(change: PlanChange)

    /** PDFs exportados de [planId], el más reciente primero. */
    fun observeExports(planId: PlanId): Flow<List<ExportRecord>>

    /** Registra un PDF exportado. */
    suspend fun saveExport(record: ExportRecord)
}
