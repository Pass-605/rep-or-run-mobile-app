package app.reporrun.core.domain.port

/**
 * Por qué no hay plan o revisión. Lo comparten [PlanGenerator] y [PlanReviewer], y es un valor:
 * ninguna implementación deja subir una excepción por un fallo esperable.
 *
 * La cancelación **no** está aquí: una corrutina cancelada termina con su
 * `CancellationException`, sin resultado y sin error.
 *
 * Quien orquesta distingue con esto qué merece otro intento, que siempre es de la persona y
 * nunca automático: [Network], [Timeout] y [Upstream] se reintentan tal cual; [RateLimited],
 * tras esperar. [InvalidResponse] también admite otro intento, pero no es repetir la petición:
 * es pedir una generación nueva, que cuesta otra llamada. [InvalidInput] es el único
 * definitivo: hasta que cambien los datos enviados, repetirlo no lo arregla.
 */
sealed interface AiError {
    /** No se pudo hablar con el servicio. */
    data object Network : AiError

    /** El servicio no contestó a tiempo. */
    data object Timeout : AiError

    /**
     * Demasiadas peticiones.
     *
     * @property retryAfterSeconds cuánto esperar antes de reintentar, si el servicio lo dijo.
     */
    data class RateLimited(val retryAfterSeconds: Int?) : AiError

    /** El servicio falló por su lado. */
    data object Upstream : AiError

    /**
     * Lo que se envió no sirve para generar o revisar.
     *
     * @property problems qué falla y dónde (`gym.equipment: …`), sin datos de la persona.
     */
    data class InvalidInput(val problems: List<String>) : AiError

    /**
     * Llegó una respuesta, pero no cumple el contrato: no se deja leer, o rompe una regla que
     * el modelo de dominio no admite. No hay plan ni revisión que usar.
     *
     * Aquí cae también el `422 PLAN_INVALID` del proxy: la reparación vive en la Lambda (plan
     * §11) y se agota dentro de esa petición, así que la app no repara ni reintenta por su
     * cuenta. Eso no lo hace definitivo: una generación nueva es otra muestra del modelo y
     * puede salir bien. Pedirla es decisión de la persona, y cuesta otra llamada.
     *
     * @property problems todos los problemas encontrados, no solo el primero, con la ruta del
     *   campo (`days[0].exercises[2].dose: …`) y la regla; nunca nombres ni notas.
     */
    data class InvalidResponse(val problems: List<String>) : AiError
}
