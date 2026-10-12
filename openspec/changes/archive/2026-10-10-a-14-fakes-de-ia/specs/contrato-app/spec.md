## MODIFIED Requirements

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
