package app.reporrun.core.ai.proxy

import app.reporrun.core.ai.contract.ApiError
import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.domain.port.AiError
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException

/*
 * Respuesta del proxy distinta de 200 → AiError. **Manda el estado HTTP, no el `code` del
 * cuerpo**: el 401 de `X-App-Key` (A-08), el 404 y el 405 viajan con `code: INVALID_INPUT`
 * porque el enum del contrato no tiene código propio, y ninguno es culpa de la persona.
 * Leídos por el código serían `InvalidInput`, que la pantalla enseña como «arregla tus datos».
 *
 * El cuerpo (`ApiError`) solo aporta los `details` del 400 y del 422 y los segundos del 429.
 */

private val BAD_REQUEST = HttpStatusCode.BadRequest
private val UNPROCESSABLE = HttpStatusCode.UnprocessableEntity
private val TOO_MANY_REQUESTS = HttpStatusCode.TooManyRequests
private val GATEWAY_TIMEOUT = HttpStatusCode.GatewayTimeout

internal fun ProxyReply.toAiError(): AiError {
    val error = apiErrorOrNull(body)
    return when (status) {
        // Lo único que la persona arregla cambiando datos (incluye el cuerpo demasiado grande).
        BAD_REQUEST -> AiError.InvalidInput(error.problems(status))

        // La reparación se agotó en esa petición; una generación nueva es otra muestra del modelo.
        UNPROCESSABLE -> AiError.InvalidResponse(error.problems(status))

        TOO_MANY_REQUESTS -> AiError.RateLimited(error?.retryAfterSeconds ?: retryAfterHeader?.toSecondsOrNull())

        GATEWAY_TIMEOUT -> AiError.Timeout

        // 401, 404, 405, 502 y cualquier estado que la app no conoce: fallo del servicio.
        else -> AiError.Upstream
    }
}

private fun apiErrorOrNull(body: String): ApiError? = try {
    ContractJson.decodeFromString(ApiError.serializer(), body)
} catch (_: SerializationException) {
    null
}

/**
 * `"<path>: <reason>"` por cada detalle: la Lambda los escribe sin datos de la persona. Sin
 * detalles legibles, un texto fijo con el estado; nunca el cuerpo tal cual.
 */
private fun ApiError?.problems(status: HttpStatusCode): List<String> =
    this?.details?.takeIf { it.isNotEmpty() }?.map { "${it.path}: ${it.reason}" }
        ?: listOf("respuesta ${status.value} sin detalles")

/** `Retry-After` en segundos. La forma de fecha HTTP no la usa la Lambda, y se ignora. */
private fun String.toSecondsOrNull(): Int? = trim().toIntOrNull()?.takeIf { it >= 0 }
