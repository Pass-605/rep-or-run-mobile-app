## Context

A-14 dejó el port `PlanGenerator`, que devuelve un `GeneratedPlan` con sus `PlanDay` ya en dominio, y el mapeo DTO → dominio de `:core:ai`, que acumula errores de pieza suelta (rango, RIR, series, formato de ids, cardio a medias). B-07 (`GeneratePlanUseCase`) necesita la segunda red antes de guardar. Las reglas son las numeradas en `../docs/contract/README.md`; esta change no las cambia.

## Goals / Non-Goals

**Goals:**

- Que ningún plan que incumpla una regla del contrato llegue a Room.
- Que un plan correcto no se rechace: la regla 6 tiene que ser holgada con el caso común (60 minutos).
- Una sola definición de `ModelErrors`.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### Quién comprueba cada regla

| Regla del contrato | Dónde se comprueba | Por qué |
|---|---|---|
| 1. Aparatos del inventario, también en alternativas | `PlanValidator` | Cruza plan e inventario |
| 2. Días ⊆ disponibles, sin repetir | `PlanValidator` | Cruza plan y preferencias |
| 3. `low ≤ high`, `sets ≥ 1`, RIR 0-5 o nulo | `:core:model` + mapeo de `:core:ai` | Es de un solo campo: `MetricRange`, `RirRange` y `PlanExercise` no dejan construirlo, y el mapeo lo informa antes con `isValid` |
| 4. Código único, con la letra de su día y su `order` | `PlanValidator` | Cruza ejercicios entre sí y con su día |
| 5. `cardioMin ≤ cardioMax` o los dos nulos | `:core:model` + mapeo de `:core:ai` | `CardioPrescription` no deja construirlo; «uno solo de los dos» lo informa el mapeo |
| 6. Volumen compatible con `sessionMinutes` | `PlanValidator` | Cruza plan y preferencias |

`PlanValidator` recibe objetos de dominio, así que una infracción de las reglas 3 o 5 **no se puede expresar** en su entrada. Repetirlas sería código que nunca se ejecuta. En los tests del validador quedan dos casos que fijan esa garantía (el modelo rechaza el dato), para que quien cambie un constructor vea qué regla deja al descubierto.

### Forma del validador

`PlanValidator.validate(days, equipment, preferences)` en `:core:domain`, paquete `validation`. Es una clase sin estado ni dependencias, registrada en `domainModule` como el resto. Recibe los días y no un `GeneratedPlan`: en v1.1 la misma comprobación vale para la versión que crea una revisión aprobada.

Resultado sellado: `PlanValidation.Valid` o `PlanValidation.Invalid(violations)`. Cada `PlanViolation` lleva la regla (`PlanRule`, con su número del contrato), el día, el código del ejercicio si lo hay, la ruta del campo con el mismo formato que el mapeo (`plan.days[0].exercises[1].equipmentId`) y el detalle. `message` lo junta todo:

```text
plan.days[0].exercises[1].equipmentId: regla 1, MONDAY, L02: el aparato eq_x no está en gym.equipment
```

Los mensajes llevan identificadores, códigos y cifras del plan, nunca nombres de ejercicio ni notas: pueden acabar en un log o en un prompt. B-07 puede pasar `Invalid.messages` a `AiError.InvalidResponse.problems` sin transformar nada.

Las infracciones salen ordenadas por regla y, dentro de cada una, por posición en el plan.

Opción descartada: devolver `List<String>` como el mapeo. Es más corto, pero la pantalla de error (C-11) y los tests tendrían que leer texto para saber qué regla falló.

### Regla 4, en tres comprobaciones independientes

Un código es `letra del día + dos dígitos` (`ExerciseCode` ya garantiza 1-2 mayúsculas y dos dígitos). Por cada ejercicio:

1. **Letra:** lo que precede a los dos dígitos es exactamente la letra de su día (L M X J V S D). `LA01` en lunes falla.
2. **Orden:** los dos dígitos, leídos como número, son su `order`. `L03` con `order = 2` falla.
3. **Unicidad:** ningún ejercicio anterior del plan tiene el mismo código. Se informa en cada repetición, con la ruta del primero.

Cada una da su propio mensaje; un ejercicio puede incumplir las tres. No se exige que `order` sea correlativo ni que coincida con la posición: el contrato no lo pide.

La letra del día vivía en una tabla privada del fake (`PlanAdaptation.kt`). Pasa a `:core:domain` (`DayOfWeek.codeLetter`) y el fake la usa: una sola definición.

### Regla 6: la heurística de volumen

El contrato dice «series × descanso estimado + cardio». Fórmula, por día:

```text
estimación = Σ ejercicios [ series × (descanso + trabajo × lados) ] + cardio
tope       = sessionMinutes × 1,10
el día incumple la regla si estimación > tope
```

