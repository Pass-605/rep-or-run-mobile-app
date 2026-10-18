package app.reporrun.core.network

/**
 * Un fallo de transporte: no hubo respuesta HTTP. Un `4xx` o un `5xx` no es un [NetworkError]:
 * hubo respuesta, y su cuerpo dice qué pasó.
 */
sealed interface NetworkError {
    /** Hubo conexión, pero la respuesta no llegó a tiempo. */
    data object Timeout : NetworkError

    /** No se pudo conectar (sin red, servidor apagado, DNS) o la conexión se cortó. */
    data object Connectivity : NetworkError
}
