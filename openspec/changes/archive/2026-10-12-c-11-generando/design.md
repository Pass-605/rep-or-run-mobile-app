## Context

B-07 dejó `GeneratePlanUseCase`: `suspend`, cancelable, y con un `GeneratePlanResult` que dice en el tipo qué puede hacer la persona después. C-10 dejó el resumen y un destino provisional en `composeApp` tras «Generar plan». A-14 dejó el fake con 3 s de espera y un modo error, los dos en `aiModule` (`FakeAiConfig`).

Diseño: pantalla **06 · Generando** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `106:1004` (*Pantallas · v2 · Claro*) y `115:1395` (*Pantallas · v2 · Oscuro*), leídos el 11/10/2026 antes de componer. El nodo `30:1198` es la 06 de la página vieja y no se usó.

## Goals / Non-Goals

**Goals:**

- Que la espera tenga algo que mirar y se pueda cancelar de verdad.
- Que la pantalla no decida qué es reintentable: lo lee del tipo.
- Que nadie gaste una generación creyendo que repite una petición fallida, ni entre en un bucle de planes inválidos.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### Módulo `:feature:plan`, con las dependencias de cualquier feature

`core:domain`, `core:model`, `core:designsystem` y `core:ui`, que pone `reporrun.feature`. Ninguna dependencia nueva entre módulos salvo `composeApp` → `feature:plan`, que es la de la raíz con cada feature. El fake de `:core:ai` no es alcanzable desde aquí ni en tests (`checkModuleDependencies`): el test del ViewModel usa un generador escrito a mano que espera y contesta como él.

### La generación empieza con el ViewModel y vive en su scope

`GeneratingViewModel` llama al caso de uso en `init`, dentro de `viewModelScope`. Opciones:

1. *Lanzarla desde un `LaunchedEffect` de la pantalla*: un giro la relanza, y cada relanzamiento es una llamada.
2. *Un evento «Start» que envía la pantalla*: lo mismo, con un paso más.
3. *En `init`*: **elegida.** Una entrada a la pantalla es una generación, sobrevive al giro y se cancela sola al salir.

Coste: tras una muerte del proceso con la espera en pantalla, al restaurarse el ViewModel se crea de nuevo y **pide otro plan sin que nadie lo toque**. Con el fake no cuesta nada; con el proxy real (A-13) es una llamada. Queda anotado para A-13: si importa, la ruta deja de restaurarse o el ViewModel guarda en `SavedStateHandle` que ya pidió.

### Cancelar es cancelar el `Job`

`CloseClicked` cancela el `Job` de la generación y emite `BackToSummary`. Atrás del sistema saca el destino de la pila, que limpia el ViewModel y cancela su scope: mismo resultado sin `BackHandler`. El caso de uso ya comprueba `ensureActive()` antes de guardar.

Un flag `finished` cierra las dos carreras del borde: un resultado que llega con la salida ya pedida no navega, y un «cerrar» que llega con el plan ya guardado no devuelve al resumen. La ventana que B-07 documenta —la transacción ya confirmada cuando se cancela— sigue existiendo y no se tapa: queda un borrador que nadie abrió.

### Estado, eventos y efectos

```kotlin
data class GeneratingUiState(val failure: GenerationFailure? = null)   // null = esperando

sealed interface GenerationFailure {
    data class Unavailable(val reason: GenerationUnavailable)
    data class InvalidPlan(val problems: List<String>, val canGenerateAgain: Boolean)
    data class InvalidInput(val problems: List<String>)
}
```

Dos eventos, `RetryClicked` y `CloseClicked`, y dos efectos, `PlanGenerated(planId)` y `BackToSummary`. «Reintentar» y «Generar otra vez» son el mismo evento porque hacen lo mismo —pedir un plan—; lo que cambia es la palabra, y esa la pone `GeneratingLabels` con un `when` exhaustivo sobre el fallo. El ViewModel comprueba por su cuenta si se puede pedir de nuevo: el tope no depende de que la pantalla esconda el botón.

`Blocked` no tiene estado propio: emite `BackToSummary`. El resumen ya enseña los bloqueos, y duplicar sus textos en otro feature sería la tercera familia de textos repetidos.

### El tope de dos

Un contador en el ViewModel, en memoria. Sube con cada `InvalidPlan` y **vuelve a cero con cualquier otro resultado**: «seguidos» se lee literal. Al llegar a `MAX_INVALID_PLANS_IN_A_ROW` (2) el fallo sale con `canGenerateAgain = false` y el pie pasa a «Volver al resumen». Salir y volver a entrar es un ViewModel nuevo, así que empieza de cero sin código.

Consecuencia de leerlo literal: inválido → sin red → inválido no llega al tope. Se acepta: un corte de red entre medias no es un bucle del modelo.

### Los mensajes de la espera

Cuatro, en `strings.xml`, que rotan cada 2 s y vuelven a empezar:

1. «Tu petición está en camino.»
2. «Esperando la respuesta del generador.»
3. «Cuando llegue, el plan se comprueba contra tus máquinas antes de guardarse.»
4. «Si cancelas, no se guarda nada a medias.»

Ninguno habla de cómo será el plan. El tercero y el cuarto son hechos de la app (`PlanValidator`, la cancelación), no del modelo. La rotación es de la pantalla (`LaunchedEffect` + `Crossfade`), no del ViewModel: es animación, no estado. El mensaje es región viva `Polite` para lectores de pantalla.

