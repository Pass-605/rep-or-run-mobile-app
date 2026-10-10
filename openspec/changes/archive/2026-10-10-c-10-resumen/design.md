## Context

B-05 dejó `BuildGenerationSummaryUseCase`: un `GenerationSummary` reactivo con perfil, gimnasio activo, inventario, preferencias, consentimiento y **todos** los bloqueos. C-09 dejó las preferencias volviendo al inventario al guardar. S-10 dejó el armazón y los controles de sección en `:core:designsystem`.

Diseño: pantalla **05 · Resumen** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `106:937` (*Pantallas · v2 · Claro*) y `115:1344` (*Pantallas · v2 · Oscuro*), leídos el 10/10/2026 antes de componer. El nodo `30:1132` es la 05 de la página vieja y no se usó.

## Goals / Non-Goals

**Goals:**

- Ver lo que se envía y poder corregirlo antes de gastar una generación.
- Que la pantalla no contenga ninguna regla: qué falta lo dice el caso de uso.
- Que editar desde el resumen vuelva al resumen, sin que los formularios sepan de dónde vienen.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### Las tres salidas: se lee la pila

Las salidas de perfil, inventario y preferencias pasan a depender de por dónde se entró. Opciones:

1. *Pasar el origen como argumento de la ruta* (`ProfileGraphRoute(from = SUMMARY)`): cada grafo gana un parámetro y cada formulario, un dato que no es suyo. Q-01 añadiría un tercer valor.
2. *Un flag en el estado o el ViewModel de cada formulario*: lo que el encargo pide evitar.
3. *Dos juegos de destinos* (alta y edición): duplica cada `composable`.
4. *Preguntar a la pila si el resumen está debajo*: **elegida.**

`:feature:plansetup` expone `NavController.popBackToSummary(): Boolean`, que es `popBackStack<SummaryRoute>(inclusive = false)`: si el resumen está en la pila vuelve a él cerrando lo que tenga encima y devuelve `true`; si no está, **no toca la pila** y devuelve `false`. En `RepOrRunNavHost` las tres salidas son la misma frase:

```kotlin
onProfileSaved      = { if (!navController.popBackToSummary()) /* gimnasios, sacando el perfil */ }
onInventoryContinue = { if (!navController.popBackToSummary()) navController.navigateToPlanSetup() }
onPreferencesSaved  = { if (!navController.popBackToSummary()) navController.navigateToSummary() }
```

Coste: el `NavHost` depende de que «el resumen está debajo» signifique «se entró a editar». Hoy es cierto porque el resumen es el final de la cadena y nada lo deja debajo de otra cosa.

**Para Q-01:** la reapertura del perfil es `navController.navigate(ProfileGraphRoute)` desde donde sea. Si Ajustes quiere que guardar vuelva a Ajustes, la misma pregunta con su ruta delante de la del resumen; el perfil no cambia.

### El resumen sustituye a las preferencias en la pila

`navigateToSummary()` hace `popUpTo<PreferencesRoute> { inclusive = true }`. El formulario lee lo guardado **una vez**: si se quedara debajo, «atrás» desde el resumen lo enseñaría con lo que tenía antes de editarlo desde el resumen, y con la acción deshabilitada (`isSaving` no vuelve a `false`). Atrás desde el resumen lleva al inventario, que sí observa.

Editar las preferencias desde el resumen las apila encima (`navigateToPreferences()`), con un ViewModel nuevo que lee lo guardado.

### El perfil y el gimnasio, abiertos desde el resumen

- **Perfil:** `navigate(ProfileGraphRoute)`. Su grafo ya precarga el perfil guardado. El paso 1 no tenía botón de volver porque en el alta es lo primero de la pila; `profileGraph` recibe `canLeave: () -> Boolean`, que el `NavHost` contesta con `previousBackStackEntry != null`. No es el origen: es si hay a dónde volver.
- **Gimnasio:** con gimnasio activo, a su inventario (el número de máquinas es lo que cuenta el resumen); sin ninguno, a la lista, y de ahí «Continuar» → inventario → «Continuar» → resumen. El grafo de gimnasios puede quedar dos veces en la pila (el del alta y este); `getBackStackEntry(GymGraphRoute)` da el de arriba.

### El motivo del botón deshabilitado

`blockers` trae todo lo que falta. Se enseña **una tarjeta con una cabecera y una línea por bloqueo**, en el orden del caso de uso (perfil, gimnasio, preferencias, consentimiento), que es el del recorrido:

```text
Para generar el plan falta:
· Añadir máquinas: tu gimnasio tiene 2 y hacen falta 5
· Guardar tu semana, con al menos un día
```

Con uno, dos o cinco se lee igual; no hay frase que componer con comas e «y». Alternativa descartada: enseñar solo el primero, que obliga a arreglar uno para descubrir el siguiente.

