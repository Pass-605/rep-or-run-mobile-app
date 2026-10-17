## ADDED Requirements

### Requirement: Configuración fuera del código

La clave de app y la URL base del proxy SHALL salir de `local.properties`, o de la variable de entorno del mismo nombre si allí faltan, y nunca de un archivo versionado. Si falta alguna, la compilación del cliente SHALL fallar nombrando cada clave que falta.

#### Scenario: Configurado

- **WHEN** `local.properties` tiene `ROR_APP_KEY`, `ROR_BASE_URL_ANDROID` y `ROR_BASE_URL_IOS`
- **THEN** el cliente compila con esos valores

#### Scenario: Respaldo del entorno

- **WHEN** una clave falta en `local.properties` y existe como variable de entorno
- **THEN** se usa la del entorno

#### Scenario: Falta una clave

- **WHEN** una clave no está ni en `local.properties` ni en el entorno
- **THEN** la compilación falla con un mensaje que nombra esa clave y dónde ponerla

### Requirement: Cabeceras en cada petición

Cada petición del cliente SHALL llevar `X-App-Key`, `X-App-Version` y `X-Install-Id`, e ir a la URL base configurada.

#### Scenario: Petición cualquiera

- **WHEN** se hace una petición a una ruta relativa
- **THEN** sale hacia la URL base con las tres cabeceras y sus valores

### Requirement: Esperar a una generación

El cliente SHALL esperar hasta 120 s a que termine una petición, sin cortar antes porque el servidor tarde en mandar el primer byte, y SHALL NOT reintentar por su cuenta.

#### Scenario: Respuesta lenta

- **WHEN** el servidor tarda más de 120 s
- **THEN** la petición termina con `NetworkError.Timeout`, sin reintento

### Requirement: Errores de red con un tipo común

Un fallo de transporte SHALL llegar como `NetworkError`: `Timeout` si hubo conexión y no hubo respuesta a tiempo, `Connectivity` si no se pudo conectar o la conexión se cortó. Una respuesta HTTP, sea cual sea su estado, SHALL NOT ser un `NetworkError`. Una cancelación SHALL propagarse sin traducir.

#### Scenario: Sin conexión

- **WHEN** no se puede conectar con el servidor
- **THEN** el resultado es `NetworkError.Connectivity`

#### Scenario: Error del servidor

- **WHEN** el servidor contesta `429`
- **THEN** el resultado es la respuesta, no un `NetworkError`

#### Scenario: Cancelación

- **WHEN** quien llama cancela la petición
- **THEN** la cancelación se propaga y no hay `NetworkError`
