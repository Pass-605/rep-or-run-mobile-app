package app.reporrun.core.network

import io.ktor.client.engine.HttpClientEngine

/** OkHttp en Android y en la JVM, Darwin en iOS. No hay forma común de elegirlo. */
internal expect fun platformEngine(): HttpClientEngine

/** `10.0.2.2` solo existe en el emulador de Android: cada plataforma tiene su URL. */
internal expect val platformBaseUrl: String
