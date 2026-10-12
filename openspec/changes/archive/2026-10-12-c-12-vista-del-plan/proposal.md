Cubre la tarea **C-12** de `../docs/backlog/C-diseno-ui.md` (Vista del plan por día).

## Why

Un plan generado se guarda como borrador y llega a un destino que solo dice que existe. Nadie puede leerlo, confirmar sus días ni activarlo, y `activatePlan` y `setDayStatus` (B-04) no tienen caso de uso ni pantalla que los llame. Es la última pantalla del alcance.

## What Changes

- **`:feature:plan`**: la vista del plan, un solo destino. Elegir día, sesión del día (prioridad, cardio, calentamiento, movilidad, vuelta a la calma) y ejercicios con series × rango, RIR, descanso, máquina, carga de ensayo, marca «por lado» y alternativas como «elige una». Aprobar un día y activar el plan. Estado vacío sin plan activo.
- **Se borra `PlanDraftScreen`** con sus cinco textos `plan_draft_*`. `navigateToGeneratedPlan` no cambia.
- **`:core:domain`**: `ObservePlanUseCase`, `ApprovePlanDayUseCase`, `ActivatePlanUseCase` y `HasSavedPlansUseCase`, con tests. La invariante de un solo plan activo sigue en `PlanDao.activate`.
- **`composeApp`**: la app abre por el plan cuando el gimnasio activo ya tiene alguno guardado; el estado vacío y la vista del plan llevan al resumen.
- **`:core:designsystem`**: `RepOrRunScreenFrame` y `RepOrRunScrollContent`, el armazón sin barra, extraído. Lo usan `RepOrRunScreenScaffold`, `InventoryScreen`, `NoticeScreen`, `GeneratingScreen` y la pantalla nueva.
- Skill `reporrun-pantalla-figma`: los cuatro nodos de 07 y 07b en la tabla.

## No objetivos

- Detalle de un ejercicio (C-13), historial y versiones (C-14): `moved:v1.1`.
- Lo que Figma dibuja en 07/07b y la app no tiene: seguimiento de la sesión («Empezar», progreso, «Hecho / En curso»), buscador, notificaciones, saludo, PDF y barra inferior.
- Deshacer la aprobación de un día.
- Mover los textos compartidos a `:core:ui` (decisión en `design.md`).
- Tocar el contrato, Room o la navegación de C-11.

## Capabilities

### New Capabilities

- `vista-del-plan`: leer un plan por día, aprobar sus días, activarlo y saber qué hacer cuando no hay plan activo.

### Modified Capabilities

Ninguna. `pantalla-generando` ya pide que al terminar «se abra el destino de ese plan»: el requisito no cambia, cambia el destino.

## Impact

- **Código:** `feature:plan`, `core:domain`, `core:designsystem`, `composeApp`, y tres pantallas que pasan al armazón (`feature:gym`, `feature:onboarding`, `feature:plan`).
- **Dependencias:** ninguna nueva, ni en el catálogo ni entre módulos.
- **Contrato:** no se toca. **Plan:** no se toca.
- **Backlog:** el primer criterio de C-12 está escrito con el sistema visual v1; se reescribe con lo que dibuja v2 (texto en `design.md`). Es edición de la propia tarjeta.
- **Figma:** no se toca. 07 y 07b v2 **no dibujan** el selector de día, aprobar, activar ni el estado vacío; anotado para C-01.
- **Comportamiento que cambia:** el destino inicial con planes guardados deja de ser la lista de gimnasios.
- **Textos nuevos, pendientes de revisión:** todos los de la pantalla.
