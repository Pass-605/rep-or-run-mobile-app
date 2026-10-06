Cubre la tarea **C-07** de `../docs/backlog/C-diseno-ui.md` (Gimnasios: crear, editar y activar).

## Why

Al guardar el perfil no hay a dónde seguir: `RepOrRunNavHost` vuelve a entrar al perfil, y con perfil guardado la app sigue abriéndose en él. `CreateGymUseCase` y `SetActiveGymUseCase` (B-05) existen, pero ninguna pantalla los llama, y sin gimnasio activo `BuildGenerationSummaryUseCase` no deja generar. Renombrar no tiene caso de uso, y el criterio de la tarea lo pide.

## What Changes

- **`:feature:gym`**, módulo nuevo: la lista de gimnasios con el activo marcado, el estado vacío para el primero y un formulario del nombre que sirve para crear y para renombrar.
- **`RenameGymUseCase`** en `:core:domain`: las mismas reglas de nombre que crear; cambia solo el nombre. **`ObserveGymsUseCase`**: la lista que pinta la pantalla.
- **Borrar un gimnasio queda fuera** (ver «No objetivos»).
- **Raíz**: guardar el perfil lleva a los gimnasios. `AppViewModel` elige el inicio con el consentimiento **y** el perfil: sin consentimiento, bienvenida; sin perfil, perfil; con los dos, gimnasios.
- **`:core:designsystem`**: `RepOrRunCheckMark` (la marca de selección de la pantalla 03, que también usará el inventario) y `RepOrRunTopBar` (la barra que el perfil tenía como privada; el perfil pasa a usarla).
- **Skill `reporrun-pantalla-figma`**: una sexta corrección medida, la del círculo de selección sin marcar.

## No objetivos

- **Borrar un gimnasio.** No está en el criterio de aceptación (crear, renombrar, activar). `GymRepository.deleteGym` borra en cascada el inventario, los planes y su historial: es borrar datos de la persona (`../CLAUDE.md` §9), pide una confirmación que el design system no tiene y una regla para cuando se borra el activo. Tarea aparte.
- El inventario del gimnasio (C-08): la fila de un gimnasio todavía no lleva a sus máquinas, y tras crear el primero no hay paso siguiente.
- Notas del gimnasio (`Gym.notes`): el caso de uso las admite; la pantalla no las pide.
- Impedir dos gimnasios con el mismo nombre: B-05 no lo exige al crear.
- Ajustes (Q-01) y sus entradas «Perfil» y «Gimnasios».
- Arreglar Figma (C-01).

## Capabilities

### New Capabilities

- `gimnasios`: ver, crear, renombrar y activar los gimnasios de la persona.

### Modified Capabilities

Ninguna archivada. Cambia de hecho dos requisitos de changes sin archivar: «El perfil sigue al consentimiento» (`c-06-perfil`) vale ahora solo mientras no haya perfil, y tras guardar el perfil se va a los gimnasios.

## Impact

- **Código:** módulo nuevo `feature:gym` (previsto en el plan §9 y en `CLAUDE.md` §1); `composeApp`, `core:domain`, `core:designsystem`, `feature:profile` (solo la barra), `settings.gradle.kts`.
- **Dependencias:** ninguna nueva en el catálogo.
- **Contrato:** no se toca. El nombre del gimnasio viaja en `Gym.name` (máx. 80, el mismo tope que aplica el caso de uso).
- **Figma:** no se toca. Se lee la pantalla 03 en las dos páginas v2; **no hay pantalla de lista de gimnasios**.
- **Plan:** no se toca.
- **Textos que necesitan revisión antes de publicar:** todos los de la pantalla, que no vienen de Figma.
