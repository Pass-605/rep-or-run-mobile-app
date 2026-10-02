## Context

B-04 dejó en `:core:domain` los ports (`ProfileRepository`, `GymRepository`, `EquipmentCatalogRepository`, `PreferencesRepository`, `SettingsRepository`) y nada más. Dos cosas de su cierre condicionan este diseño: `GymRepository.deleteEquipment` devuelve `EquipmentDeletion.InUse(usages)` en vez de la excepción de SQLite, y el gimnasio activo es `gym.isActive` en Room, que `setActiveGym` cambia en una transacción.

Los constructores de `:core:model` lanzan con las invariantes de un solo campo (B-02). B-06 acepta que el mapper DTO→modelo sea el sitio autorizado a construir desde datos de la Lambda; los formularios son la otra fuente no confiable, y es la de esta change.

## Goals / Non-Goals

**Goals:**

- Que ninguna excepción de constructor llegue a un ViewModel por un dato de un formulario.
- Que la regla viva una vez, en el dominio, y la pantalla solo la muestre.
- Que el formulario reciba todos los errores a la vez, no el primero.

**Non-Goals:**

- Los de la propuesta: renombrar o borrar gimnasios, editar aparatos, solicitud de generación, Koin, textos de error.
- Tipar los fallos inesperados de la capa de datos (disco lleno, base dañada): no son errores esperables.

## Decisions

**Módulos.** Solo `:core:domain`, que sigue dependiendo únicamente de `:core:model` (+ coroutines). Paquete `usecase`, un archivo por caso de uso con su resultado; la entrada y las reglas de cada formulario en su `XxxRules.kt`.

**Entrada cruda, no modelos.** Cada caso de uso que escribe recibe un `XxxInput` con tipos primitivos y enums, y con los obligatorios anulables (`age: Int?`): así «falta» es un error tipado más y la pantalla no decide nada. Devolver el modelo construido solo en el resultado `Saved`. La alternativa —recibir `UserProfile` ya construido— es justo lo que deja subir la excepción.

**Validar primero, construir después.** Una función pura por formulario (`XxxInput.errors()`) devuelve la lista de errores; solo con la lista vacía se llama al constructor. Los rangos se leen de los `companion` de `:core:model`, no se copian. Un test por modelo recorre los límites para comprobar que lo que la validación acepta, el constructor también: si alguien añade un `require` nuevo sin tocar la validación, el test falla en vez de fallar la app.

**Resultado sellado por caso de uso, no `kotlin.Result`.** `Result` envuelve `Throwable`, que es lo que se quiere evitar. Cada caso de uso tiene su `sealed interface` (`Saved` / `Invalid(errors)`), y los errores son otro sellado con el campo (`enum`) y el límite que se incumplió, **sin el valor tecleado**: son datos de salud y un error acaba fácilmente en un log.

**Edad: 16-90.** El backlog pide 14-90; el contrato (`Profile.age`) y `UserProfile.AGE_RANGE` dicen 16-100. Opciones:

1. *14-90 tal cual*: un perfil de 14 años pasa la validación y revienta en el constructor, o se guarda y la Lambda lo rechaza. Descartada.
2. *Cambiar el contrato a 14*: toca los dos repos y exige preguntar (`CLAUDE.md` §9). Fuera de esta change.
3. *Intersección, 16-90*: regla de negocio más estricta que el contrato, que es lo que una regla de negocio puede ser. **Elegida.** La constante vive en el dominio y un test comprueba que cabe dentro del rango del modelo.

**Talla, peso y perímetros: los del contrato** (120-230 cm, 30-300 kg, 40-200 cm). El criterio dice «rangos razonables» y estos ya lo son; inventar otros daría dos tablas.

**Texto libre.** Se recorta (`trim`) antes de medir. Un alias en blanco es «sin alias». Los modos de cardio se recortan, se quitan los vacíos y se deduplican ignorando mayúsculas antes de contar: la persona no debe recibir un error por un espacio de más.

**Identificadores: port `IdGenerator`.** `EquipmentId` tiene formato de contrato (`eq_` + 1-32 de `[A-Za-z0-9_-]`), así que el generador devuelve 32 hexadecimales y el caso de uso antepone el prefijo. Implementación por defecto con `kotlin.uuid.Uuid` (stdlib, ya usado en `:core:data`); los tests inyectan una secuencia. Perfil y preferencias son fila única: se reutiliza el id guardado y, si no hay, uno fijo.

**Reloj: `kotlin.time.Clock` inyectado** en lo que sella fechas (`updatedAt`, `createdAt`), como pide `CLAUDE.md` §3.

**El primer gimnasio queda activo.** Al crear uno sin que haya otro activo, se activa con `setActiveGym` (transaccional). Si ya hay uno activo, no se toca. La alternativa —obligar a la pantalla a encadenar dos llamadas— deja un estado intermedio sin gimnasio activo si la segunda no llega.

**Activar comprueba que el gimnasio existe.** `GymDao.setActive` desactiva todos y marca uno; con un id inexistente dejaría la app sin gimnasio activo. El caso de uso lo comprueba y devuelve `GymNotFound`.

**Marcar y desmarcar del catálogo.** `ToggleEquipmentUseCase(gymId, catalogKey)`: si el gimnasio ya tiene un aparato con esa clave lo borra, y si no lo crea copiando nombre, categoría y `loadType` de la entrada. El borrado se apoya en `EquipmentDeletion`: `InUse(usages)` pasa a ser `ToggleEquipmentResult.InUse(usages)` y el inventario no cambia (plan §4.12: la versión anterior de un plan se conserva entera).

**Límites del inventario.** Mínimo 5 para generar (criterio). Máximo 120 al añadir o marcar: es el `maxItems` de `Gym.equipment` en el contrato, y sin ese tope la solicitud sería inválida. Un aparato propio no puede repetir, ignorando mayúsculas, el nombre de otro del mismo gimnasio: el modelo los distingue por id, pero la persona y el PDF los leen por nombre.

**El resumen es un `Flow`.** `BuildGenerationSummaryUseCase` combina perfil, gimnasio activo con su inventario, preferencias y consentimiento, y emite un `GenerationSummary` con la lista de `GenerationBlocker`. Reactivo porque la pantalla de resumen deja ir a editar cada bloque y volver. B-07 puede tomar el primer valor para rechazar sin datos. «≥ 1 día» no es un bloqueo propio: unas preferencias sin días no se pueden guardar, así que su ausencia es `MissingPreferences`.

## Risks / Trade-offs

- [La validación y el constructor comprueban lo mismo dos veces] → Es el coste de no tocar `:core:model`; el test de límites evita que se separen.
- [16-90 no es lo que dice el backlog] → Anotado en el proposal y en el cierre de la tarea; cambiarlo es una constante, o un cambio de contrato si se quiere bajar de 16.
- [`flatMapLatest` es API experimental de coroutines] → `@OptIn` local; es la forma habitual de seguir el inventario del gimnasio activo.
