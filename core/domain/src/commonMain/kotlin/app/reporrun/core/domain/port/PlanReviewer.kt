package app.reporrun.core.domain.port

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.CheckInRecord
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.Gym
import app.reporrun.core.model.Plan
import app.reporrun.core.model.ReviewResult
import app.reporrun.core.model.UserProfile
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Revisa un plan con lo que la persona contó en el check-in. Devuelve **propuestas**, nunca
 * cambios: nada toca el plan hasta que la persona aprueba (plan §4.12).
 *
 * Lo que devuelve no es de fiar hasta pasar por `ProposalValidator`.
 */
fun interface PlanReviewer {
    /**
     * Pide una revisión para [request]. Suspende lo que tarde el servicio y es cancelable: si
     * se cancela, no devuelve nada. Cualquier otro fallo es un [PlanReviewResult.Failure].
     */
    suspend fun review(request: PlanReviewRequest): PlanReviewResult
}

/**
 * Lo que se envía para revisar.
 *
 * @property plan el plan vigente, entero.
 * @property equipment inventario del gimnasio del plan: un cambio de ejercicio solo puede
 *   proponer aparatos de aquí (plan §4.1).
 * @property checkIn el check-in con lo que la persona contó de cada ejercicio.
 * @property daysElapsed días desde que empezó el bloque.
 * @property previousReviews revisiones anteriores de este plan, de la más antigua a la más reciente.
 */
data class PlanReviewRequest(
    val plan: Plan,
    val gym: Gym,
    val equipment: List<Equipment>,
    val profile: UserProfile,
    val preferences: BlockPreferences,
    val checkIn: CheckInRecord,
    val daysElapsed: Int,
    val previousReviews: List<PreviousReview>,
)

/**
 * Una revisión anterior, resumida para dar contexto a la siguiente.
 *
 * @property appliedChanges los cambios que la persona aprobó, en texto.
 */
data class PreviousReview(val date: LocalDate, val summary: String, val appliedChanges: List<String>)

/**
 * Una revisión recién generada.
 *
 * @property result lo que se le enseña a la persona antes de que decida.
 * @property model modelo que la generó, tal como lo dijo el servicio.
 */
data class GeneratedReview(
    val result: ReviewResult,
    val model: String,
    val promptVersion: String,
    val schemaVersion: String,
    val generatedAt: Instant,
)

/** Resultado de [PlanReviewer.review]. */
sealed interface PlanReviewResult {
    /** Hay revisión. Falta validar sus propuestas. */
    data class Success(val review: GeneratedReview) : PlanReviewResult

    /** No hay revisión, y [error] dice por qué. */
    data class Failure(val error: AiError) : PlanReviewResult
}
