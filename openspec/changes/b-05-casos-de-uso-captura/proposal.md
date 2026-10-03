Cubre la tarea **B-05** de `../docs/backlog/B-dominio-datos.md` (Casos de uso de captura de datos).

## Why

`:core:domain` solo tiene los ports de B-04. Las pantallas de perfil, gimnasios, inventario, preferencias y resumen (C-06 a C-10) y `GeneratePlanUseCase` (B-07) necesitan una capa que decida qué datos son válidos. Hoy esa decisión solo existe en los constructores de `:core:model`, que **lanzan**: un dato mal tecleado en un formulario acabaría como excepción en un ViewModel.

## What Changes

- Ocho casos de uso en `:core:domain`: guardar y observar el perfil, crear un gimnasio, activar un gimnasio, añadir un aparato propio, marcar o desmarcar un aparato del catálogo, guardar las preferencias del bloque y construir el resumen previo a generar.
- Las reglas de negocio de la captura se deciden ahí: rangos del perfil, al menos un día disponible, minutos por sesión, límites del inventario y mínimo de 5 aparatos para generar.
- Todo dato que llega de la UI se valida **antes** de construir un modelo; el resultado es un tipo sellado con la lista completa de errores, sin valores de la persona dentro.
- Desmarcar un aparato que usa algún plan devuelve un error tipado apoyado en `EquipmentDeletion.InUse`.
- Un port nuevo y pequeño: generador de identificadores. El reloj se inyecta.

## No objetivos

- Renombrar o borrar un gimnasio y editar un aparato existente (C-07 y C-08 los pedirán; no están en el criterio de B-05).
- Construir la solicitud de generación ni comprobar el plan: B-07 y B-06.
- Módulos Koin: no está en el catálogo.
- Mensajes de error para la persona: los pone cada pantalla a partir del error tipado.
- Factorías con resultado en `:core:model` (v1.1, ver B-06).

## Capabilities

### New Capabilities

- `captura-datos`: qué acepta y qué rechaza la app al guardar perfil, gimnasios, inventario y preferencias, y cuándo deja generar un plan.

### Modified Capabilities

Ninguna.

## Impact

- **Código:** solo `:core:domain` (casos de uso y tests). Sin dependencias nuevas, ni entre módulos ni en el catálogo.
- **Contrato:** no se toca. Se leen sus límites, que ya están en `:core:model`.
- **Plan y Figma:** no se tocan.
- **Discrepancia con el backlog:** el criterio dice «edad 14-90»; el contrato y `UserProfile` solo admiten 16-100. Se aplica **16-90**; bajar de 16 es un cambio de contrato que se decide aparte.
- **Discrepancia con C-06:** «máx. 5» prioridades musculares; el contrato admite 4 y se aplica 4.
