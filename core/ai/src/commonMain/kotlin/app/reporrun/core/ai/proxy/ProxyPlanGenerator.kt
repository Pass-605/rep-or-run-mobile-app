package app.reporrun.core.ai.proxy

import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.ai.contract.GeneratePlanRequest
import app.reporrun.core.ai.contract.GeneratePlanResponse
import app.reporrun.core.ai.mapper.toDto
import app.reporrun.core.ai.mapper.toGeneratedPlan
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.usecase.IdGenerator
import app.reporrun.core.domain.validation.Mapped
import app.reporrun.core.network.NetworkError
import app.reporrun.core.network.NetworkResult
import app.reporrun.core.network.runNetworkCatching
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/** La ruta de generación del proxy (A-06). */
internal const val GENERATE_PATH: String = "/v1/plans:generate"

/** El prompt que pide la app. Cambiarlo es cambiar lo que la Lambda tiene que mantener (A-16). */
internal const val PLAN_PROMPT_VERSION: String = "plan-gen-v1"

/** La app solo existe en español. */
internal const val APP_LOCALE: String = "es"

/**
 * [PlanGenerator] que pide el plan al proxy (`POST /v1/plans:generate`) con el cliente de
 * `:core:network`.
 *
 * El cuerpo se codifica una vez y ese mismo texto se envía y se guarda como
 * `inputSnapshotJson`: lo guardado es exactamente lo enviado. La respuesta se lee con
 * [ContractJson], que ignora los campos que no conoce.
 *
 * Los errores se traducen **por estado HTTP**, no por el `code` del cuerpo (ver
 * [toAiError]). Es cancelable: nada atrapa la `CancellationException`.
 *
 * @param client el de `createHttpClient`, con la URL base y las cabeceras del proxy.
 * @param ids identificadores de los días y ejercicios del plan.
 */
class ProxyPlanGenerator(
    private val client: HttpClient,
    private val ids: IdGenerator,
    private val dispatchers: AppDispatchers,
) : PlanGenerator {
    override suspend fun generate(request: PlanGenerationRequest): PlanGenerationResult {
        val body = withContext(dispatchers.default) {
            ContractJson.encodeToString(
                GeneratePlanRequest.serializer(),
                request.toDto(promptVersion = PLAN_PROMPT_VERSION, locale = APP_LOCALE),
            )
        }
        val reply = runNetworkCatching {
            val response = client.post(GENERATE_PATH) { setBody(TextContent(body, ContentType.Application.Json)) }
            ProxyReply(response.status, response.headers[HttpHeaders.RetryAfter], response.bodyAsText())
        }

        return when (reply) {
            is NetworkResult.Failure -> PlanGenerationResult.Failure(reply.error.toAiError())

            is NetworkResult.Success -> if (reply.value.status == HttpStatusCode.OK) {
                withContext(dispatchers.default) { planFrom(reply.value.body, inputSnapshotJson = body) }
            } else {
                PlanGenerationResult.Failure(reply.value.toAiError())
            }
        }
    }

    private fun planFrom(body: String, inputSnapshotJson: String): PlanGenerationResult {
        val response = try {
            ContractJson.decodeFromString(GeneratePlanResponse.serializer(), body)
        } catch (_: SerializationException) {
            // El mensaje de la excepción copia un trozo del JSON, y la respuesta lleva textos del
            // plan: no se reenvía.
            return PlanGenerationResult.Failure(AiError.InvalidResponse(listOf(UNREADABLE_PLAN)))
        }
        return when (val mapped = response.toGeneratedPlan(ids, inputSnapshotJson)) {
            is Mapped.Valid -> PlanGenerationResult.Success(mapped.value)
            is Mapped.Invalid -> PlanGenerationResult.Failure(AiError.InvalidResponse(mapped.errors))
        }
    }

    private companion object {
        const val UNREADABLE_PLAN = "respuesta 200: no cumple generate-response.schema.json"
    }
}

/** Lo que interesa de una respuesta del proxy, ya leída. */
internal data class ProxyReply(val status: HttpStatusCode, val retryAfterHeader: String?, val body: String)

internal fun NetworkError.toAiError(): AiError = when (this) {
    NetworkError.Timeout -> AiError.Timeout
    NetworkError.Connectivity -> AiError.Network
}
