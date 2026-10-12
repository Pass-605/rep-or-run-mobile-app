## Context

`PlanGenerator` devuelve un `GeneratedPlan` (días en dominio y metadatos del servicio), no un `Plan`: el id, el gimnasio, el estado y las fechas los pone quien guarda. `BuildGenerationSummaryUseCase` ya deriva los `GenerationBlocker`, y `PlanValidator` ya dice si un plan se puede guardar. Falta el caso de uso que los encadena. El criterio 2 de la tarjeta se corrigió el 11/10: la reparación vive en la Lambda (plan §11) y la app no reintenta.

## Goals / Non-Goals

**Goals:**

- Un plan inválido, una generación fallida o una cancelada no dejan nada en Room.
- Las condiciones para generar tienen una sola definición (la del resumen).
- C-11 puede saber, sin leer texto, si ofrecer «Reintentar».

**Non-Goals:**

- Los de la propuesta.

## Decisions

### Flujo

`GeneratePlanUseCase` en `:core:domain` (`usecase`), con inyección por constructor: `BuildGenerationSummaryUseCase`, `PlanGenerator`, `PlanValidator`, `PlanRepository`, `Clock`, `TimeZone`.

1. Toma el **primer valor** del resumen. Con bloqueos → `Blocked(blockers)`, sin llamar al generador.
2. Llama a `PlanGenerator.generate`.
3. `PlanValidator.validate(plan.days, request.equipment, request.preferences)`.
4. Comprueba que la corrutina sigue activa, calcula el id y llama a `PlanRepository.savePlan` con estado `DRAFT`.

`createdAt` y la fecha del id salen del `Clock` de la app, no de `generatedAt` (hora del servicio). `reviewDate` queda en `null`: la pone la activación.

### Resultado

`GeneratePlanResult`, sellado:

| Caso | Sale de | ¿Reintenta la persona? |
|---|---|---|
| `Saved(plan)` | plan válido y guardado | — |
| `Blocked(blockers)` | resumen con bloqueos | No: tiene que completar datos |
| `Unavailable(reason)` | `AiError.Network`, `Timeout`, `RateLimited`, `Upstream` | **Sí** |
| `InvalidPlan(problems)` | `AiError.InvalidResponse` o `PlanValidator` | **No**: definitivo |
| `InvalidInput(problems)` | `AiError.InvalidInput` | No: tiene que cambiar datos |

`Unavailable` e `InvalidPlan` son tipos distintos, no un `Failure(error)` con un booleano: C-11 no puede tratar igual lo que no distingue el compilador.

**Dónde cae el `422 PLAN_INVALID`.** Hoy nada lo produce. Cuando exista el adaptador (A-13), lo mapea a `AiError.InvalidResponse`, y de ahí a `InvalidPlan`. Se corrige el KDoc de `AiError`, que aún decía que `InvalidResponse` «es lo que un reintento puede arreglar»: eso era el criterio anterior. Opción descartada: un `AiError.PlanInvalid` nuevo. Para la app, «la Lambda dice que el plan no vale» y «la respuesta no cumple el contrato» acaban igual: no hay plan y reintentar no es la salida.

### El `N` de `ROR-AAAA-MM-DD-vN`

- **Fecha:** `clock.now()` en la `TimeZone` inyectada: la local de la persona. A las 23:30 del día 11 en su zona el plan es del 11 aunque en UTC ya sea 12.
- **Contador:** port nuevo `PlanRepository.planIdsOn(date)`, que devuelve los ids guardados con esa fecha **en cualquier gimnasio** (el id es clave primaria de la tabla, no del gimnasio). `N` = mayor versión + 1; sin planes, 1. Mayor y no «cuántos hay»: si se borra un plan intermedio, contar repetiría un id existente.
- `PlanId.of(date, version)` en `:core:model`, junto al formato que ya valida.

Opciones descartadas: contar con `observeHistory(gymId)` (no ve otros gimnasios y chocaría con su id); un contador en memoria o en DataStore (se desincroniza de Room).

**Límite conocido:** leer el contador y guardar no son una sola transacción. Dos generaciones simultáneas calcularían el mismo `N` y la segunda inserción fallaría por clave primaria (no se pisa nada). Hoy solo se genera desde una pantalla, de una en una.

**Tensión anotada, sin resolver:** `PlanChange` exige que solo una `v1` carezca de versión anterior. Con `N` contado por día, la segunda generación de un día es `v2` sin anterior. B-07 no crea `PlanChange`; lo decide quien implemente la aprobación de cambios.

### Cancelación

El caso de uso no captura `CancellationException` ni usa `NonCancellable`. Si se cancela mientras el generador espera, sale por ahí y no se llega a guardar. Si el generador devuelve un plan con la corrutina ya cancelada, `ensureActive()` antes de guardar lo corta. `savePlan` es una sola transacción («todo o nada»), así que no hay plan a medias.

Queda una ventana mínima: cancelar justo cuando la transacción ya confirmó deja un `DRAFT` completo y válido, visible en el historial. Cerrarla exigiría borrar tras guardar; no compensa.

### Fecha de inicio ya pasada

`BlockPreferences.effectiveStartDate(today)` en `:core:model`: la fecha guardada si es hoy o posterior; si no, `null`. `BuildGenerationSummaryUseCase` recibe `Clock` y `TimeZone` y emite las preferencias ya normalizadas, así el resumen enseña lo mismo que se envía y la generación lo hereda al leer el resumen. El formulario usa la misma función. **Sin fecha se envía `null`**, que el contrato admite.

Opción descartada: sustituirla por hoy. Sería inventar una elección que la persona no hizo.

## Risks / Trade-offs

- El resumen calcula «hoy» en cada emisión, no al cambiar de día: una pantalla abierta a medianoche no se refresca sola → la generación vuelve a leerlo, así que lo enviado es correcto.
- `:core:database` y `:core:data` se tocan aunque la tarjeta diga `:core:domain` → es una consulta de lectura, sin esquema ni migración.
