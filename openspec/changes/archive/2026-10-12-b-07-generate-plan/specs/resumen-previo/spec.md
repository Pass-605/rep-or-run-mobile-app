## ADDED Requirements

### Requirement: Fecha de inicio ya pasada

Una fecha de inicio guardada que ya pasó SHALL tratarse como «sin fecha de inicio» en el formulario de preferencias, en el resumen y en lo que se envía para generar: los tres SHALL coincidir. Una fecha de hoy o posterior SHALL conservarse. Sin fecha de inicio, la petición SHALL ir sin ella; la app SHALL NOT inventar una.

#### Scenario: La fecha guardada es de ayer

- **WHEN** la persona llega al resumen con una fecha de inicio guardada que ya pasó
- **THEN** el resumen no enseña fecha de inicio y lo que se envía tampoco la lleva

#### Scenario: La fecha guardada es hoy

- **WHEN** la fecha de inicio guardada es la de hoy
- **THEN** el resumen la enseña y se envía
