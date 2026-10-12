package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.repository.PlanRepository
import app.reporrun.core.domain.validation.PlanValidation
import app.reporrun.core.domain.validation.PlanValidator
import app.reporrun.core.model.Plan
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Por qué no se pudo hablar con el servicio que genera. Todos invitan a que la persona lo
 * intente otra vez con los mismos datos.
 */
sealed interface GenerationUnavailable {
    /** Sin conexión. */
    data object Network : GenerationUnavailable

    /** El servicio no contestó a tiempo. */
    data object Timeout : GenerationUnavailable

    /**
     * Demasiadas peticiones.
     *
     * @property retryAfterSeconds cuánto esperar antes de reintentar, si el servicio lo dijo.
     */
    data class RateLimited(val retryAfterSeconds: Int?) : GenerationUnavailable

    /** El servicio falló por su lado. */
    data object Upstream : GenerationUnavailable
}

/**
 * Resultado de [GeneratePlanUseCase]. Solo [Saved] deja algo guardado.
 *
 * Lo que la persona puede reintentar tal cual ([Unavailable]) y lo que no ([InvalidPlan],
 * [InvalidInput], [Blocked]) son tipos distintos a propósito: quien pinta el error no tiene
 * que deducirlo.
 */
sealed interface GeneratePlanResult {
    /** Hay plan, guardado como [PlanStatus.DRAFT]. Lo activa la persona, no esto. */
    data class Saved(val plan: Plan) : GeneratePlanResult

    /**
     * Falta algo para generar y no se ha enviado nada.
     *
     * @property blockers todo lo que falta, igual que en el resumen previo.
     */
    data class Blocked(val blockers: List<GenerationBlocker>) : GeneratePlanResult

    /** No se pudo generar ahora. **Reintentable** por la persona. */
    data class Unavailable(val reason: GenerationUnavailable) : GeneratePlanResult

    /**
     * No hay plan válido: el servicio no consiguió uno (su `422 PLAN_INVALID`, después de
     * reparar) o el que llegó no pasa `PlanValidator`. **Definitivo**: no se repara ni se
     * reintenta desde aquí, y no se guarda nada.
     *
     * @property problems todos los problemas, con ruta y regla; sin nombres ni notas.
     */
    data class InvalidPlan(val problems: List<String>) : GeneratePlanResult

    /**
     * El servicio no acepta lo enviado. Repetirlo igual no sirve: hay que cambiar datos.
     *
     * @property problems qué falla y dónde, sin datos de la persona.
     */
    data class InvalidInput(val problems: List<String>) : GeneratePlanResult
}

/**
 * Genera el plan del gimnasio activo y lo guarda como borrador.
 *
 * 1. Lee el resumen previo y, si tiene bloqueos (consentimiento incluido), no envía nada.
 * 2. Pide el plan a [PlanGenerator], **una vez**: la reparación es cosa del servicio.
 * 3. Lo pasa por [PlanValidator], la segunda red. Un plan inválido no se guarda.
 * 4. Lo guarda como [PlanStatus.DRAFT] con el id `ROR-AAAA-MM-DD-vN`.
 *
 * Es cancelable: si se cancela antes de guardar, no queda nada; el guardado es todo o nada.
 *
 * @param clock de aquí salen `createdAt` y el día del id.
 * @param timeZone la zona de la persona: el día del id es el suyo, no el de UTC.
 */
class GeneratePlanUseCase(
    private val buildSummary: BuildGenerationSummaryUseCase,
    private val generator: PlanGenerator,
    private val validator: PlanValidator,
    private val planRepository: PlanRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    suspend operator fun invoke(): GeneratePlanResult {
        val summary = buildSummary().first()
        val request = summary.toRequest() ?: return GeneratePlanResult.Blocked(summary.blockers)

        return when (val generation = generator.generate(request)) {
            is PlanGenerationResult.Failure -> generation.error.toResult()
            is PlanGenerationResult.Success -> validateAndSave(generation.plan, request)
        }
    }

    private suspend fun validateAndSave(generated: GeneratedPlan, request: PlanGenerationRequest): GeneratePlanResult =
        when (val validation = validator.validate(generated.days, request.equipment, request.preferences)) {
            is PlanValidation.Invalid -> GeneratePlanResult.InvalidPlan(validation.messages)

            PlanValidation.Valid -> {
                // Un generador que no coopera puede devolver un plan con la corrutina ya
                // cancelada: nadie lo espera, así que no se guarda.
                currentCoroutineContext().ensureActive()
                val plan = generated.toDraft(request, createdAt = clock.now())
                planRepository.savePlan(plan)
                GeneratePlanResult.Saved(plan)
            }
        }

    private suspend fun GeneratedPlan.toDraft(request: PlanGenerationRequest, createdAt: Instant) = Plan(
        id = nextPlanId(createdAt),
        gymId = request.gym.id,
        createdAt = createdAt,
        status = PlanStatus.DRAFT,
        blockDays = blockDays,
        startDate = request.preferences.startDate,
        // La pone la activación, con el calendario de revisiones.
        reviewDate = null,
        focus = focus,
        notes = notes,
        days = days,
        inputSnapshotJson = inputSnapshotJson,
        model = model,
        promptVersion = promptVersion,
        schemaVersion = schemaVersion,
    )

    /**
     * `N` es la mayor versión guardada con la fecha de hoy más uno, y no cuántas hay: si se
     * borró un plan intermedio, contar repetiría un id que existe.
     */
    private suspend fun nextPlanId(now: Instant): PlanId {
        val today = now.toLocalDateTime(timeZone).date
        val lastVersion = planRepository.planIdsOn(today).maxOfOrNull { it.version } ?: 0
        return PlanId.of(today, lastVersion + 1)
    }
}

/** La petición, o `null` si falta algo. Los bloqueos no se vuelven a derivar: son los del resumen. */
private fun GenerationSummary.toRequest(): PlanGenerationRequest? = when {
    !canGenerate -> null
    profile == null || gym == null || preferences == null -> null
    else -> PlanGenerationRequest(profile, gym, equipment, preferences)
}

private fun AiError.toResult(): GeneratePlanResult = when (this) {
    AiError.Network -> GeneratePlanResult.Unavailable(GenerationUnavailable.Network)
    AiError.Timeout -> GeneratePlanResult.Unavailable(GenerationUnavailable.Timeout)
    is AiError.RateLimited -> GeneratePlanResult.Unavailable(GenerationUnavailable.RateLimited(retryAfterSeconds))
    AiError.Upstream -> GeneratePlanResult.Unavailable(GenerationUnavailable.Upstream)
    is AiError.InvalidInput -> GeneratePlanResult.InvalidInput(problems)
    is AiError.InvalidResponse -> GeneratePlanResult.InvalidPlan(problems)
}
