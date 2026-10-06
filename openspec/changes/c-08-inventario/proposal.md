Cubre la tarea **C-08** de `../docs/backlog/C-diseno-ui.md` (Inventario de máquinas).

## Why

Tras crear un gimnasio no hay a dónde seguir: la lista no tiene «Continuar» y ninguna fila lleva a las máquinas. `ToggleEquipmentUseCase` y `AddEquipmentUseCase` (B-05) existen, pero ninguna pantalla los llama, y sin cinco máquinas `BuildGenerationSummaryUseCase` no deja generar. Además nadie llama a `EquipmentCatalogRepository.loadSeed()` (S-09): el catálogo está vacío en una instalación real.

## What Changes

- **`:feature:gym`**: el inventario de un gimnasio (pantalla 03 de Figma) —catálogo agrupado por categoría, búsqueda por nombre y alias, filtro por categoría, marcar y desmarcar, contador— y un formulario de máquina para añadir una personalizada y para editar una del gimnasio.
- **Lista de gimnasios**: «Continuar» lleva a las máquinas del activo; cada gimnasio tiene una fila «Máquinas»; añadir otro pasa a ser la última fila.
- **`:core:domain`**: `ObserveGymInventoryUseCase`, `LoadCatalogSeedUseCase`, `UpdateEquipmentUseCase`, `RemoveEquipmentUseCase` y `CatalogSearch` (la regla de búsqueda: sin mayúsculas ni acentos, con la ñ como letra propia).
- **Editar se añade, acotado**: rango de carga e incremento y, en una máquina personalizada, el nombre. La categoría y el `loadType` no se cambian (ver diseño).
- **Raíz**: `AppViewModel` carga la semilla del catálogo en cada arranque. El inicio con gimnasio creado sigue siendo la lista.
- **`:core:designsystem`**: `RepOrRunSearchField`.

## No objetivos

- El paso siguiente al inventario (C-09): el pie de la pantalla dice «Listo» y vuelve a los gimnasios; el «Continuar» de Figma llega con él.
- Cambiar la categoría o el `loadType` de una máquina guardada.
- Confirmación antes de quitar una máquina personalizada: el design system no tiene diálogo (lo traerá C-15).
- Notas de la máquina (`Equipment.notes`): el caso de uso las admite; la pantalla no las pide.
- Medir la fluidez con cifras: el emulador disponible no sirve para eso (ver tareas).
- Arreglar Figma (C-01).

## Capabilities

### New Capabilities

- `inventario`: ver el catálogo, buscar, marcar lo que tiene el gimnasio, añadir máquinas propias, editarlas y quitarlas.

### Modified Capabilities

Ninguna archivada. Amplía de hecho `gimnasios` (`c-07-gimnasios`, sin archivar): la lista gana «Continuar» y la entrada a las máquinas.

## Impact

- **Código:** `feature:gym`, `core:domain`, `core:designsystem`, `composeApp`.
- **Dependencias:** ninguna nueva en el catálogo.
- **Contrato:** no se toca. Se respetan sus límites: 120 aparatos por gimnasio y los rangos de `Equipment`, los dos aplicados por los casos de uso.
- **Figma:** no se toca. Se lee la pantalla 03 en las dos páginas v2. **No dibuja** el formulario de máquina, las cabeceras de categoría ni la fila de editar.
- **Plan:** no se toca.
- **Borra datos de la persona:** quitar una máquina personalizada borra lo que escribió. Lo pide el criterio («baja») y un aparato en uso por un plan no se puede quitar.
- **Textos que necesitan revisión antes de publicar:** todos los que no vienen de la pantalla 03.
