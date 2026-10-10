package app.reporrun.core.ai.contract

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cada ejemplo válido de `contract/v1/examples/` se lee, se vuelve a escribir y tiene que
 * quedar igual: ni un campo de menos, ni uno de más, ni un valor distinto.
 */
class ContractRoundTripTest {
    @Test
    fun generateRequestRoundTrips() =
        assertRoundTrips(GeneratePlanRequest.serializer(), "generate-request.small-gym.json")

    @Test
    fun generateResponseRoundTrips() =
        assertRoundTrips(GeneratePlanResponse.serializer(), "generate-response.small-gym.json")

    @Test
    fun reviewRequestWithSufficientDataRoundTrips() =
        assertRoundTrips(ReviewPlanRequest.serializer(), "review-request.sufficient.json")

    @Test
    fun reviewRequestWithInsufficientDataRoundTrips() =
        assertRoundTrips(ReviewPlanRequest.serializer(), "review-request.insufficient.json")

    @Test
    fun reviewResponseWithSufficientDataRoundTrips() =
        assertRoundTrips(ReviewPlanResponse.serializer(), "review-response.sufficient.json")

    @Test
    fun reviewResponseWithInsufficientDataRoundTrips() =
        assertRoundTrips(ReviewPlanResponse.serializer(), "review-response.insufficient.json")

    @Test
    fun everyValidExampleHasARoundTripTest() {
        assertEquals(COVERED, ContractExamples.validNames)
    }

    @Test
    fun examplesSpeakTheSchemaVersionOfTheApp() {
        val request = ContractJson.decodeFromString(
            GeneratePlanRequest.serializer(),
            ContractExamples.valid("generate-request.small-gym.json"),
        )

        assertEquals(CONTRACT_SCHEMA_VERSION, request.schemaVersion)
    }

    // La comparación de ida y vuelta no distingue «omitido» de `null`; esto sí. Los schemas
    // del modelo exigen todas las claves, también las que valen `null`.
    @Test
    fun writesRequiredNullableFieldsAsNull() {
        val response = ContractJson.decodeFromString(
            ReviewPlanResponse.serializer(),
            ContractExamples.valid("review-response.sufficient.json"),
        )
        val proposal = response.review.proposals.first { it.targetDayOfWeek == null }

        val written = ContractJson.encodeToJsonElement(ProposalDto.serializer(), proposal).jsonObject

        assertEquals(JsonNull, written["targetDayOfWeek"])
        assertEquals(JsonNull, written["replacementEquipmentId"])
    }

    @Test
    fun omitsOptionalFieldsThatAreNull() {
        val equipment = EquipmentDto(
            id = "eq_ejemplo",
            name = "Banco plano",
            category = EquipmentCategoryDto.BENCH,
            loadType = LoadTypeDto.NONE,
        )

        val written = ContractJson.encodeToJsonElement(EquipmentDto.serializer(), equipment).jsonObject

        assertEquals(setOf("id", "name", "category", "loadType"), written.keys)
    }

    // El contrato no trae ejemplo de error: este es sintético y cubre los campos opcionales.
    @Test
    fun apiErrorRoundTrips() {
        val original = """
            {
              "code": "RATE_LIMITED",
              "message": "Demasiadas peticiones. Prueba dentro de un minuto.",
              "details": [{ "path": "/gym/equipment/3/loadType", "reason": "valor no admitido" }],
              "requestId": "req_ejemplo",
              "retryAfterSeconds": 60
            }
        """.trimIndent()

        assertRoundTrips(ApiError.serializer(), original, original)
    }

    @Test
    fun apiErrorWithoutOptionalFieldsRoundTrips() {
        val original = """{ "code": "TIMEOUT", "message": "El servicio tardó demasiado." }"""

        assertRoundTrips(ApiError.serializer(), original, original)
    }

    private fun <T> assertRoundTrips(serializer: KSerializer<T>, example: String) =
        assertRoundTrips(serializer, example, ContractExamples.valid(example))

    private fun <T> assertRoundTrips(serializer: KSerializer<T>, label: String, original: String) {
        val decoded = ContractJson.decodeFromString(serializer, original)
        val encoded = ContractJson.encodeToString(serializer, decoded)

        assertEquals(
            Json.parseToJsonElement(original).normalized(),
            Json.parseToJsonElement(encoded).normalized(),
            "Se pierde o cambia algo al reescribir $label",
        )
        assertEquals(decoded, ContractJson.decodeFromString(serializer, encoded))
    }

    private companion object {
        val COVERED = setOf(
            "generate-request.small-gym.json",
            "generate-response.small-gym.json",
            "review-request.sufficient.json",
            "review-request.insufficient.json",
            "review-response.sufficient.json",
            "review-response.insufficient.json",
        )
    }
}
