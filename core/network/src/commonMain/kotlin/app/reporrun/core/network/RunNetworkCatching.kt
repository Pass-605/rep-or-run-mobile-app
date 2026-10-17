package app.reporrun.core.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ejecuta [block] y traduce los fallos de red a [NetworkError]. Una cancelación se propaga tal
 * cual, y lo que no es un fallo de E/S es un error de programación: también se propaga.
 */
suspend fun <T> runNetworkCatching(block: suspend () -> T): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    NetworkResult.Failure(e.toNetworkError())
}

internal fun IOException.toNetworkError(): NetworkError = when (this) {
    // No llegó a conectar: para quien llama es lo mismo que no tener red.
    is ConnectTimeoutException -> NetworkError.Connectivity

    is HttpRequestTimeoutException, is SocketTimeoutException -> NetworkError.Timeout

    else -> NetworkError.Connectivity
}
