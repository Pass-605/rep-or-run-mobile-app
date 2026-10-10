Cubre la tarea **A-15** de `../docs/backlog/A-contrato-lambda.md` (Contrato en rep-or-run-mobile-app: copia, DTOs y tests).

## Why

El `CLAUDE.md` del repo describe `contract/v1/`, `scripts/sync-contract.sh` y los tests de contrato como si existieran, y no existe ninguno. Sin ellos, A-14 (fakes) y A-13 (adaptador del proxy) no tienen sobre qué apoyarse, y nada avisa si la app y `../docs/contract/` dejan de decir lo mismo.

## What Changes

- **`scripts/sync-contract.sh`**: copia `../docs/contract` a `contract/v1/`, bajando los ejemplos un nivel (`examples/` es hermano de `v1/` en la fuente y aquí cuelga de la versión). Con `--check` compara sin escribir y falla si difieren. Sin la fuente, falla diciendo qué falta y dónde se espera.
- **`contract/v1/`**: la copia versionada, 8 schemas y 14 ejemplos (7 de ellos inválidos, con su `expected.json`).
- **`:core:ai`**, módulo nuevo, solo con el paquete `contract`: un DTO por objeto de los schemas, con sus mismos nombres de campo, y el `Json` estricto con el que se leen.
- **Tests de contrato** en `:core:ai`: ida y vuelta de cada ejemplo válido y los siete inválidos.

## No objetivos

- Cambiar el contrato, un schema o `schemaVersion`.
- `:core:network`, el cliente HTTP y el adaptador del proxy (A-12 y A-13, `moved:v1.1`).
- Los ports `PlanGenerator` / `PlanReviewer` y los fakes (A-14).
- Mapear DTOs al modelo de dominio (A-13 / A-14).
- Validar reglas o límites: `PlanValidator` (B-06) y `ProposalValidator`.
- Colgar `--check` de la CI (S-05).
- La copia de la Lambda: su repo aún no tiene dónde ponerla (S-06, `moved:v1.1`).
- Cambiar `CLAUDE.md`: se crea lo que ya describe.

## Capabilities

### New Capabilities

- `contrato-app`: la app guarda una copia del contrato que se puede comprobar contra la fuente, y lee y escribe sus mensajes sin perder nada.

### Modified Capabilities

Ninguna.

## Impact

- **Código:** `scripts/`, `contract/v1/`, `core/ai/`, `settings.gradle.kts`.
- **Módulos:** `:core:ai` es nuevo, pero ya está en `CLAUDE.md` §3 y en el allowlist de `ModuleDependencies.kt`. No depende de ningún otro módulo todavía.
- **Dependencias:** ninguna nueva en el catálogo; `kotlinx-serialization-json` ya estaba (B-04).
- **Contrato:** no se toca. Se copia tal cual.
- **Plan y Figma:** no se tocan.
- **Queda señalado, sin resolver:** dos de los siete ejemplos inválidos incumplen un `minItems`, y eso unos DTOs sin validación no lo pueden rechazar (ver diseño).
