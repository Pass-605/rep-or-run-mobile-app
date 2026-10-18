# opciones-depuracion Specification

## Purpose
Lo que solo existe en una build de depuración: elegir entre los ejemplos sin red y el proxy local, y cargar el gimnasio de ejemplo del contrato para ver un plan de punta a punta contra el runner.
## Requirements
### Requirement: Elegir entre ejemplos y proxy en depuración

En una build de depuración, Ajustes SHALL enseñar una sección de desarrollo con un interruptor para generar con el proxy local en lugar de con los ejemplos sin red. La elección SHALL aplicarse a la siguiente generación sin reiniciar y SHALL conservarse al cerrar la app. Sin elección guardada, SHALL generar con los ejemplos. En una build que no es de depuración, la sección SHALL NOT aparecer y la app SHALL generar siempre con el proxy, sea cual sea la elección guardada.

#### Scenario: Activar el proxy

- **WHEN** en depuración se enciende el interruptor y se genera
- **THEN** la petición sale hacia el proxy

#### Scenario: Build de publicación

- **WHEN** la app no es de depuración
- **THEN** Ajustes no enseña la sección y la generación va al proxy

### Requirement: Gimnasio de ejemplo en depuración

En una build de depuración, la sección de desarrollo SHALL ofrecer cargar el gimnasio de ejemplo del contrato (`generate-request.small-gym.json`): sus aparatos con sus identificadores, sus días disponibles, sus minutos por sesión y su cardio. Cargarlo SHALL dejarlo como gimnasio activo y SHALL sustituir las preferencias guardadas, y la fila SHALL avisarlo antes. Cargarlo otra vez SHALL dejar lo mismo, sin duplicar nada. Los datos SHALL ser los sintéticos del contrato.

#### Scenario: Plan de punta a punta con el runner en modo fake

- **WHEN** se carga el gimnasio de ejemplo, se activa el proxy y se genera contra el runner con `FAKE_OPENAI=true`
- **THEN** el runner responde `200` y la app enseña el plan del ejemplo

#### Scenario: Cargar dos veces

- **WHEN** se carga el gimnasio de ejemplo dos veces
- **THEN** hay un solo gimnasio de ejemplo con sus 11 aparatos

