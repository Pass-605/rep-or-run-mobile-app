package app.reporrun.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun platformEngine(): HttpClientEngine = OkHttp.create()

// La JVM solo existe para los tests de :core:ai, en la máquina anfitriona, igual que el
// simulador de iOS: las dos llegan al runner por localhost.
internal actual val platformBaseUrl: String = NetworkBuildConfig.ROR_BASE_URL_IOS
