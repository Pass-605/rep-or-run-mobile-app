Cubre la tarea **C-09** de `../docs/backlog/C-diseno-ui.md` (Preferencias del bloque).

## Why

Tras el inventario no hay a dónde seguir: su pie dice «Listo» y vuelve a los gimnasios. `SavePreferencesUseCase` (B-05) existe, pero ninguna pantalla lo llama, y sin preferencias guardadas `BuildGenerationSummaryUseCase` no deja generar.

## What Changes

- **`:feature:plansetup`, módulo nuevo** (el que nombran el plan y el backlog): las preferencias del bloque (pantalla 04 de Figma) —días disponibles, minutos por sesión, duración del bloque, fecha de inicio opcional, cardio preferido y ejercicios a evitar—, precargadas si ya hay unas guardadas.
- **Inventario**: el pie pasa de «Listo» a «Continuar» y lleva a las preferencias.
- **`:core:domain`**: `ObservePreferencesUseCase` y `ObserveCardioOptionsUseCase`.
- **Los modos de cardio salen del inventario**, no de una lista escrita ni de un enum (ver diseño).
- **Raíz**: `planSetupGraph()` y `planSetupModule` conectados.

## No objetivos

- El resumen previo a generar (C-10): al guardar se vuelve al inventario.
- Editar la duración del bloque: v1 solo admite 14 días (contrato y plan §4).
- Un enum de modos de cardio en el contrato.
- Un selector de fecha de calendario: el design system no tiene uno.
- Arreglar Figma (C-01).

## Capabilities

### New Capabilities

- `preferencias-bloque`: decir con qué se pide el siguiente bloque y guardarlo.

### Modified Capabilities

Ninguna archivada. Cambia de hecho `inventario` (`c-08-inventario`, sin archivar): su pie lleva al paso siguiente.

## Impact

- **Código:** `feature:plansetup` (nuevo), `feature:gym`, `core:domain`, `composeApp`, `settings.gradle.kts`.
- **Dependencias:** ninguna nueva en el catálogo.
- **Contrato:** no se toca. Se respetan sus límites (20-180 minutos, 14 días, 6 modos de cardio de 40 caracteres, 400 caracteres de ejercicios a evitar), todos aplicados por el caso de uso.
- **Plan:** no se toca.
- **Textos que necesitan revisión antes de publicar:** los que no vengan de la pantalla 04, y en particular el aviso de que el texto libre se envía a OpenAI.
