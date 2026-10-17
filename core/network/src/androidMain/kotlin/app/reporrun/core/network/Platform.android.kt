package app.reporrun.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun platformEngine(): HttpClientEngine = OkHttp.create()

internal actual val platformBaseUrl: String = NetworkBuildConfig.ROR_BASE_URL_ANDROID
