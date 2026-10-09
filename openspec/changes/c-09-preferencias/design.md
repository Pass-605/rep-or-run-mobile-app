## Context

B-05 dejó `SavePreferencesUseCase`, que normaliza, valida contra los límites del contrato y devuelve todos los errores juntos; sin un día disponible no guarda. C-08 dejó una herencia: el pie del inventario dice «Listo» y vuelve a los gimnasios, a la espera de este paso.

Diseño: pantalla **04 · Preferencias del bloque** del archivo `oxGySsrSlyarW78NSmC3aH`. **No se ha podido leer**: el MCP de Figma estaba sin sesión durante la implementación. La pantalla está compuesta con las piezas de `:core:designsystem` y hay que contrastarla con la 04 antes de cerrar la tarea (ver tareas).

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

**Minutos: campo numérico**, solo dígitos y sin valor por defecto: qué número proponer es una decisión de producto que debería venir de Figma.

**Validación.** Solo al guardar, con `SavePreferencesUseCase`; el error de un campo se va al tocarlo. No hace falta un `ValidatePreferencesUseCase`: no hay pasos intermedios ni un tope que impedir mientras se marca (hay como mucho tantas opciones de cardio como máquinas, y el tope de 6 lo dice el caso de uso al guardar).

**Después de guardar.** No hay resumen hasta C-10: se vuelve al inventario. La acción dice «Guardar preferencias», no «Continuar», porque retrocede.

**Navegación.** `planSetupGraph()` con un destino; el resumen será el segundo. El ViewModel vive lo que su destino.

**Formato de kilos.** Esta pantalla no formatea kilos (los minutos son enteros), así que no es el tercer caso: `ProfileFormat.kt` e `InventoryFormat.kt` siguen duplicados y sin subir a `:core:ui`.

**Módulos.** `feature:plansetup` es nuevo, pero es el que nombran el plan (§9) y el backlog. Ninguna dependencia sale de §3, así que no hay ADR.

**Tokens.** Todos los colores salen de componentes ya existentes: `background`, `surface` (tarjetas, campos, chip sin elegir), `inverseSurface`/`inverseOnSurface` (chip elegido y acción), `onBackground`/`onSurface`, `onSurfaceVariant` (apoyo, etiquetas, chip sin elegir) y `accentText` para los errores, siempre dentro de una tarjeta.

## Risks / Trade-offs

- **Sin leer Figma**, la composición puede no coincidir con la 04 (orden, selector de semana en una línea, forma de elegir minutos y fecha).
- Los nombres de cardio en inglés llegan al prompt tal cual.
- El armazón de pantalla y los grupos de chips están ya copiados en tres features (`profile`, `gym`, `plansetup`): candidatos a `:core:designsystem`.
