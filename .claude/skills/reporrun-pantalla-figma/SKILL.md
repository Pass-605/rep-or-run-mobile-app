---
name: reporrun-pantalla-figma
description: Implementa una pantalla de Rep or Run en Compose partiendo del diseño de Figma, con la auditoría de contraste obligatoria antes de darla por buena. Úsala siempre que la tarea sea construir, portar o retocar una pantalla, un componente visual o el tema.
---

# Pantalla desde Figma

Figma es la fuente de verdad del diseño. Esta skill es el procedimiento; **la tabla de
tokens está en el `CLAUDE.md` de este repo, sección 6, y la normativa es el Anexo A del
plan** (`../docs/plan/PLAN_TRABAJO_MVP.md`). Si la tabla y el Anexo A se contradicen,
manda el Anexo A. No copies aquí valores de color: se quedarían viejos.

## Dónde está el diseño

- Archivo de Figma: `oxGySsrSlyarW78NSmC3aH`.
- Páginas vigentes: **Pantallas · v2 · Claro** y **Pantallas · v2 · Oscuro**, 14 pantallas
  cada una. **No están limpias**: incumplen el Anexo A en tres sitios medidos el 01/10 — ver
  «Correcciones medidas» al final de esta skill antes de copiar nada de ellas.
- Las páginas `Pantallas · Claro` / `Pantallas · Oscuro` (sin `v2`) son el diseño viejo.
  No implementes desde ellas.
- Las pantallas v2 son **composiciones directas, no instancias de componentes**: la tarea
  C-01 del backlog es la que las convertirá. Mientras siga abierta, el lenguaje visual
  está en las pantallas, no en una librería publicada.
- Los tokens viven en la colección local **Color v2**, con modos Light y Dark.

## Procedimiento

1. **Lee el diseño antes de escribir Compose.** Con el MCP de Figma, `get_design_context`
   sobre el frame de la pantalla en *ambas* páginas (claro y oscuro). Lo que devuelve es
   una referencia que hay que adaptar al proyecto, no código final.
2. **Traduce colores a tokens, nunca a literales.** Cada fill del diseño corresponde a un
   rol semántico de la tabla del `CLAUDE.md` §6. Si un fill no encaja en ningún rol, para
   y pregunta: significa que el diseño usa un color sin tokenizar, y eso es un bug del
   diseño, no algo que resolver con un `Color(0xFF…)`.
3. **Reutiliza los patrones ya definidos** (tarjeta, fila con chevron, chip de filtro,
   campo con etiqueta encima, stepper de tres nodos, interruptor, botón de icono circular,
   barra inferior en pastilla). Si la pantalla necesita uno nuevo, va a
   `:core:designsystem`, no dentro del feature.
4. **Comprueba las cinco reglas duras** del `CLAUDE.md` §6 una por una, en voz alta, en el
   PR: la acción principal no es naranja; el naranja nunca es texto ni lleva texto blanco;
   `#979797` no es color de texto; las superficies constantes se ven igual en los dos
   temas; sin elevación ni sombras.
5. **Audita el contraste** (abajo). No es opcional y no se hace a ojo.
6. **Verifica** con los comandos del `CLAUDE.md` §2 y adjunta capturas de los dos temas en
   el PR. No hay tests de UI automatizados en el MVP: las capturas son la evidencia.

## Auditoría de contraste

Para cada texto y cada icono informativo de la pantalla, en **los dos temas**:

1. Resuelve el color real del texto y el del fondo que tiene detrás (el fondo efectivo,
   no el del contenedor raíz).
2. Linealiza cada canal: `c = c/255`, y `c <= 0.03928 ? c/12.92 : ((c+0.055)/1.055)^2.4`.
3. Luminancia relativa: `L = 0.2126*R + 0.7152*G + 0.0722*B`.
4. Ratio: `(Lmax + 0.05) / (Lmin + 0.05)`.
5. Umbral: **4.5:1** para texto normal, **3:1** para texto grande (24 px o más, o 18.66 px
   o más en peso 700). Los iconos accionables, 3:1.

