package app.reporrun.core.ai.fake

import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.ai.contract.GeneratePlanRequest
import app.reporrun.core.ai.mapper.toDto
import app.reporrun.core.ai.mapper.toGeneratedPlan
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.domain.validation.Mapped
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * [PlanGenerator] sin red ni cuentas: devuelve el plan de ejemplo del contrato
 * (`generate-response.small-gym.json`, sintético) adaptado al inventario, los días, el cardio
 * y los minutos por sesión de la petición. En el MVP es la única implementación; el adaptador del proxy llega con A-13.
 *
 * El plan pasa por el mismo camino que pasará una respuesta real: DTO del contrato → dominio,
 * con sus comprobaciones. Sigue sin ser de fiar hasta `PlanValidator`.
 *
 * @param config la espera y el fallo simulados.
 * @param ids identificadores de los días y ejercicios del plan.
 * @param clock de aquí sale `generatedAt`.
 */
class FakePlanGenerator(
    private val config: FakeAiConfig,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : PlanGenerator {
    override suspend fun generate(request: PlanGenerationRequest): PlanGenerationResult {
        delay(config.latency)
        return config.failure?.let(PlanGenerationResult::Failure)
            ?: withContext(dispatchers.default) { examplePlanFor(request) }
    }

    private fun examplePlanFor(request: PlanGenerationRequest): PlanGenerationResult {
        val example = FakeExamples.generateResponse
        val plan = example.plan.adaptedTo(request, FakeExamples.generateRequest.gym.equipment)
        val response = example.copy(model = FAKE_MODEL, generatedAt = clock.now().toString(), plan = plan)
        val snapshot = ContractJson.encodeToString(
            GeneratePlanRequest.serializer(),
            request.toDto(promptVersion = example.promptVersion, locale = FAKE_LOCALE),
        )

        return if (plan.days.isEmpty()) {
            PlanGenerationResult.Failure(AiError.InvalidInput(listOf(NO_STRENGTH_EQUIPMENT)))
        } else {
            when (val mapped = response.toGeneratedPlan(ids, snapshot)) {
                is Mapped.Valid -> PlanGenerationResult.Success(
                    mapped.value.copy(
                        days = mapped.value.days.map { it.fittedTo(request.preferences.sessionMinutes) },
                    ),
                )

                is Mapped.Invalid -> PlanGenerationResult.Failure(AiError.InvalidResponse(mapped.errors))
            }
        }
    }

    private companion object {
        const val NO_STRENGTH_EQUIPMENT =
            "gym.equipment: ningún aparato del inventario sirve para un ejercicio de fuerza"
    }
}
