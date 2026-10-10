package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.io.File

/** Una entrada de `examples/invalid/expected.json`: dónde y por qué keyword falla el ejemplo. */
@Serializable
internal data class ExpectedFailure(val schema: String, val path: String, val keyword: String)

/** Los ejemplos de la copia versionada, `contract/v1/examples/` (ver `build.gradle.kts`). */
internal object ContractExamples {
    private val examplesDir: File = File(
        checkNotNull(System.getProperty("reporrun.contractDir")) {
            "Falta la propiedad reporrun.contractDir: ejecuta los tests con Gradle (:core:ai:jvmTest)."
        },
        "examples",
    )
    private val invalidDir = File(examplesDir, "invalid")

    val validNames: Set<String> get() = examplesDir.jsonNames()

    val invalidNames: Set<String> get() = invalidDir.jsonNames() - EXPECTED_FILE

    val expectedFailures: Map<String, ExpectedFailure> by lazy {
        Json.decodeFromString<Map<String, ExpectedFailure>>(File(invalidDir, EXPECTED_FILE).readText())
    }

    fun valid(name: String): String = File(examplesDir, name).readText()

    fun invalid(name: String): String = File(invalidDir, name).readText()

    private fun File.jsonNames(): Set<String> =
        listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.map { it.name }.toSet()

    private const val EXPECTED_FILE = "expected.json"
}

/**
 * [ContractJson] con la estrictez de los schemas (`additionalProperties: false`). Solo existe
 * en los tests: en la app la aplica la Lambda sobre las entradas, no la app al leer.
 */
internal val StrictContractJson: Json = Json(from = ContractJson) { ignoreUnknownKeys = false }

/**
 * El mismo JSON listo para comparar por valor y no por texto:
 * - los números como `Double`: `170` y `170.0` son el mismo número en el contrato;
 * - sin las claves que valen `null`: en un campo opcional, omitirlo y mandarlo a `null` es lo
 *   mismo para el schema, y los ejemplos hacen las dos cosas (`usualLoad`, `rirPerceived`).
 *   Que un anulable **obligatorio** se escribe siempre lo comprueba su propio test.
 */
internal fun JsonElement.normalized(): JsonElement = when (this) {
    is JsonObject -> JsonObject(filterValues { it !is JsonNull }.mapValues { it.value.normalized() })
    is JsonArray -> JsonArray(map { it.normalized() })
    is JsonNull -> this
    is JsonPrimitive -> if (isString) this else doubleOrNull?.let(::JsonPrimitive) ?: this
}

/** El elemento al que apunta un JSON Pointer como los de `expected.json` (`/gym/equipment/0`). */
internal fun JsonElement.at(pointer: String): JsonElement = pointer.split('/').drop(1).fold(this) { element, segment ->
    when (element) {
        is JsonArray -> element[segment.toInt()]
        is JsonObject -> element.getValue(segment)
        else -> error("$pointer no existe en el ejemplo")
    }
}

/** `/gym/equipment/0/loadType` como lo escribe kotlinx.serialization: `$.gym.equipment[0].loadType`. */
internal fun jsonPathOf(pointer: String): String =
    pointer.split('/').drop(1).joinToString(separator = "", prefix = "$") { segment ->
        if (segment.all(Char::isDigit)) "[$segment]" else ".$segment"
    }
