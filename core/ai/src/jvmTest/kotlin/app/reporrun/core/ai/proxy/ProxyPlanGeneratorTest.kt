package app.reporrun.core.ai.proxy

import app.reporrun.core.ai.contract.ApiError
import app.reporrun.core.ai.contract.ApiErrorCodeDto
import app.reporrun.core.ai.contract.ApiErrorDetail
import app.reporrun.core.ai.contract.ContractExamples
import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.ai.contract.GeneratePlanRequest
import app.reporrun.core.ai.contract.GeneratePlanResponse
import app.reporrun.core.ai.contract.StrictContractJson
import app.reporrun.core.ai.fake.SequentialIds
import app.reporrun.core.ai.fake.generationRequest
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.network.NetworkConfig
import app.reporrun.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/*
 * El adaptador contra un proxy simulado con MockEngine. Las respuestas 200 son los ejemplos del
 * contrato (`contract/v1/examples/`, válidos e inválidos); los cuerpos de error, que el contrato
 * no trae como ejemplo, se construyen con el DTO `ApiError` y se codifican con `ContractJson`.
 */
class ProxyPlanGeneratorTest {
    // ------------------------------------------------------------ 200

    @Test
    fun anOkResponseBecomesAPlan() = runTest {
        val example = ContractJson.decodeFromString(GeneratePlanResponse.serializer(), SMALL_GYM_RESPONSE)

        val plan = success(generate { ok(SMALL_GYM_RESPONSE) })

        assertEquals(example.model, plan.model)
        assertEquals(example.promptVersion, plan.promptVersion)
        assertEquals(example.plan.days.map { it.dayOfWeek.name }, plan.days.map { it.dayOfWeek.name })
        assertEquals(
            example.plan.days.flatMap { day -> day.exercises.map { it.code } },
            plan.days.flatMap { day -> day.exercises.map { it.code.value } },
        )
    }

    @Test
    fun postsTheContractRequestToTheGeneratePath() = runTest {
        var sent: HttpRequestData? = null

        generate { request ->
            sent = request
            ok(SMALL_GYM_RESPONSE)
        }

        val request = checkNotNull(sent)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v1/plans:generate", request.url.encodedPath)
        assertEquals("application/json", request.body.contentType?.withoutParameters()?.toString())
        // Se lee con la estrictez del schema: lo que sale tiene que pasar el 400 de la Lambda.
        val body = StrictContractJson.decodeFromString(GeneratePlanRequest.serializer(), request.bodyText())
        assertEquals(PLAN_PROMPT_VERSION, body.promptVersion)
        assertEquals(APP_LOCALE, body.locale)
    }

    @Test
    fun theSnapshotIsExactlyTheBodyThatWasSent() = runTest {
        var body = ""

        val plan = success(
            generate { request ->
                body = request.bodyText()
                ok(SMALL_GYM_RESPONSE)
            },
        )

        assertEquals(body, plan.inputSnapshotJson)
    }

    @Test
    fun ignoresFieldsOfTheResponseItDoesNotKnow() = runTest {
        val withExtra = JsonObject(
            ContractJson.parseToJsonElement(SMALL_GYM_RESPONSE).jsonObject + ("costCents" to JsonPrimitive(3)),
        ).toString()

        success(generate { ok(withExtra) })
    }

    @Test
    fun anOkResponseThatBreaksTheSchemaIsAnInvalidResponse() = runTest {
        for (name in listOf("generate-response.falta-rir.json", "generate-response.metric-minuscula.json")) {
            val error = failure(generate { ok(ContractExamples.invalid(name)) })

            // Ningún texto de la respuesta se cuela en los problemas.
            assertEquals(AiError.InvalidResponse(listOf(UNREADABLE)), error, name)
        }
    }

    // ------------------------------------------------------------ errores por estado HTTP

    @Test
    fun aBadRequestIsInvalidInputWithItsDetails() = runTest {
        val body = apiError(
            ApiErrorCodeDto.INVALID_INPUT,
            ApiErrorDetail("/gym/equipment", "minItems: hace falta al menos 1"),
        )

        val error = failure(generate { error(HttpStatusCode.BadRequest, body) })

        assertEquals(AiError.InvalidInput(listOf("/gym/equipment: minItems: hace falta al menos 1")), error)
    }

    @Test
    fun aBadRequestWithoutReadableBodyIsStillInvalidInput() = runTest {
        val error = failure(generate { respond("<html>", HttpStatusCode.BadRequest) })

        assertEquals(AiError.InvalidInput(listOf("respuesta 400 sin detalles")), error)
    }

    @Test
    fun aWrongAppKeyIsAnUpstreamFailureAlthoughItsCodeSaysInvalidInput() = runTest {
        val error = failure(generate { error(HttpStatusCode.Unauthorized, apiError(ApiErrorCodeDto.INVALID_INPUT)) })

        assertEquals(AiError.Upstream, error)
    }

    @Test
    fun anUnknownRouteIsAnUpstreamFailure() = runTest {
        val error = failure(generate { error(HttpStatusCode.NotFound, apiError(ApiErrorCodeDto.INVALID_INPUT)) })

        assertEquals(AiError.Upstream, error)
    }

    @Test
    fun aWrongMethodIsAnUpstreamFailure() = runTest {
        val error = failure(
            generate { error(HttpStatusCode.MethodNotAllowed, apiError(ApiErrorCodeDto.INVALID_INPUT)) },
        )

        assertEquals(AiError.Upstream, error)
    }