Con los 3 s del fake, medido en una grabación del emulador: el primero se ve 2,1 s y el segundo 1,2 s antes de que la pantalla cambie. Se lee; no parpadea. No se tocó `FAKE_LATENCY`.

### A dónde va un plan generado

La vista del plan es C-12. Opciones:

1. *Volver al resumen con un aviso*: la persona no sabe si se generó.
2. *Adelantar C-12*: fuera de alcance.
3. *Un destino mínimo en `:feature:plan`*, `PlanDraftRoute(planId)`: **elegida.** Dice que el borrador está guardado y con qué versión. Sin ViewModel: no lee el plan.

Es otro provisional, con cinco textos, pero vive en el módulo que lo va a sustituir y la navegación (`navigateToGeneratedPlan`, que saca la espera de la pila) ya es la definitiva. **C-12 lo sustituye.**

### Sin `RepOrRunScreenScaffold`

Es la forma «barra, scroll y acción al pie». Esta pantalla no tiene barra ni título arriba: solo cerrar, a la derecha. Se compone con piezas sueltas. Lo que comparte con el armazón es el pie fijo con el contenido en scroll encima; con `InventoryScreen`, `NoticeScreen` y `PlanDraftScreen` son ya varias pantallas con «scroll + pie» y cabecera propia. No se extrae aquí.

## Lectura de Figma

| En Figma | En código |
|---|---|
| Botón de cerrar, 44, `bg/surface` | `RepOrRunIconButton(Close)` |
| Anillo 200 con trazo 14, pista `bg/surface`, arco `bg/accent` | `CircularProgressIndicator` con `WaitRingSize`, `WaitRingStroke`, `surface` y `primary` |
| «65%» en `Display/Large` | **No se copia**: la app no sabe cuánto falta. Indeterminado |
| «Generando tu plan», `Headline/Medium` | `headlineMedium` |
| «Armamos 4 sesiones con tus 18 máquinas. Puede tardar hasta un minuto.», `Body/Small`, `text/secondary` | Solo «Puede tardar hasta un minuto.», `bodySmall`, `onSurfaceVariant`. La primera frase afirma lo que hará el modelo |
| Tarjeta con tres pasos (hecho, en curso, pendiente) | **No se copia**: la app no ve los pasos del servicio. Queda la fila «en curso» —punto de 24 en `bg/accent`— con el mensaje que rota |
| «Cancelar», 60, `bg/surface`, `Title/Small` | `RepOrRunButton(style = Tonal)`, estilo nuevo |
| Separación de 20 entre bloques y 40 bajo el cerrar | `ScreenPadding` y `WaitTopSpacing` |

**Lo que la app tiene y Figma no dibuja, para C-01:** todos los errores, la lista de problemas, el tope de dos y el destino del borrador.

### Reglas duras

- La acción principal no es naranja: los pies de error son `Primary` (`inverseSurface`); «Cancelar» es `Tonal`.
- El naranja no es texto ni lleva texto: solo el arco y el punto.
- `#979797` no es color de texto: no aparece.
- Superficies constantes: no hay ninguna aquí.
- Sin elevación ni sombras.

### Contraste medido

| Par | Claro | Oscuro | Umbral |
|---|---|---|---|
| Título y titulares de error, `onBackground` / `background` | 13,26 | 15,00 | 3 |
| Subtítulo y cuerpo de error, `onSurfaceVariant` / `background` | 5,04 | 7,39 | 4,5 |
| Mensaje y «Cancelar», `onSurface` / `surface` | 15,00 | 10,30 | 4,5 |
| Líneas de problemas, `onSurfaceVariant` / `surface` | 5,70 | 5,08 | 4,5 |
| Overline «Qué ha fallado», `accentText` / `surface` | 4,61 | 4,54 | 4,5 |
| Acción del pie, `inverseOnSurface` / `inverseSurface` | 15,00 | 15,00 | 4,5 |
| Icono de cerrar, `iconPrimary` / `surface` | 10,30 | 10,30 | 3 |
| Arco, `primary` / `background` | 3,02 | 4,40 | 3 |
| Arco, `primary` / pista `surface` | 3,41 | 3,02 | 3 |
| Punto, `primary` / `surface` | 3,41 | 3,02 | 3 |

Todo cumple. El arco sobre el lienzo en claro y sobre la pista en oscuro pasan por dos centésimas: no hay margen para aclarar el acento.

**Para C-01, sin corregir:** el contenedor de «Cancelar» (`surface` sobre `background`) mide 1,13 en claro y 1,46 en oscuro, lo mismo que cualquier tarjeta. El botón se reconoce por su texto y su forma, no por el borde; se deja como en Figma y se anota, porque es el mismo par que ya dio problemas en el círculo sin marcar de la pantalla 03.

## Risks / Trade-offs

- **Regeneración tras muerte del proceso** → anotado para A-13 (arriba).
- **Los `problems` se enseñan tal cual**, con rutas como `plan.days[0].exercises[2].equipmentId` → son los que da la validación, pensados para un prompt; legibles para depurar, no para una persona. Traducirlos es trabajo de dominio y de textos, no de esta pantalla.
- **«Reintentar» tras un límite de peticiones no espera** → la pantalla dice los segundos; pulsar antes devuelve el mismo error. Sin cuenta atrás (no objetivo).
- **Otro destino provisional** → vive donde se sustituye y está marcado en el KDoc y en `strings.xml`.
