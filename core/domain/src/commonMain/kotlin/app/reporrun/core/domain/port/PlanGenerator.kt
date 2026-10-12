package app.reporrun.core.domain.port

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.Gym
import app.reporrun.core.model.PlanDay
import app.reporrun.core.model.UserProfile
import kotlin.time.Instant

/**
 * Genera el plan de un bloque. Es un port: el dominio no sabe si detrás hay un proxy hacia un
 * LLM o unos ejemplos fijos.
 *
 * Lo que devuelve **no es de fiar**: tiene la forma del modelo de dominio, pero nadie ha
 * cruzado todavía sus ejercicios con el inventario. Pasa por `PlanValidator` antes de guardarse.
 */
fun interface PlanGenerator {
    /**
     * Pide un plan para [request]. Suspende lo que tarde el servicio y es cancelable: si se
     * cancela, no devuelve nada. Cualquier otro fallo es un [PlanGenerationResult.Failure].
     */
    suspend fun generate(request: PlanGenerationRequest): PlanGenerationResult
}

/**
 * Lo que se envía para generar.
 *
 * @property gym el gimnasio activo.
 * @property equipment su inventario: el plan solo puede usar estos aparatos (plan §4.1).
 */
data class PlanGenerationRequest(
    val profile: UserProfile,
    val gym: Gym,
    val equipment: List<Equipment>,
    val preferences: BlockPreferences,
)

/**
 * Un plan recién generado, antes de ser una versión guardada: le faltan el `PlanId`, el
 * gimnasio, el estado y las fechas, que pone quien lo guarda.
 *
 * @property days los días con sesión, ya con sus identificadores.
 * @property model modelo que lo generó, tal como lo dijo el servicio.
 * @property generatedAt cuándo se generó, según el servicio.
 * @property inputSnapshotJson el cuerpo exacto que se envió, para poder reproducirlo. Son datos
 *   de salud: se guarda con el plan y no se escribe en ningún log.
 */
data class GeneratedPlan(
    val blockDays: Int,
    val focus: List<String>,
    val notes: List<String>,
    val days: List<PlanDay>,
    val model: String,
    val promptVersion: String,
    val schemaVersion: String,
    val generatedAt: Instant,
    val inputSnapshotJson: String,
)

/** Resultado de [PlanGenerator.generate]. */
sealed interface PlanGenerationResult {
    /** Hay plan. Falta validarlo contra el inventario. */
    data class Success(val plan: GeneratedPlan) : PlanGenerationResult

    /** No hay plan, y [error] dice por qué. */
    data class Failure(val error: AiError) : PlanGenerationResult
}
