package app.reporrun.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class RunNetworkCatchingTest {
    private val config = NetworkConfig(
        baseUrl = "http://proxy.test:8787",
        appKey = "test-app-key",
        appVersion = "9.9.9",
        installId = { "install-1" },
    )

    @Test
    fun aServerSlowerThan120SecondsIsATimeout() = runTest {
        val client = client {
            delay(121.seconds)
            respond("")
        }

        assertEquals(failure(NetworkError.Timeout), runNetworkCatching { client.get("/slow") })
    }

    @Test
    fun aServerThatAnswersWithin120SecondsIsNotATimeout() = runTest {
        val client = client {
            delay(119.seconds)
            respond("ok")
        }

        assertTrue(runNetworkCatching { client.get("/slow") } is NetworkResult.Success)
    }

    @Test
    fun aSilentSocketIsATimeout() = runTest {
        val client = client { throw SocketTimeoutException("read timed out") }

        assertEquals(failure(NetworkError.Timeout), runNetworkCatching { client.get("/a") })
    }

    @Test
    fun failingToConnectInTimeIsConnectivity() = runTest {
        val client = client { throw ConnectTimeoutException("connect timed out") }

        assertEquals(failure(NetworkError.Connectivity), runNetworkCatching { client.get("/a") })
    }

    @Test
    fun anyOtherIoFailureIsConnectivity() = runTest {
        val client = client { throw IOException("connection refused") }

        assertEquals(failure(NetworkError.Connectivity), runNetworkCatching { client.get("/a") })
    }

    @Test
    fun aProgrammingErrorIsNotANetworkError() = runTest {
        val client = client { throw IllegalStateException("bug") }

        assertFailsWith<IllegalStateException> { runNetworkCatching { client.get("/a") } }
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingANetworkError() = runTest {
        val started = CompletableDeferred<Unit>()
        val client = client {
            started.complete(Unit)
            awaitCancellation()
        }
        var result: NetworkResult<*>? = null

        val job = launch { result = runNetworkCatching { client.get("/a") } }
        started.await()
        job.cancel()
        advanceUntilIdle()

        assertTrue(job.isCancelled)
        assertNull(result)
    }

    private fun failure(error: NetworkError) = NetworkResult.Failure(error)

    private fun TestScope.client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        createHttpClient(
            config = config,
            engine = MockEngine(
                MockEngineConfig().apply {
                    dispatcher = StandardTestDispatcher(testScheduler)
                    addHandler(handler)
                },
            ),
        )
}
