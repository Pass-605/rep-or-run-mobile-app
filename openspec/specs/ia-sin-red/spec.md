# ia-sin-red Specification

## Purpose
Que la app funcione de punta a punta sin red ni cuentas: un plan y una revisión que salen de los ejemplos sintéticos del contrato, coherentes con el inventario de la persona, con espera y fallo simulables.
## Requirements
### Requirement: Generar un plan sin red

La app SHALL poder obtener un plan sin red ni cuentas, a partir de los ejemplos sintéticos del contrato (`contract/v1/examples/`). El plan SHALL usar solo máquinas del inventario que se envía, tanto en los ejercicios como en las alternativas, y solo días que la persona marcó como disponibles. Los códigos de ejercicio SHALL ser únicos y corresponder a su día. El cardio SHALL usar un modo de los que la persona eligió, o no aparecer si no eligió ninguno. La carga de un ejercicio SHALL ser un texto de ensayo acorde con la forma de carga de su máquina, sin kilos calculados a partir de la persona.

#### Scenario: El gimnasio tiene las máquinas del ejemplo

- **WHEN** se genera con un inventario que tiene las máquinas del ejemplo y sus mismos días
- **THEN** el plan es el del ejemplo, con los identificadores de las máquinas de ese inventario

#### Scenario: Falta una máquina del ejemplo

- **WHEN** se genera con un inventario al que le falta una máquina del ejemplo
- **THEN** el ejercicio pasa a otra máquina del inventario o desaparece, y ningún ejercicio ni alternativa nombra una máquina que no esté en el inventario

#### Scenario: Otros días disponibles

- **WHEN** la persona tiene disponibles días distintos de los del ejemplo, o menos
- **THEN** el plan solo tiene sesiones en días disponibles y los códigos de sus ejercicios siguen siendo únicos

#### Scenario: Sin cardio elegido

- **WHEN** la persona no eligió ningún modo de cardio
- **THEN** ningún día del plan lleva cardio

#### Scenario: Inventario sin máquinas de fuerza

- **WHEN** el inventario solo tiene máquinas de cardio
- **THEN** no hay plan y el resultado dice que la entrada no sirve para generar

### Requirement: Revisar un plan sin red

La app SHALL poder obtener una revisión sin red a partir de los ejemplos sintéticos del contrato. Una revisión SHALL contener solo propuestas, nunca cambios aplicados. Cada propuesta SHALL referirse a ejercicios y días que existen en el plan revisado. Sin ningún ejercicio con datos comparables, la revisión SHALL decir que los datos son insuficientes y no proponer nada; con datos, SHALL proponer subir carga solo en ejercicios con datos comparables que llegaron al tope de su rango en todas las series.

#### Scenario: Datos insuficientes

- **WHEN** se revisa con un check-in en el que ningún ejercicio tiene datos comparables
- **THEN** la revisión dice que los datos son insuficientes y no trae propuestas

#### Scenario: Datos suficientes

- **WHEN** se revisa con ejercicios que tienen datos comparables
- **THEN** las propuestas apuntan solo a ejercicios y días del plan, y las de subir carga solo a ejercicios con datos comparables que llegaron al tope de su rango

### Requirement: Espera y fallo simulables

La generación y la revisión sin red SHALL tardar un tiempo configurable y SHALL poder configurarse para terminar en un error concreto en lugar de un resultado. Esa configuración SHALL venir de fuera de la lógica. Una operación cancelada durante la espera SHALL terminar sin resultado y sin error.

#### Scenario: Latencia

- **WHEN** se configura una espera y se genera
- **THEN** el resultado llega cuando ha pasado ese tiempo, no antes

#### Scenario: Modo error

- **WHEN** se configura un error y se genera
- **THEN** el resultado es ese error, tras la misma espera

#### Scenario: Cancelación

- **WHEN** se cancela la operación durante la espera
- **THEN** no se entrega ningún resultado ni ningún error

### Requirement: Errores de generación y revisión distinguibles

El resultado de generar o revisar SHALL distinguir, como valor y sin excepciones, el éxito de cada motivo de fallo: sin red, tiempo agotado, demasiadas peticiones (con cuánto esperar, si se sabe), fallo del servicio, entrada rechazada y respuesta que no cumple el contrato. Una respuesta que no cumple el contrato SHALL traer todos los problemas encontrados, no solo el primero, señalando el lugar sin copiar nombres ni notas de la persona.

#### Scenario: Respuesta con varios problemas

- **WHEN** un plan recibido tiene un rango invertido en un ejercicio y un RIR fuera de 0-5 en otro
- **THEN** el resultado es «respuesta que no cumple el contrato» y nombra los dos problemas con su lugar

