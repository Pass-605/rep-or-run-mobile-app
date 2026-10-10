package app.reporrun.core.ai.contract

import kotlinx.serialization.json.Json

/** El `schemaVersion` que esta app habla. Cambiarlo es cambiar el contrato (`../CLAUDE.md` §4). */
const val CONTRACT_SCHEMA_VERSION: String = "1"

/**
 * El único `Json` con el que se leen y escriben los DTOs de este paquete.
 *
 * Ignora las claves que no conoce: el contrato declara compatible añadir un campo opcional
 * (README del contrato, «Versionado»), y eso no puede romper una app ya publicada. La
 * estrictez de `additionalProperties: false` es de las entradas y la aplica la Lambda
 * (`400 INVALID_INPUT`). Faltar un campo obligatorio o salirse de un enum sigue siendo un error.
 *
 * Un campo opcional que vale `null` no se escribe (los ejemplos lo omiten); uno obligatorio y
 * anulable sí, porque en los DTOs no tiene valor por defecto.
 */
internal val ContractJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
    encodeDefaults = false
}
