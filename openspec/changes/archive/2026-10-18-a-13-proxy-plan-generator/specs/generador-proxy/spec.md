## ADDED Requirements

### Requirement: Pedir el plan al proxy

La app SHALL poder pedir el plan al proxy con `POST /v1/plans:generate`, enviando el perfil, el gimnasio activo con su inventario y las preferencias en la forma de `generate-request.schema.json`. Una respuesta `200` SHALL convertirse en un plan de dominio con identificadores nuevos para sus días y ejercicios, que todavía no es de fiar hasta pasar por `PlanValidator`. El cuerpo enviado SHALL ser exactamente el que se guarda como instantánea de la entrada del plan. La app SHALL ignorar los campos de la respuesta que no conoce.

#### Scenario: Respuesta correcta

- **WHEN** el proxy responde `200` con `generate-response.small-gym.json`
- **THEN** el resultado es un plan con los días y ejercicios del ejemplo, su `model` y su `promptVersion`

#### Scenario: Respuesta que no cumple el contrato

- **WHEN** el proxy responde `200` con un plan al que le falta un campo obligatorio, o con un valor fuera de un enum
- **THEN** el resultado es «respuesta que no cumple el contrato», sin copiar texto de la respuesta

#### Scenario: Instantánea

- **WHEN** se genera un plan
- **THEN** su instantánea de entrada es el cuerpo que salió hacia el proxy, byte a byte

### Requirement: Errores del proxy por estado HTTP

La app SHALL traducir cada respuesta distinta de `200` por su estado HTTP antes que por el código del cuerpo. `400` SHALL ser «entrada rechazada», con los problemas que traiga. `401`, `404` y `405` SHALL ser «fallo del servicio», aunque el cuerpo diga `INVALID_INPUT`: no son culpa de la persona. `422` SHALL ser «respuesta que no cumple el contrato», con sus problemas, y SHALL admitir una generación nueva. `429` SHALL ser «demasiadas peticiones», con los segundos de espera del cuerpo o, si faltan, de `Retry-After`. `504` SHALL ser «tiempo agotado». `502` y cualquier otro estado SHALL ser «fallo del servicio». Sin red SHALL ser «sin red», y una respuesta que no llega a tiempo, «tiempo agotado».

#### Scenario: Clave de app incorrecta

- **WHEN** el proxy responde `401` con `code: INVALID_INPUT`
- **THEN** el resultado es «fallo del servicio», no «entrada rechazada»

#### Scenario: Ruta inexistente

- **WHEN** el proxy responde `404`
- **THEN** el resultado es «fallo del servicio»

#### Scenario: Plan inválido tras reparar

- **WHEN** el proxy responde `422 PLAN_INVALID` con dos `details`
- **THEN** el resultado es «respuesta que no cumple el contrato» con los dos problemas

#### Scenario: Throttling

- **WHEN** el proxy responde `429` con `Retry-After: 59` y sin `retryAfterSeconds` en el cuerpo
- **THEN** el resultado es «demasiadas peticiones» con 59 segundos

### Requirement: Generación cancelable

Una generación por el proxy SHALL poder cancelarse mientras espera la respuesta. Cancelada, SHALL terminar sin resultado y sin error.

#### Scenario: Cancelar mientras el proxy piensa

- **WHEN** se cancela la generación antes de que el proxy responda
- **THEN** no se entrega ningún resultado ni ningún error
