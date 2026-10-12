# validacion-plan Specification

## Purpose
Que ningún plan generado que incumpla las reglas del contrato (`../docs/contract/README.md`) llegue a guardarse: qué plan acepta la app y qué dice, entero, cuando lo rechaza.
## Requirements
### Requirement: Un plan solo usa aparatos del inventario

La app SHALL rechazar un plan generado si algún ejercicio, o alguna alternativa de un ejercicio, usa un aparato que no está en el inventario del gimnasio para el que se generó (regla 1 de `../docs/contract/README.md`).

#### Scenario: Ejercicio con un aparato ajeno

- **WHEN** un ejercicio del plan usa un aparato que no está en el inventario
- **THEN** el plan se rechaza y el motivo señala ese ejercicio y ese aparato

#### Scenario: Alternativa con un aparato ajeno

- **WHEN** el ejercicio usa un aparato del inventario pero una de sus alternativas no
- **THEN** el plan se rechaza y el motivo señala la alternativa

### Requirement: Un plan solo tiene sesiones en días disponibles

La app SHALL rechazar un plan generado si tiene una sesión en un día que la persona no marcó como disponible, o dos sesiones el mismo día de la semana (regla 2).

#### Scenario: Día no disponible

- **WHEN** el plan tiene una sesión un día que no está entre los disponibles
- **THEN** el plan se rechaza y el motivo señala ese día

#### Scenario: Día repetido

- **WHEN** el plan tiene dos sesiones el mismo día de la semana
- **THEN** el plan se rechaza y el motivo señala la segunda

### Requirement: Los códigos de ejercicio son únicos y coherentes

La app SHALL rechazar un plan generado si un código de ejercicio se repite dentro del plan, si no empieza por la letra de su día (L, M, X, J, V, S, D de lunes a domingo) o si su número no es el orden del ejercicio en el día (regla 4). Cada una de las tres causas SHALL informarse por separado.

#### Scenario: Código repetido

- **WHEN** dos ejercicios del plan tienen el mismo código
- **THEN** el plan se rechaza y el motivo señala el segundo y dice dónde está el primero

#### Scenario: Letra de otro día

- **WHEN** un ejercicio del lunes tiene un código que no empieza por «L»
- **THEN** el plan se rechaza y el motivo dice qué letra corresponde

#### Scenario: Número distinto del orden

- **WHEN** un ejercicio tiene el código `L03` y es el segundo del día
- **THEN** el plan se rechaza y el motivo nombra el código y el orden

### Requirement: Cada sesión cabe en los minutos de la persona

La app SHALL rechazar un plan generado si la duración estimada de alguna sesión supera en más de un 10 % los minutos por sesión que indicó la persona (regla 6). La estimación SHALL contar 2,5 minutos por serie, 3 minutos por serie de un ejercicio unilateral y el mínimo del cardio del día. Las alternativas de un ejercicio SHALL NOT sumar tiempo ni series.

#### Scenario: Sesión que cabe

- **WHEN** una sesión de 20 series y 15 minutos de cardio se valida con 60 minutos por sesión
- **THEN** la sesión se acepta

#### Scenario: Sesión que no cabe

- **WHEN** una sesión de 22 series y 15 minutos de cardio se valida con 60 minutos por sesión
- **THEN** el plan se rechaza y el motivo dice la estimación y el tope de ese día

#### Scenario: Las alternativas no cuentan

- **WHEN** una sesión que cabe justa tiene ejercicios con varias alternativas
- **THEN** la sesión se acepta igual que sin ellas

#### Scenario: El ejemplo del contrato con el valor por defecto

- **WHEN** el plan de ejemplo del contrato se valida con 60 minutos por sesión
- **THEN** se acepta

### Requirement: El rechazo dice todo lo que falla

Cuando la app rechaza un plan generado, el resultado SHALL traer todas las infracciones encontradas, no solo la primera. Cada una SHALL decir la regla del contrato, el día, el ejercicio (si lo hay) y el lugar del plan. Los motivos SHALL NOT copiar nombres de ejercicio ni notas. El rechazo SHALL ser un valor, sin que nada falle ni se cierre.

#### Scenario: Varias infracciones

- **WHEN** un plan tiene un aparato ajeno el lunes y un código repetido el jueves
- **THEN** el resultado nombra las dos, cada una con su regla, su día y su ejercicio

#### Scenario: Plan correcto

- **WHEN** un plan cumple todas las reglas
- **THEN** el resultado es «válido», sin infracciones