| Supuesto | Valor | Motivo |
|---|---|---|
| Descanso por serie | 120 s | Centro de los descansos del ejemplo del contrato (de «60-90 s» a «2,5-3 min»; la media ponderada del lunes es 2,0 min) |
| Trabajo por serie y lado | 30 s | 8-12 repeticiones, una plancha de 30-45 s o un carry de 30-40 m |
| Lados | 2 si `unilateral`, 1 si no | Las series de un unilateral son por lado (regla de dominio 2): el trabajo se hace dos veces, el descanso una |
| Series | `PlanExercise.effectiveSets` | Las alternativas son «elige una» y no tienen series: no suman |
| Cardio | `cardio.minMinutes`; 0 sin cardio | El rango es la holgura que el propio plan da: la sesión cabe si cabe su versión corta |
| Margen | 10 % sobre `sessionMinutes` | Ver abajo |

Una serie normal son 2,5 min y una unilateral 3 min. **No cuentan** el calentamiento, la movilidad ni la vuelta a la calma: son texto y el contrato no los nombra en la regla. La estimación es por tanto un suelo, y la regla solo rechaza lo que no cabe ni siquiera sin calentar.

**No se lee `rest`.** Es texto libre («2,5-3 min», «90 s»): leerlo es frágil y un descanso mal escrito no debería tumbar un plan. Opción descartada.

**El margen se elige mirando 60 minutos**, el valor por defecto (C-09). Con 60, el tope son 66 min:

| Sesión de 60 min | Estimación | ¿Pasa? |
|---|---|---|
| 18 series + 10 min de cardio | 55 min | Sí |
| 20 series + 15 min de cardio | 65 min | Sí |
| 22 series + 15 min de cardio | 70 min | No |
| 26 series sin cardio | 65 min | Sí |

22 series más cardio más calentamiento no caben en una hora en ningún gimnasio; 20 series con cardio es ya una sesión apretada y pasa. Un margen del 0 % rechazaría la segunda fila (65 > 60), que es un plan razonable; un 20 % dejaría pasar la tercera.

**Contra el ejemplo del contrato** (`generate-response.small-gym.json`, pensado para 75 min):

| Día | Series | Cardio mín. | Estimación | 45 min (tope 49,5) | 60 (66) | 75 (82,5) | 90 (99) |
|---|---|---|---|---|---|---|---|
| Lunes | 14 | 15 | 50 min | **No** | Sí | Sí | Sí |
| Miércoles | 8 + 2 unilaterales | 10 | 36 min | Sí | Sí | Sí | Sí |
| Sábado | 13 | 20 | 52,5 min | **No** | Sí | Sí | Sí |

El ejemplo pasa con 60, 75 y 90, que son tres de las cuatro opciones de la pantalla. Con 45 no pasa, y es correcto: 35 min de fuerza más 15-20 de cardio más el calentamiento son cerca de una hora. La fórmula no es la estricta; lo que se ajusta es el fake.

El cálculo va en `SessionVolume`, público en `:core:domain`, en segundos enteros (sin coma flotante). El mensaje dice las series, el cardio, la estimación y el tope.

### El fake recorta para caber

Tras mapear, `FakePlanGenerator` ajusta cada día a `sessionMinutes` con el mismo `SessionVolume`, probando en este orden y quedándose con lo primero que cabe:

1. Con cardio, quitando ejercicios del final mientras queden al menos dos.
2. Sin cardio, con todos los ejercicios y quitando del final hasta dejar uno.

Quitar del final conserva los códigos (`L01`, `L02`…) coherentes sin regenerarlos. Un día con un solo ejercicio de 3 series (7,5 min) cabe en el mínimo del contrato (20 min, tope 22). Con 45 min, el lunes y el sábado del ejemplo pierden su último ejercicio y conservan el cardio.

Se recorta en dominio, después del mapeo, porque `SessionVolume` habla en `PlanDay`. Opción descartada: recortar el DTO, que obligaría a duplicar la fórmula.

### `ModelErrors` y `Mapped` suben a `:core:domain`

Paquete `validation`, públicos, con KDoc. `:core:ai` y `:core:data` ya dependen de `:core:domain` (allowlist de `CLAUDE.md` §3), así que no hay dependencia nueva. Se borran las dos copias `internal`. Opción descartada: `:core:model`, que es de tipos puros y no de utilidades para construirlos.

`PlanValidator` no usa `ModelErrors`: no construye nada, y sus errores son tipados.

## Risks / Trade-offs

- **La regla 6 es una estimación**: un plan de muchas series con descansos cortos (circuitos) puede rechazarse siendo viable → el margen del 10 % y no contar el calentamiento lo hacen improbable; si aparece con planes reales, se ajustan las constantes de `SessionVolume`, que están en un solo sitio.
- **La Lambda aplicará la misma regla** (plan §11) y la fórmula no está en el contrato → cuando exista (v1.1), tiene que copiar estos valores o el contrato tiene que fijarlos. Queda anotado en el traspaso.
- **Un plan sin días, o un día sin ejercicios, pasa las seis reglas.** El schema estricto no admite `minItems` y el README no lo numera. No se añade aquí por cuenta propia; se anota para decidir en el contrato.
- **Cobertura ≥ 90 %** no se puede medir: no hay Kover ni JaCoCo. Se cubre cada rama con un test y se dice en el PR.
- **iOS** no se compila en Windows (`needs:macos`).
