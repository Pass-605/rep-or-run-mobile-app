# resumen-previo Specification

## Purpose
Revisar lo que se va a enviar antes de gastar una generación, corregirlo desde ahí y pedir el plan solo cuando no falta nada.
## Requirements
### Requirement: Lo que se va a enviar

La app SHALL mostrar, antes de generar, cuántos días por semana, cuántas máquinas y cuántos minutos por sesión se van a pedir, y un resumen del perfil, del gimnasio activo con su número de máquinas y de la semana elegida. SHALL decir qué datos salen del dispositivo, sin afirmar que no se envía nada que sí se envía.

#### Scenario: Todo completo

- **WHEN** la persona llega al resumen con perfil, gimnasio activo con máquinas suficientes, preferencias y consentimiento
- **THEN** ve las tres cifras, las tres filas con sus datos y que todo está listo para un bloque de 14 días

#### Scenario: Los datos cambian

- **WHEN** la persona corrige algo y vuelve al resumen
- **THEN** el resumen enseña lo corregido sin tener que recargar nada

### Requirement: Generar solo si no falta nada

La app SHALL NOT permitir generar mientras falte el perfil, un gimnasio activo, máquinas suficientes, las preferencias o el consentimiento. Con la acción deshabilitada SHALL mostrar a la vista todos los motivos a la vez, uno por línea, y la fila de lo que falta SHALL decirlo. Qué falta y cuántas máquinas bastan lo decide la regla de generación, no la pantalla.

#### Scenario: Faltan máquinas

- **WHEN** el gimnasio activo tiene menos máquinas de las necesarias
- **THEN** «Generar plan» está deshabilitado y la persona lee cuántas tiene y cuántas hacen falta

#### Scenario: Faltan varias cosas

- **WHEN** faltan máquinas y las preferencias
- **THEN** la persona ve los dos motivos a la vez

#### Scenario: Deja de faltar

- **WHEN** la persona completa lo que faltaba
- **THEN** los motivos desaparecen y «Generar plan» se habilita

### Requirement: Corregir desde el resumen

La app SHALL permitir abrir desde el resumen el perfil, el gimnasio y las preferencias, cada uno con lo guardado, y al guardar SHALL volver al resumen. Sin gimnasio activo, la fila del gimnasio SHALL llevar a donde se crea o se elige uno.

#### Scenario: Editar el perfil

- **WHEN** la persona abre el perfil desde el resumen, lo cambia y lo guarda
- **THEN** vuelve al resumen y ve el cambio

#### Scenario: Editar las máquinas

- **WHEN** la persona abre el gimnasio desde el resumen, marca máquinas y continúa
- **THEN** vuelve al resumen con el número nuevo

#### Scenario: Editar la semana

- **WHEN** la persona abre las preferencias desde el resumen, las cambia y continúa
- **THEN** vuelve al resumen y ve el cambio

#### Scenario: Arrepentirse

- **WHEN** la persona abre un formulario desde el resumen y vuelve atrás sin guardar
- **THEN** está en el resumen y nada ha cambiado

### Requirement: Del alta al resumen

Durante el alta, guardar las preferencias SHALL llevar al resumen, y volver atrás desde él SHALL NOT mostrar un formulario con datos anteriores a los guardados.

#### Scenario: Primera vez

- **WHEN** la persona guarda sus preferencias por primera vez
- **THEN** llega al resumen

### Requirement: Minutos por sesión propuestos

La primera vez, las preferencias SHALL proponer 60 minutos por sesión, que la persona puede cambiar. Con unas preferencias guardadas SHALL mostrar las guardadas.

#### Scenario: Sin tocar los minutos

- **WHEN** la persona guarda por primera vez sin tocar los minutos
- **THEN** se guardan 60 y el resumen los enseña

### Requirement: Pedir la generación

Con nada pendiente, «Generar plan» SHALL llevar al paso de generación una sola vez aunque se toque dos veces seguidas. Mientras ese paso no exista, SHALL llevar a un destino que lo diga y permita volver, sin generar nada.

#### Scenario: Generar

- **WHEN** la persona toca «Generar plan» con todo completo
- **THEN** sale del resumen hacia el paso de generación

### Requirement: Fecha de inicio ya pasada

Una fecha de inicio guardada que ya pasó SHALL tratarse como «sin fecha de inicio» en el formulario de preferencias, en el resumen y en lo que se envía para generar: los tres SHALL coincidir. Una fecha de hoy o posterior SHALL conservarse. Sin fecha de inicio, la petición SHALL ir sin ella; la app SHALL NOT inventar una.

#### Scenario: La fecha guardada es de ayer

- **WHEN** la persona llega al resumen con una fecha de inicio guardada que ya pasó
- **THEN** el resumen no enseña fecha de inicio y lo que se envía tampoco la lleva

#### Scenario: La fecha guardada es hoy

- **WHEN** la fecha de inicio guardada es la de hoy
- **THEN** el resumen la enseña y se envía

