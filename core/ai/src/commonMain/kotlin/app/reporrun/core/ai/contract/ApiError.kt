package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable

/** `error.schema.json`: el cuerpo de toda respuesta distinta de 200. */
@Serializable
data class ApiError(
    val code: ApiErrorCodeDto,
    val message: String,
    val details: List<ApiErrorDetail>? = null,
    val requestId: String? = null,
    val retryAfterSeconds: Int? = null,
)

/** Un error de validación: [path] es un JSON Pointer a lo que falló y [reason], el motivo. */
@Serializable
data class ApiErrorDetail(val path: String, val reason: String)
