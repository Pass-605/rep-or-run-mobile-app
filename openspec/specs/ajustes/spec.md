# ajustes Specification

## Purpose
TBD - created by archiving change q-01-ajustes. Update Purpose after archive.
## Requirements
### Requirement: Elegir el tema

Ajustes SHALL ofrecer tres temas: el del sistema, oscuro y claro, y SHALL marcar cuál está elegido. Elegir uno SHALL aplicarse a toda la app en el momento, sin reiniciarla ni perder la pantalla en la que se está, y SHALL conservarse al cerrar y volver a abrir la app. Sin elección guardada, la app SHALL seguir el tema del sistema.

#### Scenario: Cambiar a oscuro con el sistema en claro

- **WHEN** la persona elige «Oscuro» con el sistema en claro
- **THEN** Ajustes y el resto de pantallas se ven en oscuro sin reiniciar

#### Scenario: La elección sobrevive al cierre

- **WHEN** la persona elige un tema, cierra la app y la vuelve a abrir
- **THEN** la app se abre con ese tema y Ajustes lo marca como elegido

#### Scenario: Seguir al sistema

- **WHEN** el tema elegido es el del sistema y el sistema cambia de claro a oscuro
- **THEN** la app cambia con él

### Requirement: Reabrir el perfil

Ajustes SHALL enseñar una fila del perfil, con la edad y la experiencia guardadas, que abre el formulario del perfil con sus datos. Al guardar, la persona SHALL volver a Ajustes; al salir sin guardar, también. Abrir el perfil desde el resumen previo a generar SHALL seguir volviendo al resumen.

#### Scenario: Editar desde Ajustes

- **WHEN** la persona abre el perfil desde Ajustes, cambia un dato y guarda
- **THEN** vuelve a Ajustes y la fila enseña el dato nuevo

#### Scenario: Ajustes abierto con el resumen más abajo

- **WHEN** la persona llega a Ajustes desde un plan recién generado, con el resumen todavía debajo, y guarda el perfil
- **THEN** vuelve a Ajustes, no al resumen

### Requirement: Releer el aviso y el consentimiento

Ajustes SHALL enseñar cuándo se aceptó el aviso y SHALL abrir el aviso completo. Leerlo SHALL NOT cambiar el consentimiento. Ajustes SHALL NOT ofrecer retirar el consentimiento; si una versión posterior lo ofrece, el resumen previo a generar SHALL ofrecer una acción para volver a aceptarlo, no solo el motivo.

#### Scenario: Abrir el aviso

- **WHEN** la persona toca «Aviso y consentimiento»
- **THEN** se abre el aviso completo, y al volver sigue en Ajustes con la misma fecha de aceptación

### Requirement: Licencias y versión

Ajustes SHALL enseñar la versión de la app instalada y SHALL abrir una pantalla de licencias con el texto íntegro de la licencia de la tipografía Outfit (SIL Open Font License 1.1) y la relación de librerías de terceros que se distribuyen con la app, cada una con su licencia.

#### Scenario: Ver la licencia de la tipografía

- **WHEN** la persona abre «Licencias»
- **THEN** puede leer el aviso de copyright de Outfit y el texto completo del OFL 1.1

#### Scenario: Ver la versión

- **WHEN** la persona abre Ajustes
- **THEN** lee la versión de la app instalada

### Requirement: Llegar a Ajustes

La vista del plan SHALL ofrecer una entrada a Ajustes, y de Ajustes se SHALL volver a ella.

#### Scenario: Ida y vuelta

- **WHEN** la persona abre Ajustes desde la vista del plan y vuelve atrás
- **THEN** está en la vista del plan, con el mismo día elegido

