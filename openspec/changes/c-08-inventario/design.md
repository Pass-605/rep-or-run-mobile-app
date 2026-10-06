## Context

B-05 dejó `ToggleEquipmentUseCase(gymId, catalogKey)` (al desmarcar devuelve `InUse(equipmentId, usages)`) y `AddEquipmentUseCase`, y avisó de que editar no tenía caso de uso. C-07 dejó cuatro herencias: usar `RepOrRunCheckMark` y `RepOrRunTopBar`, añadir «Continuar» y la entrada al inventario, y decidir el inicio de la app. S-09 dejó sin llamar `loadSeed()`.

Diseño: pantalla **03 · Mi gimnasio** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `104:938` (v2 · Claro) y `115:1216` (v2 · Oscuro).

## Goals / Non-Goals

**Goals:**

- Que la persona marque qué tiene su gimnasio, encuentre una máquina por cómo la llama y añada las que falten.
- Que un aparato en uso por un plan no desaparezca, y que la pantalla lo diga.

**Non-Goals:**

- Los de la propuesta.

## Decisions

**Editar se añade, pero no todo.** La nota de B-05 daba dos salidas. Opciones:

1. *Recortar editar*: el criterio lo pide, y sin él el rango de carga de una máquina del catálogo no se podría poner nunca (marcar no lo pregunta).
2. *Editar todos los campos*: el `loadType` decide cómo se leen los kilos de un plan (plan §4.5). Cambiarlo en un aparato que ya está en planes guardados reinterpreta sus cargas sin aprobación (regla 5), y saber si está en uso pediría un método nuevo en `GymRepository`, su DAO y su test.
3. *Editar lo que no cambia el significado de un plan*: **elegida.** `UpdateEquipmentUseCase` cambia rango e incremento y, en una máquina personalizada, el nombre; conserva id, gimnasio, clave, categoría y `loadType`. Para corregir el tipo se quita y se vuelve a añadir, y quitar ya está protegido por `InUse`.

**Baja.** Una máquina del catálogo se desmarca (`ToggleEquipmentUseCase`). Una personalizada no tiene clave: `RemoveEquipmentUseCase(gymId, equipmentId)`, nuevo, con el mismo `InUse`. No se quita tocando su fila, porque borra lo que la persona escribió y no hay diálogo de confirmación: se quita con un botón de contorno dentro de su formulario, que ya es un paso deliberado.

**`InUse` se dice donde se tocó.** Al desmarcar, en la tarjeta de esa fila; al quitar, en el formulario. Dentro de una tarjeta, porque `accentText` no vale sobre el lienzo. Lo mismo para el tope de 120.

**El cruce es por clave.** `GymInventory.toEntries()` marca una entrada del catálogo si el gimnasio tiene un aparato con su `catalogKey`; el nombre no interviene. Un aparato con una clave que ya no está en el catálogo se conserva como fila propia.

**Búsqueda.** `CatalogSearch` en `:core:domain`, porque es una regla y no presentación: minúsculas, sin acentos ni diéresis, la ñ intacta, la puntuación como espacio; cada palabra de la consulta tiene que estar en el nombre o en algún alias. El índice de cada máquina se calcula una vez, no en cada tecla.

**`ObserveGymInventoryUseCase`.** Un solo flujo con el gimnasio, el catálogo y sus aparatos, en vez de tres casos de uso que el ViewModel tendría que combinar.

**Un ViewModel para el inventario y su formulario**, distinto de `GymViewModel` y con el mismo dueño (la entrada del grafo). Opciones para decirle qué gimnasio:

1. *`SavedStateHandle.toRoute()`*: en los tests en host de Android depende de `Bundle`.
2. *Parámetro de Koin*: `verify()` de `AppGraphTest` pediría declarar `String` como tipo extra.
3. *Evento `Opened(gymId)` desde el destino, que lee la ruta*: **elegida.** Sin infraestructura nueva y testeable; abrir otro gimnasio reinicia el estado.

**Escrituras de una en una.** Marcar es leer y escribir: dos toques seguidos a la vez añadirían la máquina dos veces. Un `Mutex` en el ViewModel las pone en fila.

**Lista.** `LazyColumn` con `key` estable por fila (`c:<catalogKey>`, `e:<equipmentId>`, `h:<categoría>`) y `contentType`; la clave de una entrada del catálogo no cambia al marcarla. El filtrado y las cabeceras se calculan en el ViewModel. La barra y el buscador quedan fijos; los filtros y el contador hacen scroll con la lista, para que con el teclado abierto quede sitio.

**Semilla.** `AppViewModel` llama a `LoadCatalogSeedUseCase` en cada arranque, aparte del destino inicial y en `dispatchers.default`: parsear el JSON no va en el hilo principal.

**Destino inicial: sigue siendo la lista de gimnasios.** Con «Continuar» está a un toque del inventario, y el arranque no puede saber si el inventario «falta»: cuántas máquinas bastan lo decide el resumen (C-10). Cuando exista el plan, el inicio será suyo.

**Después del inventario.** No hay paso siguiente hasta C-09. El pie dice «Listo» y vuelve a los gimnasios; un «Continuar» que retrocede engañaría.

**Módulos.** `feature:gym`, `core:domain`, `core:designsystem`, `composeApp`. Ninguna dependencia sale de §3, así que no hay ADR.

**Tokens.**

| Figma (pantalla 03) | En código |
|---|---|
| `bg/canvas`, `bg/surface`, `bg/inverse` | `background`, `surface` (`RepOrRunCard`, `RepOrRunSearchField`, chip sin elegir), `inverseSurface` (botón, chip y marca elegidos, botón de añadir) |
| `text/primary`, `text/secondary`, `text/on-inverse` | `onBackground`/`onSurface`, `onSurfaceVariant`, `inverseOnSurface` |
| `icon/primary` (lupa, volver), `icon/on-inverse` | `extendedColors.iconPrimary`, `inverseOnSurface` |
| Círculo sin marcar | `RepOrRunCheckMark(checked = false)` (sexta corrección medida) |

**Componente nuevo en `:core:designsystem`.** `RepOrRunSearchField`: la pastilla con lupa de la 03. La separación de 10 entre tarjetas va a `RepOrRunDimens.CardListSpacing`.

**Desviaciones de Figma.** Ninguna es una séptima corrección medida: todos los pares de color de la 03, salvo el círculo ya corregido, cumplen el Anexo A.

1. *Cabeceras de categoría*: Figma no agrupa; el criterio sí. `labelMedium` en `onSurfaceVariant`.
2. *Fila «Rango de carga» / «Editar»* bajo una máquina marcada: no está dibujada. Es la fila con chevron de C-07; `RepOrRunIcons` no tiene lápiz.
3. *Formulario de máquina*: no está dibujado. Campos, chips y botón de contorno del design system.
4. *Pie*: «Listo» con check en vez de «Continuar» con flecha, hasta C-09.
5. *Chips de categoría*: Figma dibuja cuatro; el modelo tiene ocho y se deslizan en horizontal.
