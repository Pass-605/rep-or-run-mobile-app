Cubre la tarea **C-11** de `../docs/backlog/C-diseno-ui.md` (Pantalla Generando).

## Why

`GeneratePlanUseCase` (B-07) ya genera, valida y guarda, pero ninguna pantalla lo llama: «Generar plan» lleva a un destino provisional que dice que la generación no está. Falta la espera, con forma de cancelarla, y decir a la persona qué pasó cuando no hay plan y qué puede hacer.

## What Changes

- **`:feature:plan`**, módulo nuevo (el que alojará C-12): la pantalla 06 de Figma. Espera a pantalla completa con progreso indeterminado, mensajes que rotan y Cancelar, que cancela la generación de verdad.
- **Errores, cuatro casos y dos salidas.** Sin servicio: «Reintentar» (con los segundos a esperar si el servicio los dio). Plan inválido: «Generar otra vez», porque es otra llamada y no la repetición de la fallida. Datos que no sirven: «Revisar mis datos», al resumen. Con bloqueos: al resumen.
- **Tope de dos.** Tras dos planes inválidos seguidos deja de ofrecer generar y manda al resumen con los problemas a la vista.
- **Raíz**: se borra `GeneratingPlaceholderScreen`, su ruta y sus tres textos; `onGenerate` abre el grafo de `:feature:plan`.
- **Destino del plan generado**: la vista del plan es C-12 y no existe. Al terminar se abre un destino mínimo de `:feature:plan` que dice que el borrador está guardado y con qué versión. **C-12 lo sustituye.**
- **`:core:domain`**: KDoc de `AiError.InvalidResponse` y `GeneratePlanResult.InvalidPlan` corregidos (decían que no se arregla reintentando).
- **`:core:designsystem`**: `RepOrRunButtonStyle.Tonal` (el «Cancelar» de Figma, en `surface`) y las medidas del anillo.
- Skill `reporrun-pantalla-figma`: los dos nodos de la 06 en la tabla.

## No objetivos

- La vista del plan por día, aprobar días o activar el plan (C-12).
- Cambiar qué es reintentable: lo dice el tipo de `GeneratePlanResult`.
- Reintentos automáticos, ni reparar un plan en la app.
- Cuenta atrás que bloquee «Reintentar» tras un límite de peticiones: se dice cuánto esperar.
- El adaptador real del proxy (A-13) y su conmutador.
- Arreglar Figma (C-01).

## Capabilities

### New Capabilities

- `pantalla-generando`: esperar la generación, cancelarla y saber qué hacer cuando no hay plan.

### Modified Capabilities

- `generacion-plan`: un plan inválido deja de contarse entre lo que no admite otro intento. Sigue sin repetirse solo.

## Impact

- **Código:** `feature:plan` (nuevo), `composeApp`, `core:designsystem`, `core:domain` (solo KDoc y el nombre de un test).
- **Dependencias:** ninguna nueva en el catálogo. `composeApp` → `feature:plan`, como con cada feature.
- **Contrato:** no se toca.
- **Plan:** no se toca. **Figma:** no se toca; la 06 dibuja un porcentaje y una lista de tres pasos que la app no puede conocer, y no dibuja ningún error: anotado para C-01.
- **Textos que necesitan revisión antes de publicar:** todos los de la pantalla son nuevos (mensajes de espera, los errores, el destino del borrador).
