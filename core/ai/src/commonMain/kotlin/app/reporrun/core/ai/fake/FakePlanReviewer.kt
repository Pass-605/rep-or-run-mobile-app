package app.reporrun.core.ai.fake

import app.reporrun.core.ai.contract.ProposalDto
import app.reporrun.core.ai.contract.ProposalTypeDto
import app.reporrun.core.ai.mapper.Mapped
import app.reporrun.core.ai.mapper.toGeneratedReview
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.PlanReviewRequest
import app.reporrun.core.domain.port.PlanReviewResult
import app.reporrun.core.domain.port.PlanReviewer
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * [PlanReviewer] sin red ni cuentas: devuelve una de las dos revisiones de ejemplo del contrato
 * (`review-response.*.json`, sintéticas) según los datos del check-in.
 *
 * - Sin ningún ejercicio con datos comparables (`ExerciseFeedback.isComparable`), la de datos
 *   insuficientes, que no propone nada (plan §4.13).
 * - Con alguno, la de datos suficientes, sin las propuestas que no encajan en el plan que se
 *   revisa: las que apuntan a un ejercicio o a un día que no tiene, las que proponen un aparato
 *   fuera del inventario y las de subir carga sobre un ejercicio sin datos comparables.
 *
 * Solo devuelve propuestas; nada de aquí cambia el plan (plan §4.12). El resumen y las
 * observaciones son los del ejemplo, texto de demostración: **no describen el check-in real**.
 *
 * @param config la espera y el fallo simulados.
 * @param clock de aquí sale `generatedAt`.
 */
class FakePlanReviewer(
    private val config: FakeAiConfig,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : PlanReviewer {
    override suspend fun review(request: PlanReviewRequest): PlanReviewResult {
        delay(config.latency)
        return config.failure?.let(PlanReviewResult::Failure)
            ?: withContext(dispatchers.default) { exampleReviewFor(request) }
    }

    private fun exampleReviewFor(request: PlanReviewRequest): PlanReviewResult {
        val target = ReviewTarget(request)
        val example = if (target.comparableCodes.isEmpty()) {
            FakeExamples.reviewInsufficient
        } else {
            FakeExamples.reviewSufficient
        }
        val response = example.copy(
            model = FAKE_MODEL,
            generatedAt = clock.now().toString(),
            review = example.review.copy(proposals = example.review.proposals.filter(target::admits)),
        )

        return when (val mapped = response.toGeneratedReview()) {
            is Mapped.Valid -> PlanReviewResult.Success(mapped.value)
            is Mapped.Invalid -> PlanReviewResult.Failure(AiError.InvalidResponse(mapped.errors))
        }
    }
}

/** Lo que una propuesta puede nombrar en la revisión de [request]. */
private class ReviewTarget(request: PlanReviewRequest) {
    private val exercises = request.plan.days.flatMap { it.exercises }
    private val codes = exercises.map { it.code.value }.toSet()
    private val days = request.plan.days.map { it.dayOfWeek.name }.toSet()
    private val equipmentIds = request.equipment.map { it.id.value }.toSet()

    /** Códigos de los ejercicios del plan con datos suficientes para comparar su carga. */
    val comparableCodes: Set<String> = request.checkIn.feedback
        .filter { it.isComparable }
        .mapNotNull { feedback -> exercises.firstOrNull { it.id == feedback.planExerciseId }?.code?.value }
        .toSet()

    fun admits(proposal: ProposalDto): Boolean =
        (proposal.targetExerciseCode == null || proposal.targetExerciseCode in codes) &&
            (proposal.targetDayOfWeek == null || proposal.targetDayOfWeek.name in days) &&
            (proposal.replacementEquipmentId == null || proposal.replacementEquipmentId in equipmentIds) &&
            (proposal.type != ProposalTypeDto.INCREASE_LOAD || proposal.targetExerciseCode in comparableCodes)
}
