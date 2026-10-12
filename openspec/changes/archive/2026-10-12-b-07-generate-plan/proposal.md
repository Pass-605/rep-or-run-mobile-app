Cubre la tarea **B-07** de `../docs/backlog/B-dominio-datos.md` (GeneratePlanUseCase).

## Why

Los puertos de IA (A-14), el resumen con sus bloqueos (B-05) y `PlanValidator` (B-06) existen, pero nada los une: hoy «Generar plan» no produce ningún plan guardado. Esta change es la orquestación que falta para que C-11 tenga algo real que enseñar.

## What Changes

- `GeneratePlanUseCase` en `:core:domain`: lee el resumen previo, rechaza con sus mismos bloqueos si falta algo, llama a `PlanGenerator`, pasa el resultado por `PlanValidator` y, solo si es válido, lo guarda como `DRAFT` con `inputSnapshotJson`, `model`, `promptVersion`, `schemaVersion` y un id `ROR-AAAA-MM-DD-vN`.
- **Sin reintento de reparación en la app** (criterio 2 corregido el 11/10; plan §11): ante un plan inválido informa y no guarda nada.
- Resultado tipado que separa lo que la persona puede reintentar (red, tiempo, límite, fallo del servicio) de lo definitivo (plan inválido, datos que no sirven).
- El `N` de la versión sale de una consulta nueva al repositorio de planes, con la fecha local de la persona (`Clock` y `TimeZone` inyectados).
- Una fecha de inicio guardada que ya pasó deja de enviarse: la regla pasa a tener una sola definición en el dominio y la leen el formulario, el resumen y la generación (traspaso de C-10).
- Cancelar a mitad no deja nada guardado.

## No objetivos

- La pantalla Generando y sus textos de error: C-11.
- El adaptador del proxy y el `422 PLAN_INVALID` real: A-13. Aquí solo queda escrito dónde cae.
- Activar el plan, calcular `reviewDate` o registrar un `PlanChange`.
- Enviar errores de validación a la Lambda: sería un cambio de contrato (v1.1).
- Decidir el hueco del contrato «plan sin días» que dejó B-06.

## Capabilities

### New Capabilities

- `generacion-plan`: qué pasa cuando la persona pide un plan, qué se guarda y qué se le dice cuando no hay plan.

### Modified Capabilities

- `resumen-previo`: una fecha de inicio ya pasada se trata como «sin fecha».

## Impact

- **Código:** `:core:domain` (caso de uso, port `PlanRepository`, `BuildGenerationSummaryUseCase`, `domainModule`, KDoc de `AiError`), `:core:model` (`PlanId.of`, `BlockPreferences.effectiveStartDate`), `:core:database` y `:core:data` (una consulta de lectura; **sin cambio de esquema ni migración**), `feature:plansetup` (el formulario usa la regla común). Sin dependencias nuevas.
- **Contrato, plan y Figma:** no se tocan.
