## Context

`C-11` deja un borrador guardado y navega con `navigateToGeneratedPlan(planId)` a `PlanDraftScreen`, que no lee el plan. `PlanRepository` (B-04) ya tiene `observePlan`, `observeActivePlan`, `activatePlan` y `setDayStatus`; solo faltan casos de uso y pantalla.

Figma: **07 · Plan (v2)** `99:835` / `115:937` y **07b · Detalle del día (v2)** `101:831` / `115:1023` (claro / oscuro). `30:1223` y `30:1356` son de la página vieja.

## Goals / Non-Goals

**Goals:** leer el plan por día, aprobar días, activar el plan, estado vacío, y cerrar las cinco decisiones que la tarjeta deja abiertas.

**Non-Goals:** los del proposal.

## Decisions

### 1. Qué se toma de Figma v2 y el criterio reescrito

07 y 07b dibujan **la sesión de hoy en curso**, no un plan por aprobar. De lo que dibujan, la app tiene datos para esto:

| De Figma | Aquí |
|---|---|
| Bloque de énfasis de 07 (overline, sesión, «N ejercicios · N series») | El héroe del día elegido. Sin «75 min»: el plan no trae duración. Sin los botones «Empezar» / «Ver plan» |
| Tarjeta de cabecera de 07b (icono, línea secundaria, título, chip) | La cabecera del plan: versión, enfoque y estado |
| «Detalles del día» de 07b (filas etiqueta → valor) | La sesión. Etiqueta **encima** del valor: calentamiento y movilidad son frases, no caben a la derecha |
| Tarjeta de ejercicio de 07 (círculo con número, nombre, «3 × 6-10 · RIR 2») y «ensayo 20-35 kg» de 07b | La lista de ejercicios. El círculo siempre neutro: no hay «hecho» |
| Chip de estado | Estado del plan y del día |

No se copia: saludo, notificaciones, buscador, ajustes, stepper de progreso, «Hecho / En curso / Pendiente», «Empezar sesión», «Plan en PDF», «Aviso» y la barra inferior. Nada de eso existe en el alcance.

**Lo que Figma no dibuja y la tarjeta pide** se compone con patrones ya implementados: el selector de día son `RepOrRunChip` (chip de filtro, uno por día con sesión), aprobar y activar son la acción del pie (`inverseSurface`), y el estado vacío es título + texto + acción.

Criterio 1 de la tarjeta, reescrito:

> Un chip por día con sesión (el elegido en `inverseSurface`; al abrir, hoy si tiene sesión). Bloque de énfasis con el día, «hoy» si lo es, la sesión y «N ejercicios · N series». Ejercicios en tarjetas `surface` con el número de orden en un círculo, como en la 07 v2. Sin tonos alternos: v2 es plano.

Correcciones de Figma aplicadas (ya medidas en la skill): el overline y la línea de cifras del héroe van en `text/muted` (`#979797`) en claro → `inverseOnSurface`. El «Aviso» en acento no se usa.

### 2. Los textos compartidos se quedan por feature

Opciones:

- **A. Moverlos a `:core:ui`.** Para mapear `DayOfWeek`, `Experience` o `Goal` a un texto, `:core:ui` tiene que depender de `:core:model`: una arista nueva entre módulos (ADR, `CLAUDE.md` §3 y `ModuleDependencyRules`), más Compose Resources públicos en `:core:ui` y migrar los textos de cuatro features con sus tests. Media jornada, fuera de las 3 h.
- **B. El dominio devuelve claves y cada feature mapea.** Es lo que hay.

**Se elige B, sin cambios.** Lo que A amortiza son pantallas futuras, y después de C-12 no queda ninguna en el alcance. Lo que esta pantalla duplica es poco y está contado: los siete nombres de día y sus siete abreviaturas, que ya están en `plansetup`. **No formatea kilos** —la carga de ensayo es texto del plan—, así que no hay tercer caso de los formateadores.

Se reabre con la primera pantalla de v1.1 que necesite una tercera copia de algo (C-13 o C-14).

### 3. Qué plan se enseña: un destino, id opcional

`PlanRoute(planId: String? = null)`:

- Con id (tras generar) → ese plan.
- Sin id (destino inicial) → el plan activo del gimnasio activo; si no hay, estado vacío.

Dos destinos serían la misma pantalla y el mismo ViewModel dos veces. El ViewModel lee el id de `SavedStateHandle`, como ya recibe el suyo `GeneratingViewModel`.

**Destino inicial.** Hoy nada abre el plan sin id. `AppViewModel` añade `StartDestination.Plan` cuando el gimnasio activo tiene **algún plan guardado**, en cualquier estado. Con «solo si hay activo» el estado vacío sería inalcanzable. Consecuencia: con borradores y ninguno activo, la app abre en el estado vacío.

**Salida.** Abierta como inicio no tiene atrás, y sin barra inferior la persona se quedaría sin forma de preparar otro plan: el estado vacío y el final de la vista ofrecen ir al resumen. Tras generar, atrás ya lleva al resumen y no se repite el enlace.

**Hueco que queda:** un borrador sin activar no se puede reabrir tras cerrar la app (solo se llega a él al generarlo). Lo cierra el historial, C-14.

### 4. Casos de uso y transiciones

En `:core:domain/usecase/PlanUseCases.kt`:

