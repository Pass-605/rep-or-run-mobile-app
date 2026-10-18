## 1. build-logic

- [x] 1.1 `GenerateLocalConfigSourceTask` y `localConfigValues`: `local.properties` manda, el entorno cubre lo que falta; falla al ejecutarse nombrando lo que falta. `LocalConfigTest` (7 tests)
- [x] 1.2 `:core:network` → `emptySet()` en `ModuleDependencies.kt`, con dos tests; `CLAUDE.md` §3 y §4

## 2. Módulo

- [x] 2.1 Ktor 3.6.0 en el catálogo (seis librerías aprobadas); `include(":core:network")`; `build.gradle.kts` con `jvm()` y la fuente generada `NetworkBuildConfig`
- [x] 2.2 `NetworkConfig`, `InstallIdProvider`, `createHttpClient` (JSON, timeouts 120/120/15 s, cabeceras en `onRequest`, `expectSuccess = false`, sin reintentos)
- [x] 2.3 `platformEngine` y `platformBaseUrl`: OkHttp en Android y JVM, Darwin en iOS
- [x] 2.4 `NetworkError`, `NetworkResult` y `runNetworkCatching`
- [x] 2.5 Permiso `INTERNET` en el manifest del módulo

## 3. Tests con `MockEngine`

- [x] 3.1 `HttpClientFactoryTest` (6): las tres cabeceras en cada petición, el installId pedido en cada una, URL base, timeouts de 120 s, JSON de ida y vuelta, un `429` es respuesta
- [x] 3.2 `RunNetworkCatchingTest` (7): 121 s → `Timeout` y 119 s no (tiempo virtual), socket mudo → `Timeout`, conexión → `Connectivity`, otra E/S → `Connectivity`, un error de programación se propaga, la cancelación se propaga

## 4. Android y documentación

- [x] 4.1 `androidApp/src/debug/`: HTTP en claro solo hacia `10.0.2.2`
- [x] 4.2 `local.properties.example`, README («Red y configuración local») y CHANGELOG

## 5. Verificación

- [x] 5.1 `gradlew.bat check`: BUILD SUCCESSFUL. `build-logic:test` 28 tests (7 de `LocalConfig`, 18 de reglas); `:core:network` 13 tests en `jvmTest` y 13 en `testAndroidHostTest`, 0 fallos
- [x] 5.2 Sin `ROR_APP_KEY` ni `ROR_BASE_URL_IOS` en `local.properties`: `generateNetworkBuildConfig` falla con «faltan ROR_APP_KEY, ROR_BASE_URL_IOS. Ponlas en local.properties…». Con las dos en el entorno, compila con el valor del entorno
- [x] 5.3 Manifest fusionado: `networkSecurityConfig` en debug, ausente en release
- [ ] 5.4 iOS: `Platform.ios.kt` (Darwin) escrito y **sin compilar** (`needs:macos`, S-07)
