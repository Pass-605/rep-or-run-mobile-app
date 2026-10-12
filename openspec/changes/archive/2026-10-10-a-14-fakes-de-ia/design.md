## Context

A-15 dejó en `:core:ai` los DTOs del contrato y un `Json` estricto, sin dependencias de proyecto. No hay ports, ni mapeo al dominio, ni nada que genere. A-12 y A-13 (red y adaptador del proxy) están en `moved:v1.1`: en el MVP el fake es la única implementación. B-06 (`PlanValidator`) y B-07 (`GeneratePlanUseCase`) no existen todavía y B-07 depende de esta tarea.

## Goals / Non-Goals

**Goals:**

- Un port que B-07 pueda orquestar y A-13 implementar sin cambiarlo.
- Un fake cuyo plan pase un validador de inventario, sea cual sea el gimnasio.
- Que C-11 tenga espera y error que enseñar.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### La lectura del contrato deja de ser estricta

`ContractJson` pasa a `ignoreUnknownKeys = true`. El diseño de A-15 dejó esta tensión abierta «antes de A-13»; el README del contrato (§Versionado) ya la resuelve: la app ignora lo que no conoce y la estrictez es de las entradas, que valida la Lambda. Los tests conservan una instancia estricta para el ejemplo `campo-extra`, que es una petición. Faltar un obligatorio o salirse de un enum sigue fallando con el `Json` de producción.

### Los ports viven en `:core:domain`, paquete `port`, y hablan en dominio

`PlanGenerator.generate(request)` y `PlanReviewer.review(request)`, las dos `suspend`. `:core:domain` solo ve `:core:model`, así que ni la petición ni el resultado nombran un DTO.

- **Petición de generación:** perfil, gimnasio, inventario y preferencias. `locale`, `promptVersion` y `schemaVersion` no van: son del cable y los pone el adaptador.
- **Resultado:** `GeneratedPlan`, no `Plan`. Un `Plan` necesita `PlanId` (`ROR-AAAA-MM-DD-vN`), `gymId`, estado y fechas, que son de B-07. `GeneratedPlan` lleva lo que solo el adaptador sabe: los días ya en dominio, `model`, `promptVersion`, `schemaVersion`, `generatedAt` e `inputSnapshotJson` (el cuerpo exacto que se envió). B-07 arma el `Plan` con eso.
- **Errores:** `AiError`, sellado y compartido por los dos ports, con los seis casos de la tarjeta de A-13: `Network`, `Timeout`, `RateLimited(retryAfterSeconds)`, `Upstream`, `InvalidInput(problems)` e `InvalidResponse(problems)`. `InvalidResponse` cubre tanto el `422 PLAN_INVALID` de la Lambda como una respuesta que no se deja leer o mapear. Opción descartada: un tipo de error por port; hoy serían idénticos.
- **Cancelación:** no es un error. La `CancellationException` sube; ningún adaptador la convierte en `Failure`.

### Sin errores de reparación en el port

B-07 dice «un reintento de reparación enviando los errores». El plan §11 pone la reparación en la Lambda, y `generate-request.schema.json` no tiene dónde llevar errores. Opciones:

1. *Un campo de errores en la petición del port*: A-13 no podría enviarlo sin cambiar el contrato.
2. *El port solo distingue los resultados y B-07 reintenta llamando otra vez*: **elegida.** `InvalidResponse` y un plan que `PlanValidator` rechaza son lo que dispara el reintento; `Network` no. Los cinco tests de B-07 se escriben con un generador de test que devuelve una lista de resultados en orden.

La discrepancia entre la tarjeta de B-07 y el plan queda anotada en el backlog; no se resuelve aquí.

### El mapeo DTO ↔ dominio, en `:core:ai`, acumula errores

Paquete `mapper`. `PlanDto` → días de dominio con el mismo patrón que `PlanReading.kt` de `:core:data`: se comprueba con `MetricRange.isValid` y `RirRange.isValid` antes de construir y se devuelven todos los errores con su ruta (`days[0].exercises[2].dose`), sin nombres ni notas. Los ids de día y ejercicio salen del `IdGenerator` inyectado. Un RIR en un ejercicio que no es de repeticiones es un error, no se descarta.

El `ModelErrors` de `:core:data` es `internal`. Opciones: moverlo a `:core:domain` (toca dos módulos más por veinte líneas) o tener el suyo `:core:ai`. **Se duplica**; si aparece un tercer uso, se mueve.

