## ADDED Requirements

### Requirement: Días disponibles

La app SHALL permitir marcar en qué días de la semana puede entrenar la persona, y SHALL NOT guardar las preferencias sin al menos un día marcado, diciendo el motivo junto al selector.

#### Scenario: Sin días

- **WHEN** la persona guarda sin ningún día marcado
- **THEN** sigue en la pantalla, ve que hace falta al menos un día y no se guarda nada

### Requirement: Minutos por sesión

La app SHALL pedir los minutos por sesión con teclado numérico y SHALL rechazar, mostrando el rango admitido junto al campo, un valor vacío o fuera de los límites del contrato.

#### Scenario: Fuera de rango

- **WHEN** la persona guarda con unos minutos por debajo del mínimo
- **THEN** ve entre qué valores tiene que estar y no se guarda nada

### Requirement: Duración del bloque

La app SHALL mostrar la duración del bloque, que es de 14 días, y SHALL NOT permitir cambiarla.

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
