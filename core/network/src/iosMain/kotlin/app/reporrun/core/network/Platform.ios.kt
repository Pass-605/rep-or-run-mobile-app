package app.reporrun.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

// Sin compilar en Windows: needs:macos (S-07).
internal actual fun platformEngine(): HttpClientEngine = Darwin.create()

internal actual val platformBaseUrl: String = NetworkBuildConfig.ROR_BASE_URL_IOS
