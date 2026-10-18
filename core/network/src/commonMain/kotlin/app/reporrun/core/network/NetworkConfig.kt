package app.reporrun.core.network

/**
 * Lo que el cliente necesita para hablar con el proxy.
 *
 * @property baseUrl a la que se resuelven las rutas relativas.
 * @property appKey va en `X-App-Key`. No es un secreto: viaja dentro de la app. Filtra el tráfico
 *   automático antes de que gaste un token; no autentica a nadie.
 * @property appVersion va en `X-App-Version`. La aporta la plataforma.
 * @property installId da el valor de `X-Install-Id` en cada petición.
 */
data class NetworkConfig(
    val baseUrl: String,
    val appKey: String,
    val appVersion: String,
    val installId: InstallIdProvider,
) {
    companion object {
        /**
         * La URL base y la clave de esta build (de `local.properties`, ver
         * `local.properties.example`), con la versión y la instalación que da quien llama.
         */
        fun fromBuildConfig(appVersion: String, installId: InstallIdProvider): NetworkConfig = NetworkConfig(
            baseUrl = platformBaseUrl,
            appKey = NetworkBuildConfig.ROR_APP_KEY,
            appVersion = appVersion,
            installId = installId,
        )
    }
}