Dominio → `GeneratePlanRequest` para el `inputSnapshotJson`. Los enums se cruzan por nombre (`valueOf(name)`), y un test recorre todos los valores.

### Los ejemplos entran como fuente Kotlin generada

`commonMain` no lee archivos. Una tarea de `build-logic` (`GenerateJsonSourceTask`) escribe un `object` con el JSON de `generate-request.small-gym`, `generate-response.small-gym` y los dos `review-response`. `contract/v1/examples/` sigue siendo la única fuente. Es la opción 1 que el diseño de A-15 reservó para esta tarea. `:core:data` tiene una tarea casi igual para la semilla, definida en su `build.gradle.kts`; no se toca aquí (queda como pendiente unificarlas).

### Cómo adapta el plan el fake

Sobre el `PlanDto` del ejemplo, antes de mapear:

1. **Máquinas.** Para cada máquina que el ejemplo usa se busca una del inventario, sin repetir, en tres vueltas: la misma máquina (mismo `id`, o una entrada del catálogo de una tabla fija de `catalogKey`), una con la misma forma de carga, cualquiera que no sea de cardio. Cada vuelta termina antes de empezar la siguiente, para que una sustituta no se quede con la máquina que otro ejercicio necesitaba tal cual.
2. **Ejercicios.** Coincidencia exacta: el ejercicio va tal cual. Sustituta: toma el nombre de la máquina y una carga de ensayo sin cifras, según su `LoadType` (regla de dominio 4). Sustituta con otra forma de carga: solo si el ejercicio es de repeticiones (una plancha no pasa a una barra). Sin máquina: el ejercicio desaparece. Una alternativa solo sobrevive con coincidencia exacta.
3. **Días.** Los del ejemplo que estén disponibles, completados con otros disponibles; las sesiones se reparten en orden. Un día sin ejercicios desaparece. Los códigos se regeneran con la letra del día (L M X J V S D) y el orden.
4. **Cardio.** El primer modo que eligió la persona; sin modos, sin cardio. Velocidad e inclinación solo si el modo es el del ejemplo.
5. **Calentamiento.** El texto del ejemplo nombra máquinas: se conserva solo en un día que quedó tal cual; si no, uno genérico.

Sin ningún día, `InvalidInput`. El plan nunca nombra una máquina fuera del inventario, que es lo que `PlanValidator` rechazaría.

### Cómo adapta la revisión el fake

Sin ningún ejercicio con `ExerciseFeedback.isComparable`, devuelve el ejemplo `insufficient` (sin propuestas: regla 6). Con alguno, el `sufficient`, quitando las propuestas cuyo ejercicio o día no está en el plan y las de subir carga sobre un ejercicio sin datos comparables. El resumen y las observaciones son los del ejemplo: texto de demostración que no describe el check-in real. La revisión está fuera del MVP; no merece una plantilla.

### Latencia y error, por constructor

`FakeAiConfig(latency, failure: AiError?)` llega por constructor desde `aiModule`. La espera es un `delay`, así que es cancelable y en los tests corre en tiempo virtual. Con `failure`, se espera igual y se devuelve ese error. Leer y adaptar el ejemplo corre en `AppDispatchers.default`. Opción descartada: un proveedor que cambie en ejecución; hoy nadie lo cambiaría.

### Grafo

`aiModule` en `:core:ai` (`di/AiModule.kt`) registra `FakeAiConfig`, `PlanGenerator` y `PlanReviewer` por su interfaz. `Clock`, `AppDispatchers` e `IdGenerator` los ponen `appModule` y `domainModule`. `composeApp` añade el módulo y la dependencia de proyecto, como con `:core:data`.

## Risks / Trade-offs

- **`PlanValidator` no existe**: «el fake produce un plan válido» se comprueba contra las reglas de B-06 que se pueden escribir hoy (inventario, días, códigos únicos, series). La heurística de volumen frente a minutos es de B-06: el ejemplo está pensado para 75 min y con sesiones cortas podría chocar → se ajusta el fake en B-06.
- **Un gimnasio sin nada del ejemplo** recibe un plan de sustitutas: correcto para el validador, pobre como entrenamiento. Como una máquina del inventario no se reparte dos veces, con pocas máquinas el plan puede quedarse en un solo día. Es un fake.
- **`session`, `priority` y `target`** del ejemplo se conservan aunque la máquina sea sustituta; pueden no cuadrar con ella.
- **iOS** no se compila en Windows (`needs:macos`).
