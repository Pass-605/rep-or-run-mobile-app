Cubre la tarea **A-13** de `../docs/backlog/A-contrato-lambda.md` (ProxyPlanGenerator).

## Why

Es la última pieza de la línea A en la app: con ella la app pide el plan por HTTP al proxy que levanta A-09, en vez de sacarlo siempre de los ejemplos. A-12 dejó el cliente; A-15 y A-14, los DTO, el `ContractJson` y el port. Falta el adaptador, elegir entre él y el fake, y quitarle al fake lo que ahora decide la Lambda.

## What Changes

- **`ProxyPlanGenerator`** en `:core:ai`: implementa `PlanGenerator`, envía `GeneratePlanRequest` a `POST /v1/plans:generate` y convierte la respuesta en `GeneratedPlan`. Traduce los errores **por estado HTTP primero**: `401`, `404` y `405` son `AiError.Upstream` aunque su cuerpo diga `INVALID_INPUT`; `422 PLAN_INVALID` es `AiError.InvalidResponse`, que no es definitivo. Cancelable.
- **Conmutador fake/proxy**: un interruptor en Ajustes **visible solo en builds de depuración**. En una build que no es de depuración se usa siempre el proxy y el interruptor no existe. La elección se guarda en DataStore.
- **`AppInfo.isDebug`**: `FLAG_DEBUGGABLE` en Android, `Platform.isDebugBinary` en iOS.
- **Gimnasio de ejemplo**, solo en depuración: una fila de Ajustes crea el gimnasio de `generate-request.small-gym.json` (11 aparatos con sus ids), lo deja activo y guarda sus preferencias (lunes, miércoles y sábado, 75 min, caminadora). Con él, el runner en `FAKE_OPENAI=true` responde `200`.
- **El fake deja de recortar** cada día a `sessionMinutes` (deuda de A-09, plan §11.3). Sigue adaptando el ejemplo al inventario.
- `composeApp` arma el `HttpClient` (`createHttpClient` con `ContractJson`) con la versión de `AppInfo` y el `installId` de `SettingsRepository`. `androidApp` recibe `INTERNET` al fusionar manifiestos.

## No objetivos

- `PlanReviewer` por HTTP: línea E, `moved:v1.1`. El revisor sigue siendo el fake.
- `template.yaml` (A-10).
- **Tocar el contrato.** Los cuerpos de error que no trae `examples/` se construyen con el DTO `ApiError`, no se añaden ejemplos.

## Capabilities

### New Capabilities

- `generador-proxy`: pedir el plan al proxy y traducir su respuesta y sus errores.
- `opciones-depuracion`: el conmutador fake/proxy y el gimnasio de ejemplo, solo en depuración.

### Modified Capabilities

- `ia-sin-red`: se retira «El plan sin red cabe en los minutos de sesión».

## Impact

- **Dependencias de proyecto** ya previstas: `:core:ai` → `:core:network` (en el allowlist) y `composeApp` → `:core:network` (raíz de composición, sin regla). Ninguna librería nueva: `ktor-client-mock` ya está en el catálogo.
- **DataStore:** una clave nueva, `ai_backend`. Sin migración de Room.
- **Construir `:core:ai` exige ya `local.properties`** con las claves de A-12, porque compila `:core:network`.
- **Contrato, plan, Figma:** no se tocan. La sección de depuración de Ajustes no está en Figma: usa solo componentes del design system.
- **iOS:** `isDebugBinary` escrito y sin compilar (`needs:macos`, S-07).
