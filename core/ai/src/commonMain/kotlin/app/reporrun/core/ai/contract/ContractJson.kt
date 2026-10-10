package app.reporrun.core.ai.contract

import kotlinx.serialization.json.Json

/** El `schemaVersion` que esta app habla. Cambiarlo es cambiar el contrato (`../CLAUDE.md` §4). */
const val CONTRACT_SCHEMA_VERSION: String = "1"

/**
 * El único `Json` con el que se leen y escriben los DTOs de este paquete.
 *
 * Es estricto porque los schemas lo son: todos los objetos de v1 llevan
 * `additionalProperties: false`, así que una clave desconocida es un error y no se descarta.
 * Un campo opcional que vale `null` no se escribe (los ejemplos lo omiten); uno obligatorio y
 * anulable sí, porque en los DTOs no tiene valor por defecto.
 */
internal val ContractJson: Json = Json {
    ignoreUnknownKeys = false
    explicitNulls = true
    encodeDefaults = false
}
