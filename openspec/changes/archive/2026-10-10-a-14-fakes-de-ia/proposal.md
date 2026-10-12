Cubre la tarea **A-14** de `../docs/backlog/A-contrato-lambda.md` (Fakes de IA con fixtures sintéticos, y los ports).

## Why

La Lambda y el adaptador del proxy (A-13) se movieron a v1.1, así que en el MVP no hay nada que genere un plan. B-07 necesita un port al que llamar y C-11 algo que tarde y que falle. Además, el `Json` de A-15 rechaza claves desconocidas también en las respuestas, y el README del contrato dice que la app las ignora: un campo opcional nuevo, que no sube `schemaVersion`, rompería una app publicada.

## What Changes

- **Lectura del contrato:** la app ignora los campos que no conoce. La estrictez es de las entradas y la aplica la Lambda; en la app queda solo en los tests.
- **Ports en `:core:domain`:** generar un plan y revisarlo, en tipos de dominio, con el error como valor: red, tiempo agotado, límite de peticiones, fallo del servicio, entrada rechazada y respuesta que no cumple el contrato.
- **Mapeo DTO ↔ dominio en `:core:ai`**, que devuelve todos los errores de una respuesta y no solo el primero.
- **Fakes en `:core:ai`:** devuelven los ejemplos de `contract/v1/examples/` adaptados al inventario, a los días y al cardio de la persona, con latencia y modo error inyectados.
- **Los ejemplos entran en la app** como fuente Kotlin generada desde `contract/v1/examples/` (la opción que el diseño de A-15 dejó para esta tarea).
- **Grafo de Koin:** `aiModule` en `:core:ai`, añadido en `composeApp`.

## No objetivos

- Cambiar el contrato, un schema o `schemaVersion`.
- `:core:network` y el adaptador del proxy (A-12, A-13).
- `PlanValidator` (B-06) y el caso de uso que orquesta (B-07): la generación completa en el emulador no se puede verificar aquí.
- Un conmutador debug/release: el fake es la única implementación del MVP.
- Una pantalla o un ajuste para cambiar la latencia o el modo error en ejecución.
- Que el texto libre del fake de revisión (resumen, observaciones) describa el check-in real: solo se garantizan las referencias estructuradas.

## Capabilities

### New Capabilities

- `ia-sin-red`: la app obtiene un plan y una revisión sin red, coherentes con el inventario de la persona, con espera y fallo simulables.

### Modified Capabilities

- `contrato-app`: un campo que el contrato no define deja de ser motivo de rechazo al leer.

## Impact

- **Código:** `core/domain` (paquete `port`), `core/ai` (`mapper`, `fake`, `di`, `build.gradle.kts`), `build-logic` (tarea que genera fuente desde JSON), `composeApp` (grafo).
- **Dependencias entre módulos:** `:core:ai` → `:core:domain`, `:core:model` y `composeApp` → `:core:ai`. Las dos primeras ya están en `CLAUDE.md` §3 y en el allowlist. `koin-core` ya estaba en el catálogo.
- **Contrato, plan y Figma:** no se tocan.
- **Discrepancia que se señala, sin resolver:** B-07 pide «un reintento de reparación enviando los errores», pero el plan §11 pone la reparación en la Lambda y `generate-request` no tiene campo para errores. El port no lleva errores de reparación: B-07 reintenta llamando otra vez.
