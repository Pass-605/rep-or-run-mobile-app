## ADDED Requirements

### Requirement: Días disponibles

La app SHALL permitir marcar en qué días de la semana puede entrenar la persona, con los siete días en una línea y cuántos hay marcados a la vista, y SHALL NOT guardar las preferencias sin al menos un día marcado, diciendo el motivo junto al selector. Cada día se lee con su letra y SHALL anunciarse con su nombre entero.

#### Scenario: Sin días

- **WHEN** la persona guarda sin ningún día marcado
- **THEN** sigue en la pantalla, ve que hace falta al menos un día y no se guarda nada

### Requirement: Minutos por sesión

La app SHALL ofrecer los minutos por sesión como opciones entre las que se elige una (45, 60, 75 y 90), sin ninguna elegida de antemano, y SHALL NOT guardar las preferencias sin haber elegido, diciéndolo junto a las opciones. Unos minutos ya guardados que no estén entre las opciones SHALL ofrecerse también y aparecer elegidos.

#### Scenario: Sin elegir

- **WHEN** la persona guarda sin haber elegido los minutos
- **THEN** ve que falta ese dato y no se guarda nada

#### Scenario: Cambiar de opción

- **WHEN** la persona elige 60 minutos y después 90
- **THEN** quedan elegidos solo los 90

### Requirement: Duración del bloque

La app SHALL mostrar la duración del bloque, que es de 14 días, y SHALL NOT permitir cambiarla. Junto a la duración SHALL decir qué día empieza el bloque, o que no tiene fecha de inicio.

#### Scenario: Primer uso

- **WHEN** la persona abre las preferencias por primera vez
- **THEN** ve que el bloque dura 14 días

### Requirement: Fecha de inicio opcional

La app SHALL permitir elegir una fecha de inicio de hoy en adelante o no elegir ninguna, que es lo que hay por defecto.

#### Scenario: Sin fecha

- **WHEN** la persona guarda sin elegir fecha
- **THEN** las preferencias se guardan sin fecha de inicio

### Requirement: Cardio preferido

La app SHALL ofrecer como modos de cardio las máquinas de cardio del gimnasio activo, y ningún otro. Sin máquinas de cardio, SHALL decirlo en lugar de ofrecer opciones. Elegir un modo es opcional. Un modo elegido cuya máquina deja de estar en el gimnasio SHALL dejar de estar elegido.

#### Scenario: Gimnasio con cardio

- **WHEN** el gimnasio activo tiene una cinta y una bicicleta
- **THEN** la persona puede marcar cualquiera de las dos, las dos o ninguna

#### Scenario: Gimnasio sin cardio

- **WHEN** el gimnasio activo no tiene máquinas de cardio
- **THEN** la app lo dice y explica cómo añadir una

### Requirement: Ejercicios a evitar

La app SHALL permitir escribir en texto libre los ejercicios que la persona no quiere hacer, avisando de que ese texto se envía para generar el plan, y SHALL rechazar un texto más largo que el límite del contrato.

#### Scenario: Texto demasiado largo

- **WHEN** la persona guarda con un texto que pasa del límite
- **THEN** ve el máximo de caracteres junto al campo y no se guarda nada

### Requirement: Guardar y volver a abrir

La app SHALL guardar las preferencias sustituyendo a las anteriores y, al abrir de nuevo la pantalla, SHALL mostrarlas precargadas. Una fecha de inicio guardada que ya pasó SHALL NOT precargarse.

#### Scenario: Editar

- **WHEN** la persona vuelve a las preferencias después de haberlas guardado
- **THEN** ve lo que guardó y puede cambiarlo

### Requirement: Del inventario a las preferencias

La acción del pie del inventario SHALL llevar a las preferencias del bloque.

#### Scenario: Continuar

- **WHEN** la persona toca «Continuar» en el inventario
- **THEN** llega a las preferencias, y volver la devuelve al inventario
