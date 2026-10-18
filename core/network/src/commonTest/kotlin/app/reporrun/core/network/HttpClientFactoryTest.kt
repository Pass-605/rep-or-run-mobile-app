package app.reporrun.core.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpClientFactoryTest {
    private val config = NetworkConfig(
        baseUrl = "http://proxy.test:8787",
        appKey = "test-app-key",
        appVersion = "9.9.9",
        installId = { "install-1" },
    )

    @Test
    fun sendsTheThreeHeadersOnEveryRequest() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = client { request ->
            requests += request
            respond("")
        }

        client.get("/a")
        client.post("/b")

        assertEquals(2, requests.size)
        requests.forEach { request ->
            assertEquals("test-app-key", request.headers["X-App-Key"])
            assertEquals("9.9.9", request.headers["X-App-Version"])
            assertEquals("install-1", request.headers["X-Install-Id"])
        }
    }

    @Test
    fun asksForTheInstallIdOnEachRequest() = runTest {
        var calls = 0
        val seen = mutableListOf<String>()
        val client = client(config.copy(installId = { "install-${++calls}" })) { request ->
            seen += request.headers["X-Install-Id"].orEmpty()
            respond("")
        }

        client.get("/a")
        client.get("/a")

        assertEquals<List<String>>(listOf("install-1", "install-2"), seen)
    }

    @Test
    fun resolvesRelativePathsAgainstTheBaseUrl() = runTest {
        var url = ""
        val client = client { request ->
            url = request.url.toString()
            respond("")
        }

        client.post("/v1/plans:generate")

        assertEquals("http://proxy.test:8787/v1/plans:generate", url)
    }

    @Test
    fun waitsUpTo120SecondsForTheWholeRequestAndForTheSocket() = runTest {
        var timeout: HttpTimeoutConfigSeen? = null
        val client = client { request ->
            val capability = request.getCapabilityOrNull(HttpTimeoutCapability)
            timeout = HttpTimeoutConfigSeen(capability?.requestTimeoutMillis, capability?.socketTimeoutMillis)
            respond("")
        }

        client.get("/a")

        assertEquals(HttpTimeoutConfigSeen(request = 120_000L, socket = 120_000L), timeout)
    }

    @Test
    fun writesAndReadsJson() = runTest {
        var sent = ""
        val client = client { request ->
            sent = request.body.toByteArray().decodeToString()
            respond(
                content = """{"value":"pong"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val response = client.post("/echo") {
            contentType(ContentType.Application.Json)
            setBody(Message("ping"))
        }

        assertEquals("""{"value":"ping"}""", sent)
        assertEquals(Message("pong"), response.body<Message>())
    }

    @Test
    fun returnsAnErrorStatusAsAResponseNotAsAFailure() = runTest {
        val client = client { respond("""{"code":"RATE_LIMITED"}""", HttpStatusCode.TooManyRequests) }

        val result = runNetworkCatching { client.post("/v1/plans:generate") }

        val response = (result as NetworkResult.Success).value
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals("""{"code":"RATE_LIMITED"}""", response.bodyAsText())
    }

    @Serializable
    private data class Message(val value: String)

    private data class HttpTimeoutConfigSeen(val request: Long?, val socket: Long?)

    private fun TestScope.client(
        config: NetworkConfig = this@HttpClientFactoryTest.config,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = createHttpClient(
        config = config,
        engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler(handler)
            },
        ),
    )
}
