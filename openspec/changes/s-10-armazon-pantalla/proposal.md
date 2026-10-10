Cubre la tarea **S-10** de `../docs/backlog/S-base.md` (Armazón de pantalla y controles repetidos a `:core:designsystem`).

## Why

`profile`, `gym` y `plansetup` tienen cada uno su copia del armazón de pantalla (barra de arriba, contenido con scroll, acción al pie) y de cuatro controles de sección. C-09 registró la tercera copia; `EquipmentEditorScreen` escondía una cuarta de dos de ellos. C-10, C-11 y C-12 harían la quinta, la sexta y la séptima. Las copias ya no son iguales: solo una anuncia sus títulos como encabezado.

## What Changes

- `:core:designsystem` gana `RepOrRunScreenScaffold`, `RepOrRunSectionTitle`, `RepOrRunSupportingText`, `RepOrRunChipGroup` y `RepOrRunSectionError`, con una sola definición cada uno.
- `profile`, `gym` y `plansetup` los consumen de allí y borran sus copias. En `profile` queda un envoltorio fino, `ProfileStepScaffold`, que pone el título del perfil y el stepper.
- Lo que anuncia el botón de volver y el texto de un error llegan al componente como `String`: los recursos y `FieldIssue` son del feature.
- `checkModuleDependencies` pasa a vigilar `:core:designsystem`: no puede depender de ningún módulo del proyecto. `CLAUDE.md` §3 lo dice.
- La skill `reporrun-pantalla-figma` gana la tabla de nodos por pantalla y el aviso sobre `get_metadata`.
- **Un cambio no visual:** los títulos de sección de `profile` y del formulario de máquinas pasan a anunciarse como encabezado a los lectores de pantalla, como ya hacían los de `plansetup`. Ver `design.md`.

## No objetivos

- Cambiar el aspecto, los textos, el orden, los errores o los estados de carga de ninguna pantalla.
- Tests de comportamiento nuevos.
- El inventario (`InventoryScreen`) y el aviso (`NoticeScreen`): tienen otra forma (lista perezosa con buscador fijo; sin barra ni acción al pie) y no usan este armazón.
- Las tarjetas de aviso sueltas de `gym` (`EditorProblemCard` y la del inventario).
- Re-extraer la barra de arriba: `RepOrRunTopBar` ya es compartida.
- Mover nada a `:core:ui`, que es plomería de UDF.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `design-system`: el armazón de pantalla de formulario y los controles de sección pasan a ser componentes del sistema.

## Impact

- **Código:** `core:designsystem` (dos archivos nuevos y una muestra en el catálogo), `feature:profile`, `feature:gym`, `feature:plansetup` (desaparece `PlanSetupComponents.kt`) y `build-logic` (una entrada en la lista de permitidos y su test).
- **Dependencias:** ninguna nueva, ni en el catálogo ni entre módulos.
- **Contrato, Figma y plan:** no se tocan.
- **Documentación del repo:** `CLAUDE.md` §3, la skill de Figma y el CHANGELOG.
- **iOS:** todo es `commonMain`; no se compila en Windows.
