# generacion-plan Specification

## Purpose
Qué pasa cuando la persona pide un plan: cuándo se envía algo, qué se guarda y con qué identificador, y qué se le dice, sin guardar nada, cuando no hay plan.
## Requirements
### Requirement: Generar solo con todo completo

La app SHALL NOT pedir un plan mientras falte el perfil, un gimnasio activo, máquinas suficientes, las preferencias o el consentimiento. Las condiciones SHALL ser las mismas que enseña el resumen previo, y la respuesta SHALL decir todo lo que falta, no solo lo primero. Sin consentimiento, ningún dato de la persona SHALL salir del dispositivo.

#### Scenario: Falta el consentimiento

- **WHEN** se pide un plan sin haber aceptado el envío de datos
- **THEN** no se envía nada, no se guarda nada y la respuesta dice que falta el consentimiento

#### Scenario: Faltan varias cosas

- **WHEN** se pide un plan sin preferencias y con menos máquinas de las necesarias
- **THEN** la respuesta nombra las dos y no se envía nada

### Requirement: Un plan generado se guarda como borrador

Un plan generado que cumple las reglas de `validacion-plan` SHALL guardarse como borrador del gimnasio activo, sin activarse, junto con lo que se envió para generarlo, el modelo que lo generó y las versiones de prompt y de contrato con que se hizo. La app SHALL NOT activarlo ni sustituir el plan activo por su cuenta.

#### Scenario: Generación correcta

- **WHEN** se pide un plan con todo completo y el plan generado es válido
- **THEN** queda guardado un borrador del gimnasio activo con sus días, lo enviado, el modelo y las versiones

#### Scenario: Ya había un plan activo

- **WHEN** se genera un plan teniendo otro activo
- **THEN** el activo sigue activo y el nuevo queda como borrador

### Requirement: Identificador de versión por fecha

Cada plan guardado SHALL identificarse como `ROR-AAAA-MM-DD-vN`, donde la fecha es la del día en que se generó **en la zona horaria de la persona** y `N` es uno más que la mayor versión ya guardada con esa fecha, en cualquier gimnasio; el primero del día es `v1`.

#### Scenario: Primer plan del día

- **WHEN** se genera un plan un día sin planes guardados
- **THEN** su identificador termina en `v1`

#### Scenario: Segundo plan del día

- **WHEN** se genera otro plan el mismo día
- **THEN** su identificador termina en `v2` y el primero se conserva

#### Scenario: Cerca de medianoche

- **WHEN** se genera un plan cuando en la zona de la persona aún es día 11 y en UTC ya es día 12
- **THEN** el identificador lleva el día 11

### Requirement: Un plan inválido no se guarda ni se repara

Si no llega un plan utilizable, o llega uno que incumple las reglas de `validacion-plan`, la app SHALL NOT guardar nada, SHALL NOT volver a pedirlo por su cuenta y SHALL informar de todos los problemas encontrados, sin nombres ni notas de la persona.

#### Scenario: El plan usa un aparato que no está en el inventario

- **WHEN** el plan generado usa un aparato ajeno al inventario
- **THEN** no se guarda nada, el generador se ha llamado una sola vez y la respuesta lleva el motivo

### Requirement: Distinguir lo que se puede reintentar

La respuesta a una generación fallida SHALL distinguir tres cosas: lo que la persona puede reintentar tal cual (sin conexión, sin respuesta a tiempo, demasiadas peticiones, fallo del servicio); un plan inválido, que no se repite pero admite pedir una generación nueva; y datos que no sirven para generar, que no admiten otro intento hasta cambiarlos. En ningún caso SHALL quedar un plan guardado.

#### Scenario: Sin conexión

- **WHEN** la generación falla por la red
- **THEN** la respuesta es reintentable y no se guarda nada

#### Scenario: Demasiadas peticiones

- **WHEN** el servicio pide esperar
- **THEN** la respuesta es reintentable y dice cuánto esperar si el servicio lo dijo

#### Scenario: Plan inválido

- **WHEN** no hay plan válido tras la generación
- **THEN** la respuesta no es de las reintentables tal cual, y se distingue de la de datos que no sirven

#### Scenario: Datos que no sirven

- **WHEN** el servicio no acepta lo enviado
- **THEN** la respuesta dice qué falla y no es de las que admiten otro intento

### Requirement: Cancelar no guarda nada

Si la generación se cancela antes de terminar, la app SHALL NOT guardar ningún plan, ni entero ni a medias.

#### Scenario: Cancelar mientras se espera

- **WHEN** la persona sale mientras se espera al servicio
- **THEN** no queda ningún borrador

#### Scenario: La respuesta llega tarde

- **WHEN** el plan llega después de cancelar
- **THEN** se descarta sin guardarse

