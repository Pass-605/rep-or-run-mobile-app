## ADDED Requirements

### Requirement: Leer el plan por día

La vista del plan SHALL enseñar la versión del plan, su enfoque y su estado, y un selector con los días que tienen sesión. Al abrirse SHALL estar elegido hoy si hoy tiene sesión y, si no, el primer día. Del día elegido SHALL enseñar la sesión, la prioridad, el cardio (minutos, modo y esfuerzo, y velocidad e inclinación si las trae), el calentamiento, la movilidad, la vuelta a la calma y su estado.

#### Scenario: Plan recién generado

- **WHEN** termina una generación
- **THEN** se ve ese plan, como borrador, con sus días y el primero que toque elegido

#### Scenario: Hoy tiene sesión

- **WHEN** se abre un plan que tiene sesión el día de la semana de hoy
- **THEN** ese día está elegido y marcado como hoy

#### Scenario: Día sin cardio

- **WHEN** el día elegido no lleva cardio
- **THEN** la vista lo dice en lugar de dejar el hueco

### Requirement: Lista de ejercicios del día

De cada ejercicio del día elegido la vista SHALL enseñar, en su orden: nombre, series × rango en su unidad (repeticiones, metros o segundos), RIR cuando se prescribe, descanso, la máquina por su nombre y la carga de ensayo tal como la escribe el plan. Un ejercicio unilateral SHALL llevar la marca de que las series son por lado. La vista SHALL NOT calcular ni convertir cargas.

#### Scenario: Ejercicio unilateral

- **WHEN** un ejercicio es unilateral
- **THEN** se lee que sus series son por lado

#### Scenario: Ejercicio en metros o segundos

- **WHEN** la métrica de un ejercicio no son repeticiones
- **THEN** el rango lleva su unidad y no aparece RIR

### Requirement: Las alternativas son «elige una»

Las alternativas de un ejercicio SHALL aparecer dentro de él, como opciones entre las que elegir una, sin número de orden ni series. Los recuentos de ejercicios y de series de la vista SHALL NOT incluirlas.

#### Scenario: Ejercicio con alternativas

- **WHEN** un ejercicio trae dos alternativas
- **THEN** se leen como «elige una» bajo ese ejercicio, y el recuento de series del día no cambia

### Requirement: Aprobar un día

En un plan en borrador, la persona SHALL poder aprobar el día elegido mientras esté propuesto. Aprobar SHALL cambiar solo el estado de ese día. La app SHALL NOT ofrecer deshacerlo.

#### Scenario: Aprobar

- **WHEN** la persona aprueba un día propuesto
- **THEN** ese día queda aprobado y los demás no cambian

#### Scenario: Día ya aprobado

- **WHEN** el día elegido ya está aprobado
- **THEN** no se ofrece aprobarlo

### Requirement: Activar el plan

La persona SHALL poder activar un plan cuando todos sus días están aprobados. Mientras falte alguno, la vista SHALL decir cuáles y SHALL NOT activar. Al activar, el plan que estuviera activo en el mismo gimnasio SHALL quedar archivado y conservarse. En un plan activo SHALL NOT ofrecerse aprobar ni activar.

#### Scenario: Faltan días

- **WHEN** queda algún día propuesto
- **THEN** no se puede activar y se lee qué días faltan

#### Scenario: Activar con otro plan activo

- **WHEN** se activa un plan y el gimnasio ya tenía uno activo
- **THEN** el nuevo es el único activo y el anterior queda archivado

### Requirement: Sin plan activo

Abierta sin un plan concreto, la vista SHALL enseñar el plan activo del gimnasio activo. Si no hay ninguno SHALL decirlo y ofrecer ir al resumen previo a generar. Con algún plan guardado en el gimnasio activo, la app SHALL abrirse por esta vista.

#### Scenario: Arranque con plan activo

- **WHEN** se abre la app y el gimnasio activo tiene un plan activo
- **THEN** se ve ese plan

#### Scenario: Arranque sin plan activo

- **WHEN** se abre la app, hay planes guardados y ninguno activo
- **THEN** se ve que no hay plan activo y la forma de preparar uno

#### Scenario: Arranque sin planes

- **WHEN** se abre la app y el gimnasio activo no tiene ningún plan
- **THEN** la app empieza por donde empezaba
