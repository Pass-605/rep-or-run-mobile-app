## Context

La Lambda (A-08) exige `X-App-Key`, `X-Install-Id` y `X-App-Version`, y el runner local (A-09) escucha en el 8787 y lee `ROR_APP_KEY` de su `.env`. `:core:ai` ya tiene `jvm()` para sus tests de contrato y la regla de dependencias le permite `:core:network`. `build-logic` ya genera fuente Kotlin para `:core:ai` (`GenerateJsonSourceTask`).

## Goals / Non-Goals

**Goals:** los cuatro criterios de la tarjeta, sin plugin nuevo y sin valores en git.

**Non-Goals:** los del proposal.

## Decisions

### 1. Configuración: tarea de `build-logic`, no BuildKonfig

Se eligió (con visto bueno) una tarea hermana de `GenerateJsonSourceTask` antes que el plugin BuildKonfig: no añade un plugin de terceros cuya compatibilidad con AGP 9, el plugin KMP de Android y Kotlin 2.4 está sin verificar, el mensaje de error es nuestro y se prueba como `JsonSource`. A cambio la mantenemos nosotros.

- `LocalConfig.resolve(names, propertiesText, env)` decide los valores: manda `local.properties` y, si allí falta o está en blanco, la variable de entorno del mismo nombre. Se lee con `providers.fileContents` y `providers.environmentVariable`, compatible con la configuration cache.
- `GenerateLocalConfigSourceTask` recibe los nombres obligatorios y los valores encontrados. **Falla al ejecutarse**, no al configurar: solo rompe quien compila `:core:network`, no cualquier `./gradlew`.
- La tarea **no es `@CacheableTask`**: los valores no tienen por qué acabar en una caché de build. Generar el archivo cuesta nada.
- Las constantes se llaman igual que la clave (`NetworkBuildConfig.ROR_APP_KEY`): un solo nombre en el `.env` del runner, en `local.properties` y en el código.

**Sin debug/release.** Una librería KMP no tiene build types: `commonMain` es el mismo en todas las builds, y BuildKonfig tampoco lo resolvía (sus *flavors* son una propiedad suya). Por eso falta = falla, siempre. Hoy no existe una build de release; cuando exista, esta decisión se reabre.

### 2. La URL por plataforma

Dos claves, `ROR_BASE_URL_ANDROID` (`http://10.0.2.2:8787`) y `ROR_BASE_URL_IOS` (`http://localhost:8787`): `10.0.2.2` no existe en el simulador. Un `expect`/`actual` de una línea elige la suya. La JVM (solo para los tests de `:core:ai`) usa la de iOS: los dos corren en la máquina anfitriona.

### 3. Lo que es de runtime

`X-Install-Id` es un UUID por instalación que vive en DataStore, y la versión es de la plataforma (`AppInfo` de Q-01). Ninguno de los dos es configuración de build: `NetworkConfig` recibe `appVersion` y un `InstallIdProvider` (`suspend`). Las cabeceras se ponen con un plugin de cliente en `onRequest`, que admite `suspend`; `defaultRequest` no.

### 4. Timeouts

- **Petición: 120 s**, lo que pide la tarjeta (una generación tarda decenas de segundos).
- **Socket: 120 s también.** La Lambda no manda un byte hasta tener el plan; con el valor por defecto de OkHttp (10 s de lectura) la petición moriría mucho antes.
- **Conexión: 15 s.** Un runner apagado o una red caída deben fallar pronto, no a los dos minutos.

### 5. Engines

Un `expect fun platformEngine()`: OkHttp en Android y en la JVM, Darwin en iOS. `createHttpClient` acepta otro engine para los tests (`MockEngine`). Es de las pocas cosas para las que `CLAUDE.md` §3 admite `expect`/`actual`: no hay forma común de elegir el engine.

### 6. `NetworkError`: dos casos

| Excepción | `NetworkError` |
|---|---|
| `HttpRequestTimeoutException`, `SocketTimeoutException` | `Timeout`: hubo conexión y no llegó respuesta a tiempo |
| `ConnectTimeoutException` y cualquier otra `IOException` | `Connectivity`: no se pudo conectar o se cortó |

Son los dos que `AiError` distingue (`Timeout` y `Network`); separar «sin red» de «conexión cortada» exigiría leer excepciones de cada plataforma y A-13 los trataría igual. Una `CancellationException` **nunca** se traduce: se propaga. Lo que no es `IOException` es un fallo de programación y también se propaga.

Un `4xx`/`5xx` **no** es un `NetworkError`: hubo respuesta y lleva un `ApiError`. `expectSuccess` se queda en `false` y A-13 decide por el estado HTTP.

### 7. JSON

`createHttpClient` recibe el `Json` que instala en `ContentNegotiation`, con el `Json` por defecto de kotlinx. Si la lectura de respuestas debe ser estricta o tolerante está pendiente para A-13 (nota de A-15) y no se decide aquí.

### 8. Android: HTTP en claro, solo en debug y solo hacia `10.0.2.2`

`androidApp/src/debug/` con un `network_security_config` que permite texto plano para ese dominio, sin `cleartextTrafficPermitted` global. Release no lo lleva. El permiso `INTERNET` lo declara `:core:network`, que es quien lo necesita.

## Risks / Trade-offs

- **Un clon nuevo no compila `:core:network` sin `local.properties`** → buscado: falla diciendo qué falta, no con un `401`.
- **La clave acaba en el APK** → esperado: no es secreta; filtra tráfico automático. Lo que se evita es que entre en git.
- **Darwin sin compilar** → queda anotado como no verificado hasta S-07.
