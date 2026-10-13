Cubre la tarea **B-08** de `../docs/backlog/B-dominio-datos.md` (Nombres del catálogo en español).

## Why

Los 62 nombres de la semilla están en inglés, como se construyeron en B-01, y los alias en español solo sirven para buscar. La app es en español (`../CLAUDE.md` §10) y el inventario, la pantalla con más texto del MVP, enseña «Leg Press».

## What Changes

- **`seed/equipment_catalog.json`**: `name` pasa a ser el nombre visible en español y aparece `nameEn`, el nombre en inglés que rotula el gimnasio. Las 62 `key` no cambian. `catalogVersion` sube a 2 porque cambia la forma del archivo.
- **`:core:data`**: el lector de la semilla exige `nameEn`, comprueba que el nombre visible cabe en un aparato y entrega el inglés como un término de búsqueda más. Los alias dejan de ser obligatorios.
- **`:core:database` y `:core:data`**: cargar la semilla pone al día el texto de las entradas que ya estaban y el nombre de los aparatos marcados desde el catálogo. Antes una clave conocida no se tocaba, así que una instalación existente no vería nunca el español ni una corrección posterior.
- **Decisión para A-07**: en `Gym.equipment[].name` viaja **el nombre en español**, el mismo que se ve. Razones en `design.md`.

## No objetivos

- Enviar el nombre en inglés a la Lambda, o guardar los dos nombres en Room. `nameEn` queda en la semilla para poder hacerlo si A-04 lo pide.
- Cambiar el esquema de Room: no hay columna nueva ni versión 2.
- Tocar el contrato, los prompts o los ejemplos de `contract/v1/`.
- Traducir los nombres de las máquinas personalizadas ni los ejercicios de planes ya guardados: un plan conserva el nombre con el que se generó.
- Cambiar la pantalla del inventario: enseña el mismo campo, que ahora viene en español.

## Capabilities

### New Capabilities

- `catalogo-maquinas`: en qué idioma se lee el catálogo, por qué nombres se encuentra una máquina y cómo llega un cambio de texto a una instalación que ya lo tenía.

### Modified Capabilities

Ninguna.

## Impact

- **Código:** `seed/`, `core:data`, `core:database` (una consulta del DAO, sin cambio de esquema), y KDoc en `core:model` y `core:domain`.
- **Dependencias:** ninguna nueva, ni en el catálogo de versiones ni entre módulos.
- **Contrato:** no se toca. El nombre que viaja es un campo que ya existe (`Equipment.name`) y el ejemplo `generate-request.small-gym.json` ya lo lleva en español.
- **Figma y plan:** no se tocan.
- **Backlog:** las 62 traducciones son texto visible nuevo que no viene de Figma: se suman a la revisión humana de «Condiciones antes de publicar».
