# contrato-app Specification

## Purpose
Que la app y `../docs/contract/` digan lo mismo: una copia del contrato que se puede comprobar contra la fuente, y mensajes que se leen y se escriben sin perder nada. No redefine el contrato.
## Requirements
### Requirement: Copia del contrato en el repo

El repo SHALL llevar una copia versionada del contrato v1 (los schemas y sus ejemplos, válidos e inválidos) tomada de la fuente de verdad del workspace, `../docs/contract/`, sin cambiar su contenido. La app SHALL compilar y pasar sus tests solo con esa copia, sin que exista el resto del workspace.

#### Scenario: Sincronizar

- **WHEN** alguien sincroniza el contrato con la fuente presente
- **THEN** la copia del repo queda idéntica a la fuente, con los ejemplos dentro de la carpeta de la versión

#### Scenario: Sin el resto del workspace

- **WHEN** el repo se clona solo, sin `../docs/`
- **THEN** compila y sus tests de contrato pasan

### Requirement: Comprobar la copia contra la fuente

El repo SHALL ofrecer una comprobación que compare la copia con la fuente sin modificar nada y que falle cuando difieren, diciendo en qué. Si la fuente no está, tanto sincronizar como comprobar SHALL fallar diciendo qué falta y dónde se espera.

#### Scenario: Al día

- **WHEN** se comprueba y la copia es idéntica a la fuente
- **THEN** la comprobación termina bien y no escribe nada

#### Scenario: La copia se quedó atrás

- **WHEN** se comprueba y un schema o un ejemplo difiere, falta o sobra
- **THEN** la comprobación falla, muestra la diferencia y no escribe nada

#### Scenario: Falta la fuente

- **WHEN** se sincroniza o se comprueba sin `../docs/contract`
- **THEN** falla con un mensaje que dice qué carpeta falta y dónde se espera

### Requirement: Leer y escribir los mensajes del contrato sin pérdida

La app SHALL poder leer y volver a escribir cada mensaje del contrato v1 (petición y respuesta de generación, petición y respuesta de revisión, y error) con los nombres de campo del contrato, sin perder, añadir ni cambiar ningún dato. Un campo opcional sin valor SHALL poder omitirse; un campo obligatorio que admite `null` SHALL escribirse siempre.

#### Scenario: Ejemplo válido

- **WHEN** se lee un ejemplo válido del contrato y se vuelve a escribir
- **THEN** el resultado dice lo mismo que el ejemplo

#### Scenario: Ejemplo nuevo en el contrato

- **WHEN** una sincronización trae un ejemplo válido que ningún test cubre
- **THEN** los tests fallan hasta que se cubre

### Requirement: Rechazar mensajes con la forma equivocada

La app SHALL rechazar al leer un mensaje sin un campo obligatorio o con un valor fuera de un enumerado, y el rechazo SHALL señalar el lugar del mensaje y el motivo. La app SHALL ignorar al leer los campos que el contrato no define: el contrato declara compatible añadir un campo opcional, y rechazar las entradas con campos de más corresponde a la Lambda. La app SHALL NOT comprobar al leer los límites del contrato (cantidades mínimas, rangos, longitudes): eso corresponde a los validadores de dominio y a la Lambda.

#### Scenario: Campo de más en una respuesta

- **WHEN** se lee una respuesta válida que además trae un campo que el contrato no define
- **THEN** la lectura no falla y el resto del mensaje se lee igual

#### Scenario: Campo de más en un ejemplo inválido

- **WHEN** los tests leen con lectura estricta una petición cuyo perfil trae un campo que el contrato no define
- **THEN** la lectura falla señalando el perfil y el campo

#### Scenario: Falta un campo obligatorio

- **WHEN** se lee un plan con un ejercicio sin RIR
- **THEN** la lectura falla señalando ese ejercicio y el campo que falta

#### Scenario: Valor fuera del enumerado

- **WHEN** se lee un mensaje con una forma de carga desconocida, una métrica en minúsculas o una confianza que no existe
- **THEN** la lectura falla señalando ese campo

#### Scenario: Lista vacía donde el contrato pide al menos un elemento

- **WHEN** se lee una petición sin máquinas o sin días disponibles
- **THEN** la lectura no falla, porque la forma es correcta; rechazarla no es tarea de esta capa

