## Context

- **A-12** dejó `createHttpClient(config, json)` con `expectSuccess = false` (un `4xx`/`5xx` vuelve como respuesta) y `runNetworkCatching`, que da `NetworkError.Timeout` o `Connectivity`.
- **A-15/A-14** dejaron los DTO, `ContractJson` (ignora claves desconocidas: la app lee respuestas y es tolerante) y el port `PlanGenerator` con `AiError`.
- **A-06/A-08** fijan los estados de la Lambda: `400 INVALID_INPUT` (incluido el cuerpo demasiado grande), `401`, `404` y `405` con `INVALID_INPUT` porque el `enum` no tiene código propio, `422 PLAN_INVALID`, `429 RATE_LIMITED` con `Retry-After`, `502 UPSTREAM_ERROR` y `504 TIMEOUT`.
- **A-09**: el runner con `FAKE_OPENAI=true` devuelve `generate-response.small-gym.json` sin adaptar y lo valida contra la petición. Con otro inventario contesta `422`.
- El plan `PLAN_LAMBDA_LOCAL.md` §5.1 dejó sin decidir cómo elige la app entre fake y proxy.

## Goals / Non-Goals

**Goals:** los cuatro criterios de la tarjeta y las tres decisiones abiertas (conmutador, recorte del fake, gimnasio de ejemplo).

**Non-Goals:** los del proposal.

## Decisions

### 1. Errores: por estado HTTP, y el código del cuerpo no manda

| Estado | `AiError` | Por qué |
|---|---|---|
| `200` que se lee y se mapea | — (`Success`) | |
| `200` que no se deja leer o no se mapea | `InvalidResponse(problems)` | La respuesta no cumple el contrato |
| `400` | `InvalidInput(details)` | Lo único que la persona puede arreglar cambiando datos |
| `401`, `404`, `405` | `Upstream` | Build mal configurada o fallo de integración; **no** es culpa de la persona aunque el cuerpo diga `INVALID_INPUT` |
| `422` | `InvalidResponse(details)` | La reparación se agotó en esa petición; **no es definitivo**: C-11 ofrece «Generar otra vez» |
| `429` | `RateLimited(retryAfterSeconds)` | Del cuerpo; si no viene, de la cabecera `Retry-After` |
| `504` | `Timeout` | El modelo no llegó a tiempo: para la persona es lo mismo que un timeout de red |
| `502`, otro `5xx`, cualquier otro estado | `Upstream` | Un estado que la app no conoce no es culpa de la persona |
| `NetworkError.Timeout` / `Connectivity` | `Timeout` / `Network` | |

Los `problems` salen de `details` como `"<path>: <reason>"`, que la Lambda ya escribe sin datos de la persona. Sin `details` legibles queda un texto fijo con el estado. **Nunca** se copia el mensaje de una excepción de `kotlinx.serialization`: incluye un trozo del JSON, y una respuesta lleva textos del plan.

### 2. El cuerpo enviado es el `inputSnapshotJson`

Se codifica la petición una vez con `ContractJson` y ese mismo texto se envía (`TextContent`) y se guarda como `inputSnapshotJson`. Así lo guardado es **exactamente** lo enviado, que es lo que promete `GeneratedPlan`. `promptVersion` = `plan-gen-v1`, `locale` = `es`, constantes de `:core:ai`.

### 3. Cancelación

Nada la atrapa: `runNetworkCatching` relanza `CancellationException` y la llamada de Ktor se cancela con la corrutina. Codificar y mapear corre en `dispatchers.default`, también cancelable. Lo que no se puede cancelar desde la app es la llamada al modelo en la Lambda (riesgo ya anotado en A-09).

### 4. El conmutador: interruptor en Ajustes, solo en depuración

Se elige la segunda opción de §5.1 con un cerrojo:

- **`AiBackend { FAKE, PROXY }`** en `:core:model`, guardado en DataStore (`ai_backend`) por `SettingsRepository`. Sin valor guardado, `FAKE`: la build de depuración sigue funcionando sin levantar el runner, como hasta hoy.
- **`SelectablePlanGenerator`** en `:core:ai` es el único `PlanGenerator` del grafo: en cada `generate` pregunta a `AiBackendSelection` y delega en el fake o en el proxy. Se lee en cada llamada, así que el cambio vale sin reiniciar.
- **El cerrojo vive en la raíz de composición.** `composeApp` registra `AiBackendSelection`: si `AppInfo.isDebug`, lo guardado; si no, **siempre `PROXY`**, ignorando lo guardado. Y solo con `isDebug` le pasa a Ajustes `showDeveloperOptions = true`. Una build que no es de depuración no enseña el control ni lo obedece: desaparece la pega de §5.1 («publicado, alguien puede apagar el generador real»).
- **`isDebug`**: `ApplicationInfo.FLAG_DEBUGGABLE` en Android. `composeApp` es una librería KMP sin `BuildConfig` propio, y el `BuildConfig.DEBUG` de `androidApp` no lo ve; el flag del paquete instalado dice lo mismo. En iOS, `Platform.isDebugBinary`.

Alternativas descartadas: por tipo de build (dos APK para enseñar los dos comportamientos) y quitar el fake (no se podría trabajar sin el runner, y los tests y C-11 lo usan).

### 5. El fake deja de recortar

Se borra `SessionFit.kt` (`fittedTo`) y su requisito en `ia-sin-red`. Con sesiones más cortas que el ejemplo, el plan del fake ya no pasa la regla de volumen y la app enseña «plan inválido»: es lo que haría el proxy con ese mismo ejemplo, y no es un fallo.

**La adaptación al inventario se queda.** La nota de A-09 pedía quitarla también, pero la tarjeta de esta tarea solo pide el recorte, y sin adaptación el fake no serviría con ningún gimnasio real. Queda como punto abierto.

### 6. Gimnasio de ejemplo

Para volver a ver un plan en local con el runner en modo fake. Solo en depuración, una fila de Ajustes:

- `SampleGymSource` (port en `:core:domain`, implementado en `:core:ai` desde `ContractExampleSources`, la copia del contrato) da el gimnasio, los 11 aparatos **con los ids del ejemplo** (`eq_chest_press`…) y las preferencias. Sin `startDate`: la del ejemplo ya pasó.
- `LoadSampleGymUseCase` guarda el gimnasio con un id fijo (`gym_ejemplo`), su inventario, lo deja activo y **sustituye las preferencias**. Es idempotente: cargarlo dos veces deja lo mismo. La fila lo avisa.
- Es una **petición** de ejemplo, no una respuesta adaptada: no reabre la deuda del punto 5. El fixture es sintético.

### 7. El cliente en el grafo

`composeApp` registra `NetworkConfig.fromBuildConfig(appInfo.versionName, settings::installId)`; `aiModule` crea el `HttpClient` con `ContractJson` (es `internal` de `:core:ai`) y el `ProxyPlanGenerator`. `aiModule` no conoce `AppInfo`.

## Risks / Trade-offs

- **`:core:ai` ya no compila sin `local.properties`** (arrastra `:core:network`). El CI tendrá que poner las tres claves o variables de entorno. → Ya pasaba con `check`, que compila `:core:network`.
- **Con el fake y sesiones cortas, «plan inválido».** → Documentado; el gimnasio de ejemplo da 75 min.
- **El cerrojo depende de que `FLAG_DEBUGGABLE` sea falso en release.** Lo pone AGP según el build type; no hay release firmada hoy.
- **Darwin y `isDebugBinary` sin compilar** hasta S-07.
