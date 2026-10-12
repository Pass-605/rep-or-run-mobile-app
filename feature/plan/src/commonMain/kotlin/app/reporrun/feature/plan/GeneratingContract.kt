// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.plan

import app.reporrun.core.domain.usecase.GenerationUnavailable
import app.reporrun.core.model.PlanId

/**
 * La pantalla Generando: o se está esperando, o la generación terminó sin plan.
 *
 * @property failure `null` mientras se espera; si no, por qué no hay plan y qué se puede hacer.
 * @property secondsUntilRetry cuánto falta para poder reintentar cuando el servicio pidió
 *   esperar; `0` si no pidió nada o ya ha pasado. Mientras no sea `0`, el pie no se puede pulsar:
 *   reintentar antes devolvería el mismo error.
 */
data class GeneratingUiState(val failure: GenerationFailure? = null, val secondsUntilRetry: Int = 0) {
    val isGenerating: Boolean
        get() = failure == null

    val canRetryNow: Boolean
        get() = secondsUntilRetry == 0
}

/**
 * Por qué no hay plan. Cada caso lleva a una acción distinta al pie, y **no son
 * intercambiables**: repetir una petición que no llegó no es lo mismo que pedir otro plan.
 */
sealed interface GenerationFailure {
    /** No se pudo hablar con el servicio. Se reintenta tal cual. */
    data class Unavailable(val reason: GenerationUnavailable) : GenerationFailure

    /**
     * El plan que volvió no es válido y no se ha guardado.
     *
     * @property problems todos los problemas, tal como los da la validación.
     * @property canGenerateAgain `false` cuando ya van [MAX_INVALID_PLANS_IN_A_ROW] seguidos:
     *   entonces la pantalla deja de ofrecer generar y solo deja volver al resumen.
     */
    data class InvalidPlan(val problems: List<String>, val canGenerateAgain: Boolean) : GenerationFailure

    /** El servicio no acepta lo enviado: hay que cambiar datos en el resumen. */
    data class InvalidInput(val problems: List<String>) : GenerationFailure

    /**
     * La generación se quedó a medias: el proceso murió con la espera en pantalla y, al volver,
     * **no se pide otra por cuenta propia**. No es un plan inválido y no cuenta para el tope.
     */
    data object Interrupted : GenerationFailure
}

sealed interface GeneratingEvent {
    /** «Reintentar» o «Generar otra vez»: las dos piden un plan de nuevo. */
    data object RetryClicked : GeneratingEvent

    /** Cancelar la espera, o salir de un error hacia el resumen. */
    data object CloseClicked : GeneratingEvent
}

sealed interface GeneratingEffect {
    /** Hay plan guardado. */
    data class PlanGenerated(val planId: PlanId) : GeneratingEffect

    /** Se vuelve al resumen: por cancelar, por salir de un error o porque faltaba algo. */
    data object BackToSummary : GeneratingEffect
}

/**
 * Cuántos planes inválidos seguidos se aceptan antes de dejar de ofrecer otro. Cada uno es una
 * llamada real al servicio, y el plan de trabajo §5 le pone presupuesto.
 */
const val MAX_INVALID_PLANS_IN_A_ROW: Int = 2
