package app.reporrun.core.network

/** El resultado de una llamada de red: lo que devolvió, o el [NetworkError] por el que no llegó. */
sealed interface NetworkResult<out T> {
    data class Success<out T>(val value: T) : NetworkResult<T>

    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}
