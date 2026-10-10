## Context

La fuente del contrato es `../docs/contract/`, fuera de git: `v1/` con 8 schemas y, a su lado, `examples/` con `invalid/` dentro. El repo de la Lambda no tiene copia. `:core:ai` y `:core:network` no existen; el primero tiene regla en `CLAUDE.md` §3 (`core:ai → core:network, core:domain, core:model`).

## Goals / Non-Goals

**Goals:**

- Que el repo compile y pase sus tests sin `../docs/`.
- Que una diferencia entre la copia y la fuente se detecte con un comando.
- Que los DTOs sean el borde: forma, nada más.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### El script monta la copia aparte y luego copia o compara

`sync-contract.sh` arma en un directorio temporal lo que debe haber en `contract/v1/` (los `*.schema.json` y `examples/` un nivel más abajo) y después, o lo pone en su sitio, o hace `diff -r` contra lo que hay. Copiar y comparar parten así de lo mismo, y un archivo borrado en la fuente también cuenta como diferencia. La alternativa, comparar archivo a archivo con dos rutas distintas, repite en dos sitios la regla del nivel de los ejemplos.

Solo viajan los schemas y los ejemplos. `README.md` y `validate.py` se quedan en `docs/`: `../CLAUDE.md` §4 dice «schemas + ejemplos».

`CONTRACT_SOURCE` permite otra ruta de origen; por defecto, `../docs/contract`.

### `:core:ai` nace sin dependencias de proyecto

Solo `kotlinx-serialization-json`. La regla de §3 le *permite* `core:network`, `core:domain` y `core:model`, pero los DTOs no los necesitan: los pedirá quien mapee (A-13, A-14). `:core:network` no se crea.

### DTOs con sufijo `Dto` y enums propios

`Gym`, `Plan`, `BlockPreferences` y los enums ya existen en `:core:model`. Opciones:

1. *Mismos nombres, otro paquete*: los mappers de A-13 tendrían que importar con alias cada tipo.
2. *Reutilizar los enums de `:core:model`*: renombrar un enum del dominio cambiaría lo que viaja a la Lambda, y `:core:model` no puede llevar `@Serializable`.
3. *Sufijo `Dto` en lo que choca y enums del cable propios*: **elegida.**

Los cuatro sobres se llaman como en el backlog (`GeneratePlanRequest`, `GeneratePlanResponse`, `ReviewPlanRequest`, `ReviewPlanResponse`) y el error, `ApiError`. Los nombres de **campo** son los del schema, sin `@SerialName`.

Las fechas (`startDate`, `painDate`, `generatedAt`) son `String`: el DTO guarda lo que viajó. Convertirlas es del mapper.

### Opcional y anulable no son lo mismo

- Opcional en el schema → anulable con `= null`. Si vale `null`, no se escribe.
- Obligatorio y anulable (todo `plan.schema.json` y `review-result.schema.json`, que son estrictos) → anulable **sin** valor por defecto. Si falta, leer falla; si vale `null`, se escribe `null`.

### Un solo `Json`, estricto

`ContractJson` rechaza claves desconocidas porque todos los objetos de v1 llevan `additionalProperties: false`. Es `internal`: quien lee y escribe el contrato vive en `:core:ai`.

**Tensión que no se resuelve aquí:** el README del contrato dice que añadir un campo opcional es un cambio compatible que no sube `schemaVersion`. Con lectura estricta, una Lambda que añada un campo a una *respuesta* rompería a una app ya publicada. Hoy no hay Lambda desplegada ni app publicada; la decisión (lectura laxa solo en respuestas, o exigir `v2`) es de contrato y se toma antes de A-13.

### Tests en `jvmTest`, leyendo la copia del disco

`commonTest` no puede leer archivos. Opciones:

1. *Generar los ejemplos como fuente Kotlin*, como la semilla de `:core:data`: una tarea de Gradle más, hoy solo para tests.
2. *`jvmTest` + la ruta de `contract/v1` como propiedad del sistema*: **elegida.** `allTests` lo ejecuta, y `contract/v1` es *input* de la tarea, así que cambiar un ejemplo vuelve a correr los tests.

Si A-14 necesita los ejemplos dentro de la app, la opción 1 se hace entonces.

La ida y vuelta compara por valor: `170` y `170.0` son el mismo número, y un campo opcional omitido es igual a uno en `null` (los ejemplos hacen las dos cosas con `usualLoad` y `rirPerceived`). Como eso podría tapar un anulable obligatorio que dejara de escribirse, hay un test aparte para ese caso.

### Los inválidos: cinco se rechazan, dos no se pueden

`expected.json` da, por ejemplo, el schema, la ruta y la keyword que fallan.

| Keyword | Ejemplos | Qué pasa en los DTOs |
|---|---|---|
| `additionalProperties` | `campo-extra` | Falla al leer: clave desconocida |
| `enum` | `loadtype-desconocido`, `metric-minuscula`, `confianza-invalida` | Falla al leer: valor fuera del enum |
| `required` | `falta-rir` | Falla al leer: falta el campo |
| `minItems` | `sin-inventario`, `sin-dias` | **Se lee sin error** |

En los cinco primeros el test exige que el fallo sea en la ruta de `expected.json` y por ese motivo, no un fallo cualquiera.

Una lista vacía tiene la forma correcta; lo que incumple es un límite. Rechazarla en el DTO sería meter validación en el borde, que es justo lo que la tarea prohíbe. Opciones para esos dos:

1. *`init { require(...) }` en los DTOs*: validación en el borde. Descartada.
2. *Un validador de JSON Schema en los tests*: comprobaría los siete igual que `validate.py`, pero es una dependencia nueva (§9: se pregunta antes) y probaría los schemas, no los DTOs.
3. *Comprobar que el motivo sigue en su sitio y dejar constancia de que se leen*: **elegida.** El test verifica que la lista de `expected.json` está vacía en el ejemplo y que el DTO la lee.

Son dos peticiones, no respuestas: la app las construye, no las recibe. Quien impide mandarlas son los casos de uso de B-05 (al menos un día, mínimo de máquinas) y, en última instancia, la Lambda con `400 INVALID_INPUT`.

## Risks / Trade-offs

- **Los tests de los inválidos leen el texto de los errores de kotlinx.serialization** (ruta y motivo). Una versión nueva puede cambiar la redacción y romperlos sin que haya cambiado nada real → fallan en rojo, no en silencio, y se ajustan al subir la versión.
- **En Windows el script necesita Git Bash.** La CI corre en Ubuntu.
- **`.gitattributes` normaliza a LF** y la fuente ya está en LF. Si alguien guardara un schema con CRLF en `docs/`, `--check` daría diferencias tras un checkout limpio.
- **La copia de la Lambda no existe**: «los dos repos con la misma versión» (`../CLAUDE.md` §4) no se puede comprobar hasta S-06.