    @Test
    fun anInvalidPlanAfterRepairIsAnInvalidResponseWithEveryDetail() = runTest {
        val body = apiError(
            ApiErrorCodeDto.PLAN_INVALID,
            ApiErrorDetail("/plan/days/0/exercises/1/equipmentId", "no está en el inventario"),
            ApiErrorDetail("/plan/days/2", "no cabe en sessionMinutes"),
        )

        val error = failure(generate { error(HttpStatusCode.UnprocessableEntity, body) })

        assertEquals(
            AiError.InvalidResponse(
                listOf(
                    "/plan/days/0/exercises/1/equipmentId: no está en el inventario",
                    "/plan/days/2: no cabe en sessionMinutes",
                ),
            ),
            error,
        )
    }

    @Test
    fun tooManyRequestsTakesTheSecondsFromTheBody() = runTest {
        val body = apiError(ApiErrorCodeDto.RATE_LIMITED).copy(retryAfterSeconds = 30)

        val error = failure(generate { error(HttpStatusCode.TooManyRequests, body, retryAfter = "59") })

        assertEquals(AiError.RateLimited(retryAfterSeconds = 30), error)
    }

    @Test
    fun tooManyRequestsFallsBackToTheRetryAfterHeader() = runTest {
        val body = apiError(ApiErrorCodeDto.RATE_LIMITED)

        val error = failure(generate { error(HttpStatusCode.TooManyRequests, body, retryAfter = "59") })

        assertEquals(AiError.RateLimited(retryAfterSeconds = 59), error)
    }

    @Test
    fun tooManyRequestsWithoutSecondsSaysNothingAboutWaiting() = runTest {
        val error = failure(generate { error(HttpStatusCode.TooManyRequests, apiError(ApiErrorCodeDto.RATE_LIMITED)) })

        assertEquals(AiError.RateLimited(retryAfterSeconds = null), error)
    }

    @Test
    fun aBadGatewayIsAnUpstreamFailure() = runTest {
        val error = failure(generate { error(HttpStatusCode.BadGateway, apiError(ApiErrorCodeDto.UPSTREAM_ERROR)) })

        assertEquals(AiError.Upstream, error)
    }

    @Test
    fun aGatewayTimeoutIsATimeout() = runTest {
        val error = failure(generate { error(HttpStatusCode.GatewayTimeout, apiError(ApiErrorCodeDto.TIMEOUT)) })

        assertEquals(AiError.Timeout, error)
    }

    @Test
    fun anUnknownStatusIsAnUpstreamFailure() = runTest {
        val error = failure(generate { respond("", HttpStatusCode.Forbidden) })

        assertEquals(AiError.Upstream, error)
    }

    // ------------------------------------------------------------ transporte

    @Test
    fun noConnectionIsANetworkError() = runTest {
        val error = failure(generate { throw IOException("connection refused") })

        assertEquals(AiError.Network, error)
    }

    @Test
    fun aSilentServerIsATimeout() = runTest {
        val error = failure(generate { throw SocketTimeoutException("read timed out") })

        assertEquals(AiError.Timeout, error)
    }

    // ------------------------------------------------------------ cancelación

    @Test
    fun cancellingWhileTheProxyThinksEndsWithoutResultOrError() = runTest {
        val reached = CompletableDeferred<Unit>()
        val generator = generator { _ ->
            reached.complete(Unit)
            awaitCancellation()
        }

        val call = async { generator.generate(generationRequest()) }
        reached.await()
        call.cancel()
        call.join()

        assertTrue(call.isCancelled)
    }

    // ------------------------------------------------------------ ayudas

    private fun TestScope.generator(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ProxyPlanGenerator {
        val client = createHttpClient(CONFIG, json = ContractJson, engine = MockEngine(handler))
        return ProxyPlanGenerator(client, SequentialIds(), AppDispatchers(UnconfinedTestDispatcher(testScheduler)))
    }

    private suspend fun TestScope.generate(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): PlanGenerationResult = generator(handler).generate(generationRequest())

    private fun success(result: PlanGenerationResult): GeneratedPlan =
        assertIs<PlanGenerationResult.Success>(result, "$result").plan

    private fun failure(result: PlanGenerationResult): AiError = assertIs<PlanGenerationResult.Failure>(result).error

    private fun MockRequestHandleScope.ok(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(status: HttpStatusCode, body: ApiError, retryAfter: String? = null) =
        respond(
            content = ContractJson.encodeToString(ApiError.serializer(), body),
            status = status,
            headers = if (retryAfter == null) {
                headersOf(HttpHeaders.ContentType, "application/json")
            } else {
                headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.RetryAfter to listOf(retryAfter),
                )
            },
        )

    private fun apiError(code: ApiErrorCodeDto, vararg details: ApiErrorDetail) = ApiError(
        code = code,
        message = "Mensaje para la persona.",
        details = details.toList().ifEmpty { null },
        requestId = "req-1",
    )

    private suspend fun HttpRequestData.bodyText(): String = body.toByteArray().decodeToString()

    private companion object {
        val CONFIG = NetworkConfig(
            baseUrl = "http://proxy.test:8787",
            appKey = "test-app-key",
            appVersion = "9.9.9",
            installId = { "install-1" },
        )

        const val UNREADABLE = "respuesta 200: no cumple generate-response.schema.json"

        val SMALL_GYM_RESPONSE: String = ContractExamples.valid("generate-response.small-gym.json")
    }
}
