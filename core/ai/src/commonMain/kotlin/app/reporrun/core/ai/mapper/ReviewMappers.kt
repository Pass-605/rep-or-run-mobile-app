package app.reporrun.core.ai.mapper

import app.reporrun.core.ai.contract.ProposalDto
import app.reporrun.core.ai.contract.ReviewPlanResponse
import app.reporrun.core.domain.port.GeneratedReview
import app.reporrun.core.model.Confidence
import app.reporrun.core.model.DataQuality
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.EquipmentId
import app.reporrun.core.model.ExerciseCode
import app.reporrun.core.model.ProposalType
import app.reporrun.core.model.ProposedChange
import app.reporrun.core.model.ReviewResult
import kotlin.time.Instant

/**
 * Respuesta de revisión → dominio, con todos los errores juntos. Solo mira cada propuesta por
 * separado; que apunte a un ejercicio del plan o a un aparato del inventario es de
 * `ProposalValidator`.
 */
internal fun ReviewPlanResponse.toGeneratedReview(): Mapped<GeneratedReview> {
    val errors = ModelErrors()
    val generated = errors.build("generatedAt") { Instant.parse(generatedAt) }
    val proposals = review.proposals.mapIndexed { index, proposal ->
        errors.build("review.proposals[$index]") { proposal.toProposedChange() }
    }
    // Con una propuesta rota no se construye el resultado: saldría sin ella y parecería válido.
    val result = if (errors.size > 0) {
        null
    } else {
        errors.build("review") {
            ReviewResult(
                dataQuality = DataQuality.valueOf(review.dataQuality.name),
                summary = review.summary,
                observations = review.observations,
                proposals = proposals.filterNotNull(),
                questions = review.questions,
            )
        }
    }

    return if (result == null || generated == null) {
        Mapped.Invalid(errors.errors)
    } else {
        Mapped.Valid(GeneratedReview(result, model, promptVersion, schemaVersion, generated))
    }
}

private fun ProposalDto.toProposedChange() = ProposedChange(
    type = ProposalType.valueOf(type.name),
    targetExerciseCode = targetExerciseCode?.let(::ExerciseCode),
    targetDayOfWeek = targetDayOfWeek?.let { DayOfWeek.valueOf(it.name) },
    replacementEquipmentId = replacementEquipmentId?.let(::EquipmentId),
    description = description,
    rationale = rationale,
    confidence = Confidence.valueOf(confidence.name),
)
