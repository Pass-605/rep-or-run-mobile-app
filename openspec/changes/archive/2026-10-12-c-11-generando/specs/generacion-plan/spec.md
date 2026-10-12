## MODIFIED Requirements

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
