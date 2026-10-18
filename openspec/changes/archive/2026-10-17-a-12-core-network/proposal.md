Cubre la tarea **A-12** de `../docs/backlog/A-contrato-lambda.md` (`:core:network`: cliente Ktor base).

## Why

A-13 tiene que hablar con el proxy y hoy la app no tiene cliente HTTP. Configurarlo una sola vez, aparte del adaptador, deja en un sitio los engines por plataforma, los timeouts, las cabeceras que la Lambda exige (A-08) y la traducción de los fallos de transporte, y deja a A-13 solo el contrato.

## What Changes

- **`:core:network`**, módulo nuevo (`reporrun.kmp.library` + `jvm()`), con:
  - `createHttpClient(config)`: OkHttp en Android y en la JVM, Darwin en iOS; `ContentNegotiation` JSON; timeout de petición de 120 s.
  - Las cabeceras `X-App-Version`, `X-Install-Id` y `X-App-Key` en cada petición, y la URL base.
  - `NetworkError` (`Timeout`, `Connectivity`) y `runNetworkCatching`, que traduce los fallos de red a ese tipo.
- **Configuración por `local.properties`**, con la variable de entorno del mismo nombre como respaldo: `ROR_APP_KEY`, `ROR_BASE_URL_ANDROID` y `ROR_BASE_URL_IOS`. Una tarea de `build-logic` (`GenerateLocalConfigSourceTask`, hermana de `GenerateJsonSourceTask`) las escribe como fuente Kotlin generada; si falta alguna, la compilación de `:core:network` falla diciendo cuál. `local.properties.example` con los nombres.
- **`build-logic`**: `:core:network` entra en `ModuleDependencies.kt` con `emptySet()`, con su test.
- **`androidApp`**: la build de debug permite HTTP en claro **solo** hacia `10.0.2.2`.
- `CLAUDE.md` §4 deja de decir «BuildKonfig».

## No objetivos

- Hablar con el proxy, los DTO y traducir `NetworkError` a `AiError` → A-13. Este módulo no sabe que existe un plan.
- Reintentos automáticos: un reintento lo pide siempre la persona. No se instala `HttpRequestRetry`.
- Una configuración de release: no hay URL real hasta tener cuentas.

## Capabilities

### New Capabilities

- `cliente-red`: el cliente HTTP común, su configuración y sus errores de transporte.

### Modified Capabilities

Ninguna.

## Impact

- **Dependencias nuevas (aprobadas):** Ktor Client 3.6.0 (`core`, `okhttp`, `darwin`, `content-negotiation`, `serialization-kotlinx-json`) y `ktor-client-mock` en tests. Usa coroutines 1.11.0 y serialization 1.11.0, las del catálogo.
- **Construir `:core:network` exige `local.properties` con las tres claves** (o el entorno). `check` lo compila, así que quien no las tenga verá el error con los nombres que faltan.
- **Contrato, plan, Room, Figma:** no se tocan.
- **iOS:** el engine de Darwin queda escrito y sin compilar (`needs:macos`, S-07).