Si algo no llega, **no bajes el umbral y no toques la paleta de la marca**: cambia el rol
semántico por otro que sí cumpla, o propón un derivado nuevo con su ratio medido y su
motivo, como están documentados los que ya existen en el Anexo A.

## Trampas conocidas

- El texto secundario tiende a acabar en `#979797` porque en el diseño se ve bien. No
  cumple. Va `onSurfaceVariant`.
- La barra de progreso y el marcador de hoy son los sitios donde el acento **sí** manda:
  si se neutralizan por aplicar la regla del acento, se pierde la señal de "paso 2 de 3".
- Escribir texto en Figma sin `loadFontAsync` de todos los estilos de Outfit falla.

## Correcciones medidas (01/10/2026, durante C-04)

**Las pantallas v2 de Figma incumplen el Anexo A en varios sitios.** La lista crece según se implementan pantallas: cada tarea añade lo que encuentra, para que la siguiente no lo redescubra. Se midieron al implementar
el design system en código. **Manda el código, no el diseño**: cuando implementes una pantalla
y veas uno de estos casos, aplica la corrección y no copies lo que hay en Figma. Figma se
arregla en C-01, que está en `moved:v1.1`.

| Lo que hace Figma | Ratio | Lo que va en código |
|---|---|---|
| `text/muted` (`#979797`) como color de texto, en **nueve sitios** | 2,86:1 | `onSurfaceVariant` (`#656565` en claro, `#B4B4B4` en oscuro). `#979797` es borde, separador e icono inactivo, nunca texto — regla del Anexo A. |
| Icono activo de la barra inferior ligado a `icon/primary` | casi invisible | `inverseOnSurface`: la pastilla activa es `inverseSurface`, así que el icono encima va en su par. |
| `accentText` sobre el fondo de pantalla en claro | **4,07:1** | Solo dentro de tarjeta (`surface`, `#FFFCF9`), donde da 4,61:1. Sobre `background` (`#F1EEEB`) **no cumple**. |
| Acción principal en `bg/accent` (pantalla 01, «Aceptar y empezar») | — | `inverseSurface`. **La acción principal nunca es naranja**: negra en claro, blanca en oscuro. El naranja marca estado, no manda. |
| Checks de tarjeta con `text/accent` y el frame en modo Light (pantalla 01) | **2,24:1** | El token es el correcto, el **modo del frame** es el que está mal: `#DA330E` sobre `#3F3F3F`. En código resuelve al valor del tema oscuro y da 4,54:1. Si ves un contraste absurdo con un token correcto, mira el modo del frame antes que el token. |

| Círculo de selección **sin marcar** en `bg/canvas` sobre `bg/surface` (pantalla 03, fila «Mancuernas»; medido el 05/10 en C-07) | **1,13:1** en claro, **1,46:1** en oscuro | `RepOrRunCheckMark(checked = false)`: el mismo relleno con un borde `onSurfaceVariant` (5,70:1 y 5,08:1). `outline` no vale para ese borde: 2,86:1 en claro. Una marca de estado pide 3:1, como un icono accionable. El círculo marcado (`bg/inverse` con `icon/on-inverse`) está bien. |

Los dos primeros se ven peor en oscuro: la nota de alimentación de la pantalla 02 y el héroe
en tema oscuro son los dos peores casos del primero.

**El tercero es una restricción nueva, no un error de la pantalla**, y por eso está también en
el Anexo A: `accentText` no vale sobre el lienzo. Si una cifra o un aviso en acento tiene que
ir fuera de una tarjeta, o se mete en una tarjeta, o deja de ser acento. Es el motivo de que el
error de `RepOrRunField` vaya dentro de la tarjeta y no debajo.
