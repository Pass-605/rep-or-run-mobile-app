## Context

Es un refactor: ninguna pantalla cambia de aspecto ni de comportamiento. Lo que había antes de empezar:

| Componente | Copias |
|---|---|
| Armazón | `GymScaffold`, `PlanSetupScaffold`, `ProfileStepScaffold` |
| `SectionTitle` | `profile`, `plansetup` y una privada en `EquipmentEditorScreen` |
| `SupportingText` | `gym`, `plansetup`, `profile` |
| `ChipGroup` | `plansetup`, `profile` y una privada en `EquipmentEditorScreen` |
| `SectionError` | `plansetup` (`String?`) y `profile` (`FieldIssue?`) |

La barra de arriba (`RepOrRunTopBar`) ya era compartida desde C-07; faltaba lo que la envuelve.

## Goals / Non-Goals

**Goals:**

- Una definición de cada pieza, en `:core:designsystem`.
- Que la pantalla siguiente (C-10) no tenga nada que copiar.
- Que `:core:designsystem` no pueda acabar importando dominio sin que `check` lo diga.

**Non-Goals:**

- Los de la propuesta.

## Decisions

**Módulos que se tocan.** `core:designsystem`, `feature:profile`, `feature:gym`, `feature:plansetup` y `build-logic`. No se crea ningún módulo ni ninguna dependencia entre módulos. No hay pantalla de Figma nueva: los tokens son los que ya usaban las copias (`titleMedium`, `bodySmall`, `labelSmall`, `onSurfaceVariant`, `extendedColors.accentText`, y `ScreenPadding`, `SectionSpacing`, `ChipSpacing` y `ListRowSpacing` de `RepOrRunDimens`).

**A `:core:designsystem`, no a `:core:ui`.** `:core:ui` aloja `CollectEffects`, que es plomería de UDF; esto es visual. Un feature puede depender de los dos.

**Nombres.** Con el prefijo del módulo: `RepOrRunScreenScaffold`, `RepOrRunSectionTitle`, `RepOrRunSupportingText`, `RepOrRunChipGroup`, `RepOrRunSectionError`. El armazón va en su archivo y las cuatro piezas de sección en `RepOrRunSection.kt`. `RepOrRunChipGroup` envuelve `RepOrRunChip`; no lo duplica.

**Las cinco fricciones.**

1. *El texto del botón de volver.* Sale de los recursos de cada feature (`gym_back`, `prefs_back`, `profile_back`) y `:core:designsystem` no los alcanza. El armazón recibe `backContentDescription: String`, igual que `RepOrRunTopBar`, y cada pantalla pasa el suyo.
2. *`ProfileStepScaffold` recibe `step`, no `title`.* El armazón genérico toma `title: String`. En `:feature:profile` queda `ProfileStepScaffold` como envoltorio: pone el título del perfil y mete el stepper como primer elemento del contenido. Está en la misma columna y con la misma separación que antes, así que no se mueve nada.
3. *`onBack`.* Nulable en `gym` y `profile`, no nulable en `plansetup`. Gana la nulable, con `null` por defecto: el primer destino no tiene vuelta.
4. *`actionIcon` y `trailing`.* Se quedan, con `null` por defecto. `plansetup` no pasa icono; solo `plansetup` pasa `trailing` (el «N elegidos» de C-09).
5. *`SectionError`.* `FieldIssue` vive en `core:domain` y el design system no lo importa. El componente toma `String?` y `profile` traduce con su `FieldIssue.message()` antes de llamar, como ya hacía `plansetup`.

**`gym` no conserva envoltorio.** Opciones:

1. *Un `GymScaffold` fino que solo añada `gym_back`*: ahorra una línea en tres llamadas y deja un nombre más que buscar.
2. *Llamar al armazón directamente y pasar `gym_back`.* **Elegida.** `profile` sí conserva envoltorio porque aporta algo (el stepper); uno que solo fija un texto no.

**El armazón no decide navegación.** Recibe la vuelta y la acción del pie como lambdas. No conoce rutas ni ViewModels.

**Quién no usa el armazón.** `InventoryScreen` tiene buscador fijo y lista perezosa: un `verticalScroll` alrededor de una `LazyColumn` no vale. `NoticeScreen` no tiene barra ni acción al pie. Se quedan como están. Si C-12 trae otra pantalla de lista con pie fijo, ese será el momento de sacar una variante, con dos casos delante y no con uno.

**`:core:designsystem` entra en `checkModuleDependencies`.** `ModuleDependencyRules` no tenía entrada para el módulo, así que nada vigilaba lo que entraba: la misma ceguera que S-09 anotó con Koin en `:core:domain`. El módulo no depende de ningún módulo del proyecto, así que la entrada es `":core:designsystem" to emptySet()`, con su línea en `CLAUDE.md` §3 (la tabla sigue al documento, no al revés) y un test en `build-logic`. El límite de la tarea sigue siendo el que anotó S-09: solo mira dependencias de proyecto, no librerías.

**El catálogo.** `RepOrRunCatalog` dice reunir todos los componentes, así que gana una muestra de las cuatro piezas de sección. El armazón no entra: ocupa la pantalla entera.

## Diferencias entre las copias

Al unificar apareció una, y no es de color ni de medida, así que el Anexo A no la decide:

| Pieza | `plansetup` | `profile` y `EquipmentEditorScreen` | Unificado |
|---|---|---|---|
| `SectionTitle` | `Row` a todo el ancho, título con `semantics { heading() }` y `trailing` opcional | `Text` suelto, sin semántica de encabezado | El de `plansetup` |

- **A la vista no cambia nada.** El título va en `titleMedium` en las dos; un `Text` con `weight(1f)` en una fila a todo el ancho ocupa y parte línea igual que un `Text` suelto en la columna.
- **Sí cambia lo que oye un lector de pantalla:** los títulos de sección del perfil y del formulario de máquinas pasan a anunciarse como encabezado. Opciones:
  1. *Un parámetro para apagar la semántica y conservar cada pantalla exactamente como estaba*: mantiene viva la diferencia que el refactor viene a quitar, y a favor de la variante peor.
  2. *Unificar con encabezado.* **Elegida.** Es la copia más reciente y la única leída contra Figma (C-09), y va en la línea del requisito de accesibilidad de C-04. Queda dicho en la propuesta y en el PR porque es el único punto en que el criterio «ningún cambio de comportamiento» no se cumple a la letra.

Lo demás era idéntico salvo comentarios: el armazón (mismos modificadores y mismas medidas en las tres copias), `SupportingText`, `ChipGroup` y el cuerpo de `SectionError`.

## Risks / Trade-offs

- [Los títulos de `profile` y del formulario de máquinas se anuncian ahora como encabezado] → Es una mejora y está documentada arriba; si no se quiere dentro de un refactor, se revierte quitando una línea de `RepOrRunSectionTitle`, pero entonces `plansetup` la pierde.
- [Un armazón compartido tienta a añadirle parámetros por pantalla] → Lo que sea de una sola pantalla va en su contenido o en un envoltorio del feature, como el stepper del perfil.
- [La regla nueva de `checkModuleDependencies` no ve librerías externas] → Limitación conocida de la tarea (S-09). Fuera de esta change.
- [iOS no se compila en Windows] → Todo es `commonMain` y solo usa APIs que los features ya usaban desde `commonMain`.