- `ObservePlanUseCase`: el plan por id, o el activo del gimnasio activo, **con los aparatos de su gimnasio** (`PlanDetails`): el ejercicio guarda `equipmentId` y la pantalla enseña el nombre.
- `ApprovePlanDayUseCase`: `PROPOSED` → `APPROVED`, solo en un plan `DRAFT`.
- `ActivatePlanUseCase`: `DRAFT` o `ARCHIVED` → `ACTIVE`. Solo orquesta: archivar el activo anterior lo hace `PlanDao.activate` en su transacción.
- `HasSavedPlansUseCase`: para el destino inicial.

**Aprobar un día no es reversible.** Es la confirmación de la persona (plan §4.11); el repositorio admite volver a `PROPOSED`, pero ningún caso de uso lo expone. Deshacer una confirmación no tiene consecuencia que evitar: el plan no cambia al aprobar.

**Activar exige todos los días aprobados.** Regla 5: nada cambia sin aprobación, y activar es lo que cambia el plan vigente. Con días pendientes el caso de uso devuelve cuántos faltan y no activa. Un plan archivado ya fue activo, así que los tiene todos: C-14 podrá reactivarlo con el mismo caso de uso.

### 5. Estado y pie

`PlanUiState` guarda `PlanDetails?`, el día elegido y `today` (del `Clock` y `TimeZone` inyectados, una vez). La acción del pie se deriva:

| Plan | Día elegido | Pie |
|---|---|---|
| `DRAFT` | `PROPOSED` | «Aprobar este día» |
| `DRAFT` | `APPROVED`, faltan otros | «Activar plan» deshabilitado + qué días faltan, encima |
| `DRAFT` / `ARCHIVED` | todos aprobados | «Activar plan» |
| `ACTIVE` | — | sin pie |

Sin efectos: la pantalla no navega por nada que decida el ViewModel. Un resultado que no sea el esperado (día ya aprobado, plan borrado) es una carrera; el estado observado ya lo refleja.

### 6. El armazón sin barra, extraído

La pantalla es «contenido en scroll + pie fijo» con un pie que cambia y a veces no está: no cabe en `RepOrRunScreenScaffold`, que exige barra y una acción. Es la quinta, así que se extrae en `:core:designsystem`:

- `RepOrRunScreenFrame(footer?) { cuerpo }`: márgenes seguros, margen lateral y el pie fijo.
- `ColumnScope.RepOrRunScrollContent(spacing, horizontalAlignment)`: el cuerpo con scroll.

Dos piezas y no una porque `InventoryScreen` tiene cabecera fija y `LazyColumn`: usa el marco y no el cuerpo. `RepOrRunScreenScaffold` pasa a ser marco + cuerpo + barra + botón, sin cambiar su firma.

### 7. Alternativas

Van en una línea dentro de la tarjeta de su ejercicio, «O elige una: A · B», sin número, sin series y sin tarjeta propia. El recuento del héroe son las series efectivas del modelo (`PlanDay.effectiveSets`), que no las suma.

## Tokens

`background`, `surface`, `inverseSurface` / `inverseOnSurface`, `onSurface`, `onSurfaceVariant`, `primary` / `onPrimary` (solo el chip «Activo»). Radios 24 y 28 de `MaterialTheme.shapes`. Medida nueva en `RepOrRunDimens`: `ExerciseIndexSize` (40, de la 07).

## Contraste medido

Todos los pares de la pantalla, con la fórmula de la skill. Umbral 4,5:1.

| Par | Claro | Oscuro |
|---|---|---|
| Título y cabeceras sobre el fondo | 13,26 | 15,00 |
| Secundario sobre el fondo («N de M aprobados», el motivo del pie, el estado vacío) | 5,04 | 7,39 |
| Texto en tarjeta | 15,00 | 10,30 |
| Secundario en tarjeta | 5,70 | 5,08 |
| Héroe, botón principal y chip de día elegido (`inverseOnSurface` sobre `inverseSurface`) | 15,00 | 15,00 |
| Chip neutro y número en el círculo (`onSurfaceVariant` sobre `background`) | 5,04 | 7,39 |
| Chip de día sin elegir (`onSurfaceVariant` sobre `surface`) | 5,70 | 5,08 |
| Chip «Activo» (`onPrimary` sobre `primary`) | 4,51 | 4,51 |

Reglas duras: la acción principal es `inverseSurface`; el naranja solo es el relleno del chip «Activo», con `#232323` encima; `#979797` solo en separadores; no hay superficies constantes en esta pantalla; sin elevación ni sombras. **Sin medir:** «Activar plan» deshabilitado (38 %), como todo botón deshabilitado del design system; por eso el motivo va encima en texto normal.

## Lo que la verificación encontró

- **El fake no da 5 días.** La tarjeta pide probar con un plan de 5 días; `FakePlanGenerator` adapta los 3 del ejemplo del contrato y no devuelve más. Verlo pide otro ejemplo o el adaptador real (A-13).
- **Los commits `feat(plan)` y `feat(app)` van juntos:** el primero cambia la firma de `planGraph` y `composeApp` no compila hasta el segundo.

## Risks / Trade-offs

- **Cambia el arranque** con planes guardados → un test de `AppViewModel` por rama; sin planes sigue igual.
- **El armazón toca tres pantallas ya verificadas** → recorrido de las tres en el emulador.
- **«Activar plan» deshabilitado** lleva texto al 38 % → el motivo va encima, en texto normal.
- **Borrador inalcanzable tras reiniciar** → anotado para C-14.

## Open Questions

Ninguna que bloquee. Para revisar con quien decide producto: si sin plan activo debería abrirse el último borrador en vez del estado vacío.
