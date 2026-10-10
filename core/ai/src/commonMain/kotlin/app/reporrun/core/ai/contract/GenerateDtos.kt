package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable

/** `generate-request.schema.json`: el cuerpo de `POST /v1/plans:generate`. */
@Serializable
data class GeneratePlanRequest(
    val schemaVersion: String,
    val promptVersion: String,
    val locale: String,
    val profile: ProfileDto,
    val gym: GymDto,
    val preferences: BlockPreferencesDto,
)

/**
 * `generate-response.schema.json`: la respuesta 200 de `POST /v1/plans:generate`.
 * [generatedAt] es una fecha y hora ISO tal cual viaja.
 */
@Serializable
data class GeneratePlanResponse(
    val schemaVersion: String,
    val promptVersion: String,
    val model: String,
    val generatedAt: String,
    val usage: UsageDto,
    val plan: PlanDto,
)
