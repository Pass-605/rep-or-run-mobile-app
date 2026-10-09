## Context

B-05 dejó `SavePreferencesUseCase`, que normaliza, valida contra los límites del contrato y devuelve todos los errores juntos; sin un día disponible no guarda. C-08 dejó una herencia: el pie del inventario dice «Listo» y vuelve a los gimnasios, a la espera de este paso.

Diseño: pantalla **04 · Preferencias** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `106:870` (página *Pantallas · v2 · Claro*) y `115:1281` (*Pantallas · v2 · Oscuro*). **Leída el 08/10/2026**, después del merge, y la composición se ajustó a ella (ver «Lectura de Figma»). El nodo `30:1044` es la 04 de la página *Pantallas · Claro*, sin `v2`: el diseño viejo, del que no se implementa.

## Goals / Non-Goals

**Goals:**

- Que la persona diga con qué se pide el siguiente bloque y lo pueda corregir después.
- Que la pantalla no contenga ninguna regla: los límites son del caso de uso.

**Non-Goals:**

- Los de la propuesta.

## Decisions

**Los modos de cardio salen del inventario.** El encargo pedía que salieran «del enum del contrato», pero ese enum no existe: `BlockPreferences.cardioModes` es una lista de texto (6 como mucho, 40 caracteres) y el modelo la documenta como «en palabras de la persona». Opciones, consultadas el 08/10:

1. *Lista fija de textos en la app*: es justo la lista escrita a mano que se quería evitar.
2. *Texto libre con chips*: fiel al contrato, pero deja preferir un cardio para el que el gimnasio no tiene máquina.
3. *Enum nuevo en el contrato*: cambio de contrato en los dos repos; tarea aparte.
4. *Las máquinas de categoría `CARDIO` del gimnasio activo*: **elegida.** `EquipmentCategory.CARDIO` sí es un enum del contrato; no hay lista escrita, no se toca el contrato y encaja con la regla 1 del dominio (nada sin máquina del inventario).

Consecuencias: se guarda el **nombre** de la máquina, que para las del catálogo está en inglés, como en la semilla; un gimnasio sin máquinas de cardio no ofrece ninguna y la pantalla lo dice; un modo elegido cuya máquina se quita deja de estar elegido.

**`ObserveCardioOptionsUseCase` en `:core:domain`.** Qué cuenta como modo de cardio es una regla, no presentación. Aplica la misma limpieza que guardar (recortar, sin repetidos ignorando mayúsculas) y recorta a 40 caracteres, porque el nombre de un aparato admite 80.

**Lectura.** Las preferencias guardadas se leen una vez (`ObservePreferencesUseCase`, nuevo), como el perfil: observarlas pisaría lo que se está escribiendo. Las opciones de cardio sí se observan.

**Duración del bloque: se enseña, no se edita.** El contrato fija `blockDays` en 14 (`const`) y el modelo solo admite `BLOCK_DAYS_V1`. Un selector con una sola opción no es un selector. El número sale del estado, que lo toma del modelo; no está escrito en la pantalla ni en los textos.

**Fecha de inicio: chips, no calendario.** «Sin fecha» y los catorce días de hoy en adelante, en una fila que se desliza, como los filtros del inventario. Un calendario sería un componente nuevo del design system o el `DatePicker` de Material, que trae elevación y otro lenguaje. Hoy sale de `Clock` y `TimeZone`, inyectados. Una fecha guardada que ya pasó no se precarga.

**Minutos: chips, no campo.** Figma los dibuja como cuatro opciones (45, 60, 75 y 90) y así van; el primer corte fue un campo numérico porque no se había leído el diseño. Las opciones son de la pantalla, no un límite del contrato, que sigue admitiendo de 20 a 180. Ninguna viene elegida: Figma enseña 75 marcado, pero es un dato de ejemplo, y qué número proponer sigue siendo una decisión de producto. Unos minutos ya guardados que no estén entre las cuatro se añaden a las opciones, para que reabrir no los pierda.

**Validación.** Solo al guardar, con `SavePreferencesUseCase`; el error de un campo se va al tocarlo. No hace falta un `ValidatePreferencesUseCase`: no hay pasos intermedios ni un tope que impedir mientras se marca (hay como mucho tantas opciones de cardio como máquinas, y el tope de 6 lo dice el caso de uso al guardar).

**Después de guardar.** No hay resumen hasta C-10: se vuelve al inventario. La acción dice «Guardar preferencias», no «Continuar», porque retrocede.

**Navegación.** `planSetupGraph()` con un destino; el resumen será el segundo. El ViewModel vive lo que su destino.

**Formato de kilos.** Esta pantalla no formatea kilos (los minutos son enteros), así que no es el tercer caso: `ProfileFormat.kt` e `InventoryFormat.kt` siguen duplicados y sin subir a `:core:ui`.

**Módulos.** `feature:plansetup` es nuevo, pero es el que nombran el plan (§9) y el backlog. Ninguna dependencia sale de §3, así que no hay ADR.

**Tokens.** Todos los colores salen de componentes ya existentes: `background`, `surface` (tarjetas, campos, chip sin elegir), `inverseSurface`/`inverseOnSurface` (chip elegido y acción), `onBackground`/`onSurface`, `onSurfaceVariant` (apoyo, etiquetas, chip sin elegir) y `accentText` para los errores, siempre dentro de una tarjeta.

## Lectura de Figma (08/10/2026)

**Contraste.** Los pares de texto de la 04 cumplen el Anexo A en los dos temas: `text/secondary` sobre `bg/canvas` (5,04 y 7,39), `text/primary` sobre `bg/canvas` (13,26 y 15,00), `text/secondary` sobre `bg/surface` (5,70 y 5,08), `text/primary` sobre `bg/surface` (15,00 y 10,30) y `text/on-inverse` sobre `bg/inverse` (15,00). No usa `text/muted` ni `text/accent`. **No hay séptima corrección** y la skill `reporrun-pantalla-figma` no se toca.

**Lo que se ajustó a Figma:** el título («Tu semana»); los días con una letra, los siete en una línea, con «N elegidos» junto al título de la sección; los minutos como chips; la tarjeta del bloque con «14 días», la fecha de inicio debajo y «Fijo en esta versión»; y los títulos «Cardio que puedes hacer» y «Qué prefieres evitar».

**Lo que la app tiene y Figma no dibuja**, y se queda: cómo se elige la fecha de inicio (Figma solo la enseña en la tarjeta del bloque), el aviso de privacidad del texto libre, la nota de dónde salen las opciones de cardio, el caso del gimnasio sin cardio y los errores. Para C-01.

**Lo que Figma dibuja y no se copia:** el overline «Paso 3 de 4» (ninguna pantalla lo lleva: la barra es `RepOrRunTopBar`, con el título centrado, y el recorrido real ya no tiene cuatro pasos); el título de «Qué prefieres evitar» fuera de la tarjeta (el campo es `RepOrRunField`, con la etiqueta dentro); los chips de día de 40 de alto (el Anexo A fija 34); y «Continuar» con flecha, que llega con C-10.

## Risks / Trade-offs

- Los minutos ya no admiten cualquier valor del rango del contrato desde la pantalla: solo los cuatro de Figma.
- Los nombres de cardio en inglés llegan al prompt tal cual.
- El armazón de pantalla y los grupos de chips están ya copiados en tres features (`profile`, `gym`, `plansetup`): candidatos a `:core:designsystem`.
