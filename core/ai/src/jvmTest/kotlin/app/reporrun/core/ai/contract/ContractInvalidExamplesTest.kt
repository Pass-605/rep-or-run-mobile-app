package app.reporrun.core.ai.contract

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Los ejemplos de `contract/v1/examples/invalid/`. Cada uno tiene que fallar donde y por lo
 * que dice `expected.json`, no por cualquier otra cosa.
 *
 * Lo que es **forma** (un campo de menos, un valor fuera de un enum) lo rechazan los DTOs al
 * leer. Un campo de más solo lo rechaza la lectura estricta de los tests ([StrictContractJson]):
 * la app lo ignora y quien rechaza esa petición es la Lambda.
 *
 * Lo que es un **límite** (`minItems`) no lo rechaza nadie aquí: los DTOs no validan, así que
 * esos ejemplos se leen sin error y solo se comprueba que el motivo sigue donde se espera.
 * Rechazarlos es cosa de la Lambda (`400 INVALID_INPUT`) y de los casos de uso que montan la
 * petición.
 */
class ContractInvalidExamplesTest {
    @Test
    fun strictReadingRejectsExtraFieldInProfile() =
        assertRejected("generate-request.campo-extra.json", StrictContractJson) { failure ->
            assertContains(failure.message.orEmpty(), "unknown key 'fullName'")
        }

    @Test
    fun appIgnoresExtraFieldInProfile() {
        val request = ContractJson.decodeFromString(
            GeneratePlanRequest.serializer(),
            ContractExamples.invalid("generate-request.campo-extra.json"),
        )

        assertEquals(CONTRACT_SCHEMA_VERSION, request.schemaVersion)
    }

    // README del contrato, «Versionado»: un campo opcional nuevo en una respuesta es un cambio
    // compatible y no sube `schemaVersion`. Una app ya publicada tiene que seguir leyéndola.
    @Test
    fun readsResponseWithFieldsTheContractDoesNotDefineYet() {
        val original = ContractExamples.valid("generate-response.small-gym.json")
        val extended = JsonObject(
            Json.parseToJsonElement(original).jsonObject.mapValues { (key, value) ->
                if (key == "plan") JsonObject(value.jsonObject + ("blockLabel" to JsonPrimitive("Bloque 1"))) else value
            } + ("cached" to JsonPrimitive(true)),
        )

        val decoded = ContractJson.decodeFromString(GeneratePlanResponse.serializer(), extended.toString())

        assertEquals(ContractJson.decodeFromString(GeneratePlanResponse.serializer(), original), decoded)
    }

    @Test
    fun rejectsUnknownLoadType() = assertRejected("generate-request.loadtype-desconocido.json") { failure ->
        assertContains(failure.message.orEmpty(), "LoadTypeDto does not contain element")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun rejectsExerciseWithoutRir() = assertRejected("generate-response.falta-rir.json") { failure ->
        assertIs<MissingFieldException>(failure)
        assertEquals(listOf("rirLow"), failure.missingFields)
    }

    @Test
    fun rejectsLowercaseMetric() = assertRejected("generate-response.metric-minuscula.json") { failure ->
        assertContains(failure.message.orEmpty(), "MetricDto does not contain element with name 'reps'")
    }

    @Test
    fun rejectsUnknownConfidence() = assertRejected("review-response.confianza-invalida.json") { failure ->
        assertContains(failure.message.orEmpty(), "ConfidenceDto does not contain element")
    }

    @Test
    fun readsEmptyInventoryBecauseDtosDoNotCheckLimits() = assertEmptyListIsRead("generate-request.sin-inventario.json")

    @Test
    fun readsEmptyAvailableDaysBecauseDtosDoNotCheckLimits() = assertEmptyListIsRead("generate-request.sin-dias.json")

    @Test
    fun everyInvalidExampleIsExpectedAndTested() {
        assertEquals(ContractExamples.expectedFailures.keys, ContractExamples.invalidNames)
        assertEquals(REJECTED_BY_DTOS + READ_BY_DTOS, ContractExamples.invalidNames)
    }

    /** El ejemplo no se puede leer, y falla en la ruta de `expected.json`. */
    private fun assertRejected(
        example: String,
        json: Json = ContractJson,
        assertCause: (SerializationException) -> Unit,
    ) {
        val expected = ContractExamples.expectedFailures.getValue(example)
        assertTrue(expected.keyword in SHAPE_KEYWORDS, "$example falla por ${expected.keyword}, que no es de forma")

        val failure = assertFailsWith<SerializationException> {
            json.decodeFromString(serializerOf(expected.schema), ContractExamples.invalid(example))
        }

        assertContains(failure.message.orEmpty(), jsonPathOf(expected.path))
        assertCause(failure)
    }

    /** El ejemplo solo incumple un `minItems`: la lista vacía está donde se espera y se lee. */
    private fun assertEmptyListIsRead(example: String) {
        val expected = ContractExamples.expectedFailures.getValue(example)
        val text = ContractExamples.invalid(example)

        assertEquals("minItems", expected.keyword)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(text).at(expected.path))
        ContractJson.decodeFromString(serializerOf(expected.schema), text)
    }

    private fun serializerOf(schema: String): KSerializer<*> = when (schema) {
        "generate-request.schema.json" -> GeneratePlanRequest.serializer()
        "generate-response.schema.json" -> GeneratePlanResponse.serializer()
        "review-request.schema.json" -> ReviewPlanRequest.serializer()
        "review-response.schema.json" -> ReviewPlanResponse.serializer()
        else -> error("Schema sin DTO: $schema")
    }

    private companion object {
        val SHAPE_KEYWORDS = setOf("additionalProperties", "enum", "required")
        val REJECTED_BY_DTOS = setOf(
            "generate-request.campo-extra.json",
            "generate-request.loadtype-desconocido.json",
            "generate-response.falta-rir.json",
            "generate-response.metric-minuscula.json",
            "review-response.confianza-invalida.json",
        )
        val READ_BY_DTOS = setOf(
            "generate-request.sin-inventario.json",
            "generate-request.sin-dias.json",
        )
    }
}
