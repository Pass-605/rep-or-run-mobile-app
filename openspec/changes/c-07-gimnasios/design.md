## Context

B-05 dejó `CreateGymUseCase` (el primer gimnasio queda activo) y `SetActiveGymUseCase` (comprueba que existe), y avisó de que renombrar y borrar no tenían caso de uso. C-06 dejó el modelo de formulario (`feature/profile`) y dos herencias: `onProfileSaved` no tiene destino y `AppViewModel` solo mira el consentimiento.

Diseño: pantalla **03 · Mi gimnasio** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `104:938` (v2 · Claro) y `115:1216` (v2 · Oscuro). Es el **inventario de un gimnasio** (C-08), no una lista de gimnasios: Figma no dibuja la pantalla de esta tarea. La 13 · Ajustes tiene una fila «Gimnasios · 1 activo» que lleva a ella, sin destino dibujado.

## Goals / Non-Goals

**Goals:**

- Que se pueda crear el primer gimnasio, tener varios, renombrarlos y elegir el activo.
- Que el flujo siga después del perfil y que la app no vuelva a abrirse en él.

**Non-Goals:**

- Los de la propuesta.

## Decisions

**Renombrar se añade; borrar se recorta.** La nota de B-05 daba dos salidas para las dos operaciones. No tienen el mismo coste:

1. *Renombrar*: está en el criterio de aceptación, es una lectura y un `saveGym`, y no pierde nada. **Se añade** como `RenameGymUseCase` en `:core:domain`, con la misma regla de nombre que crear (una función privada compartida) y `GymNotFound` si el gimnasio ya no está. El ViewModel no compara longitudes.
2. *Borrar*: no está en el criterio. `deleteGym` arrastra en cascada inventario, planes e historial; es borrar datos de la persona, que `../CLAUDE.md` §9 manda preguntar antes. Además pide dos cosas que no existen: una confirmación (el design system no tiene diálogo) y decidir qué pasa al borrar el activo. **Se recorta** y se dice en el cierre.

`saveGym` es un `@Upsert` de Room: actualiza la fila, no la reemplaza, así que renombrar no dispara el `CASCADE` del inventario.

**`ObserveGymsUseCase`.** La pantalla necesita la lista. El feature podría leer `GymRepository` (está en `:core:domain`), pero el patrón del repo es que un ViewModel solo reciba casos de uso (`ObserveProfileUseCase`, `ObserveConsentUseCase`).

**Módulos.** Nuevo: `:feature:gym` (`reporrun.feature`). Tocados: `composeApp`, `core:domain`, `core:designsystem`, `feature:profile`. Ninguna dependencia sale de §3, así que no hay ADR.

**Lista y formulario: dos destinos, un ViewModel.** El mismo esquema que el perfil y por el mismo motivo: sin `BackHandler` (deprecado en Compose 1.12), el «atrás» del sistema solo cierra el formulario si es un destino. Opciones para el formulario:

1. *En la misma pantalla, debajo de la lista*: no hay forma limpia de cerrarlo con atrás.
2. *Destino con su ViewModel y el id en la ruta*: dos ViewModels para un nombre.
3. *Destino del grafo que comparte ViewModel*: **elegida.** `gymGraph(graphOwner = …)`. El formulario se abre siempre desde cero (`GymEditorState` nuevo), con el nombre actual si es renombrar.

**La lista se observa; el perfil no.** Aquí no hay nada a medio escribir que pisar: lo que se pinta es lo guardado, y crear, renombrar o activar no tocan la lista a mano.

**Activar.** Tocar un gimnasio que no está activo lo activa. El activo no es pulsable. Sin confirmación: es reversible con otro toque y no cambia ningún plan (regla 5: los planes cuelgan de su gimnasio; cambiar de activo no los edita).

**Destino inicial.** `AppViewModel` lee una vez consentimiento y perfil: `Onboarding` → `Profile` → `Gyms`. No se añade un cuarto escalón («hay gimnasio»): la lista con el estado vacío ya es el sitio correcto con cero gimnasios. Cuando exista el plan, el inicio será suyo.

**Después del perfil.** `onProfileSaved` navega a `GymGraphRoute` sacando el perfil de la pila: su ViewModel se queda con `isSaving`, y volver atrás lo enseñaría bloqueado. Editar el perfil después es de Ajustes.

**Después del gimnasio.** No hay paso siguiente hasta C-08. La lista no tiene «Continuar»: su única acción de pie es crear.

**Tokens.** Sin pantalla en Figma, se compone con piezas ya hechas y la fila de la 03.

| Figma (pantalla 03) | En código |
|---|---|
| `bg/canvas`, `bg/surface`, `bg/inverse` | `background`, `surface` (`RepOrRunCard`, `RepOrRunField`), `inverseSurface` (`RepOrRunButton`, marca elegida) |
| `text/primary`, `text/secondary`, `text/on-inverse` | `onBackground`/`onSurface`, `onSurfaceVariant`, `inverseOnSurface` |
| `icon/primary`, `icon/on-inverse` | `extendedColors.iconPrimary` (botón de volver), `inverseOnSurface` (check, icono del botón) |
| Círculo sin marcar: `bg/canvas` sobre `bg/surface` | `background` con borde `onSurfaceVariant` (ver desviación 1) |

**Componentes nuevos en `:core:designsystem`.** `RepOrRunCheckMark(checked)`: el círculo de 28 con check de la 03; lo usará también el inventario. `RepOrRunTopBar`: la barra que el perfil tenía privada; con dos features usándola ya es un patrón.

**Desviaciones de Figma.**

1. *Círculo de selección sin marcar* (`bg/canvas` sobre `bg/surface`): 1,13:1 en claro y 1,46:1 en oscuro; una marca de estado pide 3:1. Se le añade un borde `onSurfaceVariant` (5,70 y 5,08). `outline` no vale: 2,86 en claro. **Es la sexta corrección medida** y se añade a la skill.
2. *Toda la pantalla*: no está dibujada. Una tarjeta por gimnasio con dos filas (`RepOrRunListRow`): nombre + estado + marca, y «Renombrar» con chevron.
3. *Estado vacío*: tarjeta con título y dos párrafos; la acción es el botón del pie.

No hay iconos nuevos: «Renombrar» es una fila con texto porque `RepOrRunIcons` no tiene lápiz y exportarlo sería tocar Figma.
