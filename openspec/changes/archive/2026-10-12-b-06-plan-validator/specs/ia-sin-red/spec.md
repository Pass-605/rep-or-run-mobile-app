## ADDED Requirements

### Requirement: El plan sin red cabe en los minutos de sesión

El plan que se obtiene sin red SHALL caber en los minutos por sesión de la persona según la misma estimación con la que la app valida un plan. Cuando el ejemplo no cabe, SHALL perder ejercicios desde el final del día y, si aun así no cabe, el cardio; un día SHALL conservar al menos un ejercicio y los códigos de los que quedan SHALL seguir siendo coherentes.

#### Scenario: Sesión de la duración del ejemplo

- **WHEN** se genera con 75 minutos por sesión
- **THEN** el plan conserva todos los ejercicios y el cardio del ejemplo

#### Scenario: Sesión corta

- **WHEN** se genera con 45 minutos por sesión
- **THEN** los días que no cabían tienen menos ejercicios y el plan pasa la validación

#### Scenario: Sesión mínima

- **WHEN** se genera con 20 minutos por sesión
- **THEN** cada día conserva al menos un ejercicio y el plan pasa la validación
