## ADDED Requirements

### Requirement: Bienvenida en el primer arranque

Mientras la persona no haya aceptado el aviso, la app SHALL abrirse en la bienvenida, que muestra antes de cualquier otra pantalla que el plan no es consejo médico, qué datos se envían y a quién, y que el resto de los datos se queda en el teléfono.

#### Scenario: Primer arranque

- **WHEN** la persona abre la app recién instalada
- **THEN** ve la bienvenida con el resumen del aviso y la acción de aceptar

#### Scenario: Cerrar sin aceptar

- **WHEN** la persona cierra la app sin aceptar y la vuelve a abrir
- **THEN** ve otra vez la bienvenida

### Requirement: Aceptar guarda el consentimiento

Al aceptar, la app SHALL guardar en el dispositivo el momento de la aceptación y llevar a la persona al resto de la app.

#### Scenario: Aceptar

- **WHEN** la persona pulsa aceptar en la bienvenida
- **THEN** el consentimiento queda guardado con su fecha y la app muestra el destino siguiente

#### Scenario: Atrás después de aceptar

- **WHEN** la persona pulsa atrás después de aceptar
- **THEN** la app no vuelve a la bienvenida

#### Scenario: Dos toques seguidos

- **WHEN** la persona pulsa aceptar dos veces seguidas
- **THEN** la app navega una sola vez y la fecha guardada es la del primer toque

### Requirement: La bienvenida solo aparece una vez

Con el consentimiento guardado, la app SHALL abrirse directamente en su destino principal, sin pasar por la bienvenida.

#### Scenario: Arranque posterior

- **WHEN** la persona abre la app después de haber aceptado en una sesión anterior
- **THEN** no ve la bienvenida

### Requirement: Sin consentimiento no se genera

Mientras no haya consentimiento guardado, la app SHALL contar su falta entre lo que impide generar un plan, y no enviar ningún dato fuera del dispositivo.

#### Scenario: Antes de aceptar

- **WHEN** no hay consentimiento guardado
- **THEN** la generación figura como bloqueada por falta de consentimiento

#### Scenario: Después de aceptar

- **WHEN** la persona acepta
- **THEN** la falta de consentimiento deja de bloquear la generación

### Requirement: Aviso completo

La app SHALL ofrecer el aviso completo desde la bienvenida y también después de haber aceptado. Leerlo no acepta ni retira nada.

#### Scenario: Leer antes de aceptar

- **WHEN** la persona abre el aviso completo desde la bienvenida y vuelve
- **THEN** está otra vez en la bienvenida y el consentimiento sigue sin darse

#### Scenario: Leer después de aceptar

- **WHEN** la persona abre el aviso completo con el consentimiento ya guardado
- **THEN** lo ve y, al volver, el consentimiento sigue guardado con la misma fecha

### Requirement: La bienvenida es igual en los dos temas

La bienvenida SHALL verse igual con el sistema en claro y en oscuro. El aviso completo sigue el tema de la app.

#### Scenario: Cambio de tema

- **WHEN** la persona cambia el tema del sistema con la bienvenida abierta
- **THEN** la bienvenida no cambia de colores

### Requirement: Sin consejos de salud ni de alimentación

Los textos de la bienvenida y del aviso SHALL limitarse a decir qué es la app, qué envía y a quién. No contienen pautas de alimentación, suplementos ni diagnósticos.

#### Scenario: Contenido del aviso

- **WHEN** la persona lee el aviso completo
- **THEN** no encuentra recomendaciones de alimentación, de suplementos ni diagnósticos
