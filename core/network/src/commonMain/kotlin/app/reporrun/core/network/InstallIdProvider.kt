package app.reporrun.core.network

/**
 * El identificador de esta instalación, que viaja en `X-Install-Id`. Es un UUID que se genera
 * una vez y vive en DataStore: no es configuración de build, así que se pide en cada petición.
 */
fun interface InstallIdProvider {
    suspend fun installId(): String
}
