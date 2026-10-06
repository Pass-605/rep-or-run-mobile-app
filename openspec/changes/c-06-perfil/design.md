## Context

B-05 dejó `SaveProfileUseCase` y `ObserveProfileUseCase` con las reglas en `ProfileRules` (`internal`). C-05 dejó el modelo de pantalla (`feature/onboarding`) y un destino provisional que esta tarea borra.

Diseño: pantalla **02 · Perfil** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `104:870` (v2 · Claro) y `115:1131` (v2 · Oscuro). El diseño la dibuja en **una** pantalla; el plan §3 y el backlog piden dos pasos. No trae cadera, músculos prioritarios ni preferencias, que el perfil sí tiene.

## Goals / Non-Goals

**Goals:**

- Que el perfil se pueda crear y editar, con los errores que dicta el caso de uso.

**Non-Goals:**

- Los de la propuesta.

## Decisions

**Módulos.** Nuevo: `:feature:profile` (`reporrun.feature`). Tocados: `composeApp`, `core:domain`, `core:designsystem`, `androidApp`. `composeApp` → `feature:profile`; el feature, las cuatro de §3. Ninguna dependencia sale de §3, así que no hay ADR.

**Quién valida.** La pantalla no compara números. Opciones para avanzar del paso 1:

1. *Hacer públicas las reglas de `ProfileRules`*: el ViewModel acabaría repitiendo qué es obligatorio.
2. *Validar solo al guardar*: los errores del paso 1 saldrían estando en el 2.
3. *`ValidateProfileUseCase`*: **elegida.** Devuelve los mismos errores que guardar, sin guardar. El ViewModel se queda con los de los campos del paso en curso. Sirve también para el tope de prioridades: al marcar una, pregunta; si el caso de uso dice `TooMany`, no la marca y enseña ese error.

**Dos pasos: dos destinos, un ViewModel.** Opciones:

1. *Un destino con el paso en el estado y `BackHandler`*: en Compose 1.12 `BackHandler` está deprecado y su sustituto (`navigationevent`) es una dependencia nueva.
2. *Dos destinos con un ViewModel cada uno*: lo tecleado en el paso 1 habría que pasarlo por la ruta o guardarlo a medias.
3. *Dos destinos del grafo que comparten ViewModel*: **elegida.** El «atrás» del sistema funciona solo y el estado sobrevive al ir y volver. `profileGraph` recibe `graphOwner: () -> ViewModelStoreOwner`, que `composeApp` resuelve con la entrada de `ProfileGraphRoute`: el feature sigue sin ver el `NavController`.

Los dos pasos recogen los efectos con la misma función (`ProfileEffects`): durante la transición cualquiera de los dos puede ser el colector.

**Estado.** Los números van como texto. El ViewModel limpia lo tecleado (dígitos; una coma y un decimal; el punto se convierte) y lo convierte a `ProfileInput`. Un campo vacío es «sin dato». Los errores son `Map<ProfileField, FieldIssue>`; la frase la pone la pantalla con el límite que trae el `FieldIssue`. El error de un campo se borra al tocarlo.

**Precarga.** El perfil se lee una vez en `init` (`first()`), no se observa: observarlo pisaría lo que se está escribiendo. Mientras carga no se pinta el formulario.

**Después de guardar.** No hay destino siguiente hasta C-07. `composeApp` vuelve a entrar al grafo del perfil, que se abre con lo guardado. Es lo que C-07 cambia.

**Tarjeta del objetivo.** `Goal.explanation` es un `when` exhaustivo a recursos de `strings.xml`. El texto de recomposición es el de Figma; los otros tres son nuevos, con la regla 10 del plan §4 presente (no se promete hipertrofia medible en 14 días). La nota de alimentación es un recurso aparte que la tarjeta pinta siempre.

**Tokens.**

| Figma | En código |
|---|---|
| `bg/canvas`, `bg/surface`, `bg/inverse` | `background`, `surface` (`RepOrRunCard`, `RepOrRunField`), `inverseSurface` (`RepOrRunButton`, chip elegido) |
| `text/primary`, `text/secondary`, `text/on-inverse` | `onBackground`/`onSurface`, `onSurfaceVariant`, `inverseOnSurface` |
| `bg/accent` (borde de 1,5 y punto de la tarjeta) | `primary`; no es texto |
| `text/accent` (overline de la tarjeta) | `extendedColors.accentText`, dentro de tarjeta |
| `text/muted` al 25 % (separador) | `RepOrRunDivider` (`outline` al 20 %) |

**Desviaciones de Figma.**

1. *Nota de alimentación en `text/muted`* (claro, 2,86:1): `onSurfaceVariant`. Es la primera de las «Correcciones medidas».
2. *Errores de los grupos de chips*: no están dibujados. Van en una tarjeta, porque `accentText` sobre el lienzo no cumple en claro.
3. *Stepper de dos nodos* en una tarjeta para indicar el paso: no está en el diseño, que es de una sola pantalla.
4. *Sin botón de volver en el paso 1*: es el primer destino.
5. *«Limitaciones» y «Preferencias»* usan `RepOrRunField` con la etiqueta dentro, no un título encima.

Las medidas propias de la tarjeta entran en `RepOrRunDimens`.
