## Context

`seed/equipment_catalog.json` (B-01) trae cada máquina con `key`, `name` en inglés y `aliases` en español. El `name` es lo que enseña el inventario (C-08), lo que copia `ToggleEquipmentUseCase` a `Equipment.name` al marcar una máquina, lo que enseñan después el resumen y la vista del plan, y lo que `RequestMappers` de `:core:ai` manda en `Gym.equipment[].name`. Es un solo campo para tres usos: verse, buscarse y viajar.

La semilla se carga en cada arranque con `INSERT … IGNORE`: una `key` que ya está en `equipment_catalog` no se toca.

## Goals / Non-Goals

**Goals:**

- Que las 62 máquinas se lean en español en todas las pantallas, también en una instalación que ya tenía el catálogo en inglés.
- Que se sigan encontrando por su nombre en inglés y por sus alias.
- Dejar escrito qué nombre viaja a la Lambda, para A-07.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### Qué nombre viaja a la Lambda: el español

**Para A-07:** `Gym.equipment[].name` es `Equipment.name`, tal cual, y `Equipment.name` es el nombre visible en español. El cliente de la Lambda no traduce ni consulta el catálogo. Una máquina personalizada viaja con el nombre que escribió la persona.

Opciones:

1. *Llevar los dos y enviar el inglés.* Era la propuesta de la tarea: lo que lee el modelo no cambia. Coste: `Equipment` solo tiene un `name`, así que hay que guardar el inglés en `equipment_catalog` (columna nueva: Room v2 y la primera `Migration` del proyecto) y sustituir el nombre por `catalogKey` al armar la petición en `GeneratePlanUseCase`.
2. *Enviar el español*: **elegida**, confirmada con Sebastián el 12/10. Sin cambio de esquema y sin sustitución: se ve lo mismo que viaja.

Por qué la 2. La premisa de la 1 era que los prompts y el ejemplo del contrato están pensados con los nombres en inglés, y no es así:

- `contract/v1/examples/generate-request.small-gym.json` ya manda 9 de 11 nombres en español («Remo con apoyo pectoral», «Jalón al pecho neutro», «Caminadora»). Es la única referencia que existe de lo que el modelo va a leer.
- `plan-gen-v1.md` está en español, pide escribir en español y cruza los aparatos por `id`, nunca por nombre.
- Las máquinas personalizadas viajan siempre con lo que tecleó la persona. El modelo tiene que entender nombres en español en cualquier caso.

Lo que no se puede saber sin cuenta de OpenAI (A-04 está `blocked:accounts`) es si el modelo elige mejor con un idioma u otro. **Criterio de reapertura:** si A-04 lo mide y gana el inglés, el dato sigue en la semilla (`nameEn`) y la opción 1 se hace entonces, con su migración. No es un cambio de contrato en ninguno de los dos casos.

Una precisión sobre el enunciado de la tarea: `Gym.equipment[].id` no es la `key` del catálogo, es el `EquipmentId` del aparato (`eq_…`). La `key` no viaja; une el aparato con su entrada del catálogo dentro de la app. No cambia ninguna de las dos.

### Forma de la semilla: `name` en español y `nameEn`

`name` pasa a ser el visible y aparece `nameEn`. Renombrar `name` a `nameEs` y dejar el inglés en `name` obligaría a tocar todo lo que lee `name` para enseñarlo; así no cambia ninguna pantalla. `catalogVersion` sube a 2: el README dice que sube cuando cambia la forma, y un `nameEn` obligatorio la cambia.

El alias en español que pasa a ser `name` se quita de `aliases`. Por eso `aliases` deja de exigir al menos uno (tres entradas se quedan sin ninguno): `nameEn` ya garantiza un segundo término de búsqueda.

`nameEn` no llega al modelo de dominio como campo. El lector lo añade a `EquipmentCatalogItem.aliases`, que es exactamente lo que es hoy para la app: un término por el que se busca. Así `equipment_catalog` no cambia y `CatalogSearch` e `InventoryEntry` no se tocan. La regla de que ningún término se repite entre entradas incluye ahora `nameEn`.

El lector comprueba además que `name` cabe en `Equipment.NAME_LENGTH`: es el nombre que toma el aparato al marcarlo, y si no cupiera `ToggleEquipmentUseCase` devolvería `CatalogItemUnavailable` en el dispositivo en vez de fallar el test de la semilla.

### La carga de la semilla pone al día el texto

Con `IGNORE`, una instalación que ya cargó el catálogo se queda en inglés para siempre, y lo mismo pasaría con cualquier corrección de las 62 traducciones tras la revisión humana. Opciones:

1. *Dejar `IGNORE`* y pedir desinstalar: vale hoy, que no hay release, y deja de valer el día que la haya.
2. *`@Upsert` del catálogo* y poner al día el nombre de los aparatos marcados: **elegida.** El catálogo es de la app y de solo lectura para la persona; la `key` es estable y lo demás es texto que puede corregirse.

`EquipmentDao.syncCatalogNames()` da a cada aparato con `isCustom = 0` el `name` de la entrada de su `catalogKey`, solo si difiere. No toca los personalizados ni los de una clave que ya no está en el catálogo. Es coherente con `UpdateEquipmentUseCase`: un aparato del catálogo «conserva el nombre del catálogo» y la persona no puede editarlo, así que no se pisa nada que haya escrito.

No se toca ningún plan: un ejercicio guarda su propio nombre y apunta al aparato por id.

## Risks / Trade-offs

- **Las 62 traducciones las redactó la implementación.** → Van a la revisión humana de «Condiciones antes de publicar». Con la carga que pone al día el texto, corregir una es editar la semilla.
- **Variantes regionales** («Caminadora» y no «Cinta de correr», «Remadora»). → Se eligió la que ya usaba el ejemplo del contrato cuando había; las demás siguen como alias.
- **El modelo podría rendir mejor con nombres en inglés.** → No se puede medir sin cuentas; criterio de reapertura arriba.
- **Un gimnasio podría acabar con dos aparatos del mismo nombre** si una máquina personalizada ya se llamaba como el nuevo nombre en español. → Se cruzan por id y por clave, nunca por nombre; solo se vería repetido.
- **El upsert reescribe el catálogo en cada arranque.** → 62 filas una vez por arranque; la sincronización de nombres solo escribe si algo difiere.

## Migration Plan

Sin migración de Room. En el primer arranque con esta versión, la carga de la semilla deja el catálogo y los aparatos marcados en español.