**Dónde:** arriba, bajo el titular, no junto al botón. El armazón fija la acción al pie y el contenido hace scroll: un motivo al final del contenido puede quedar fuera de la vista. El titular cambia con él («Falta algo antes de generar»), y cada fila dice además qué le falta («Sin completar», «Sin gimnasio activo», «Sin guardar»).

La tarjeta es `RepOrRunSectionError`: `accentText` dentro de una tarjeta. «≥ 1 día» no es un bloqueo aparte: va en `MissingPreferences`, y su línea lo dice.

Los números del bloqueo de máquinas (`current`, `required`) vienen del caso de uso; la pantalla no compara nada.

### Estado

`SummaryUiState(summary: GenerationSummary?)`: el del caso de uso, sin traducir. `null` mientras el almacenamiento no contesta, y entonces no se pinta nada. A diferencia de los formularios **se observa**: no hay nada a medio escribir.

Las filas de editar son navegación pura y van como lambdas; «Generar plan» pasa por el ViewModel (`GenerateClicked` → `GenerationRequested`), que vuelve a mirar `canGenerate`.

### Minutos por sesión: 60 por defecto

Decisión de producto del 08/10. `PreferencesViewModel` propone 60 cuando no hay preferencias guardadas; unas guardadas mandan. El resumen los enseña en una de las tres cifras, en acento, como en Figma. La constante vive en el ViewModel: no es un límite del contrato.

### Generando

`composeApp` añade `GeneratingPlaceholderRoute`, un destino mínimo que dice que la generación aún no está y vuelve al resumen, como el destino provisional de C-05. Se borra con C-11.

### Módulos y tokens

`feature:plansetup`, `feature:profile`, `core:designsystem` y `composeApp`. Ninguna dependencia sale de §3; no hay ADR.

`RepOrRunStatCard` es un patrón nuevo (cifra + etiqueta) y va a `:core:designsystem`: `surface`, cifra en `extendedTypography.numberMedium` (`onSurface`, o `accentText` si se destaca), unidad en `bodySmall` y etiqueta en `labelSmall`, las dos en `onSurfaceVariant`.

El resto sale de componentes existentes: `RepOrRunScreenScaffold`, `RepOrRunCard` + `RepOrRunListRow` (una fila por tarjeta, separadas con `CardListSpacing`), `RepOrRunSectionError`; titular en `headlineMedium`; overline de «Qué se envía» en `extendedTypography.overline` con `accentText`, dentro de tarjeta.

La experiencia y el objetivo ya tienen texto en `feature:profile`, pero un feature no puede usar otro: `plansetup` tiene los suyos, en minúscula y para ir dentro de una frase.

## Lectura de Figma (10/10/2026)

**Contraste**, medido en los dos temas: `text/primary` sobre `bg/canvas` 13,26 y 15,00; `text/primary` sobre `bg/surface` 15,00 y 10,30; `text/secondary` sobre `bg/surface` 5,70 y 5,08; `text/accent` sobre `bg/surface` 4,61 y 4,54 (la cifra de minutos y el overline, los dos dentro de tarjeta); `text/on-inverse` sobre `bg/inverse` 15,00. No usa `text/muted`. **No hay séptima corrección.** El chevron (`outline` sobre `surface`, 2,86 en claro) es decorativo y ya estaba en `RepOrRunListRow`.

**Lo que la app tiene y Figma no dibuja:** el estado con algo incompleto (titular, tarjeta de motivos, filas sin dato, cifras con «–») y la fecha de inicio ausente. Para C-01.

**Lo que Figma dibuja y no se copia:**

- El overline «Paso 4 de 4», como en la 04.
- El círculo del icono de fila a 40 con icono de 18: `RepOrRunListRow` lo tiene a 38 y 17 (pantalla 03).
- El texto de «Qué se envía»: Figma dice «sin nombre», y el alias, si se puso, sí viaja con el perfil. Va «la app no pide tu nombre real», como el aviso de C-05.

## Risks / Trade-offs

- [`MissingConsent` se enseña pero no se puede arreglar desde el resumen] → Hoy no se llega aquí sin consentimiento: la app arranca en la bienvenida mientras falte. Retirarlo será cosa de Ajustes (Q-01), que tendrá que ofrecer cómo volver a darlo.
- [Una fecha de inicio guardada que ya pasó se enseña tal cual] → El formulario no la precarga, pero sigue guardada hasta que se guarde de nuevo. A decidir con C-11/B-07 qué se envía.
- [El destino inicial no cambia] → Con todo completo la app sigue abriéndose en los gimnasios; al resumen se llega con tres «Continuar». Cuando exista el plan, el inicio será suyo.
