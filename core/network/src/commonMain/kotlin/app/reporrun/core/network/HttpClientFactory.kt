package app.reporrun.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Una generación tarda decenas de segundos. */
internal const val REQUEST_TIMEOUT_MILLIS = 120_000L

/** La Lambda no manda un byte hasta tener el plan: el socket aguanta lo mismo que la petición. */
internal const val SOCKET_TIMEOUT_MILLIS = REQUEST_TIMEOUT_MILLIS

/** Un servidor apagado o una red caída tienen que fallar pronto. */
internal const val CONNECT_TIMEOUT_MILLIS = 15_000L

internal const val HEADER_APP_KEY = "X-App-Key"
internal const val HEADER_APP_VERSION = "X-App-Version"
internal const val HEADER_INSTALL_ID = "X-Install-Id"

/**
 * El cliente HTTP de la app, configurado una vez: la URL base, las cabeceras que exige el proxy
 * en cada petición, JSON con [json] y los timeouts. Sin reintentos: un reintento lo pide siempre
 * la persona. Una respuesta `4xx`/`5xx` no lanza: se devuelve para que quien llama lea su cuerpo.
 *
 * @param engine el de la plataforma; los tests pasan un `MockEngine`.
 */
fun createHttpClient(
    config: NetworkConfig,
    json: Json = Json,
    engine: HttpClientEngine = platformEngine(),
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
    }
    defaultRequest { url(config.baseUrl) }
    install(appHeaders(config))
}

// `defaultRequest` no admite `suspend`, y el installId se lee de DataStore.
private fun appHeaders(config: NetworkConfig) = createClientPlugin("RepOrRunHeaders") {
    onRequest { request, _ ->
        request.headers[HEADER_APP_KEY] = config.appKey
        request.headers[HEADER_APP_VERSION] = config.appVersion
        request.headers[HEADER_INSTALL_ID] = config.installId.installId()
    }
}
