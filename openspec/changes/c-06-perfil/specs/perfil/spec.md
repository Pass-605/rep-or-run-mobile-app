## ADDED Requirements

### Requirement: Perfil en dos pasos

La app SHALL pedir el perfil en dos pasos: primero los datos de la persona (alias, edad, altura, peso, cintura, cadera y experiencia) y después lo que busca (objetivo, músculos prioritarios, limitaciones y preferencias). Alias, peso, cintura y cadera son opcionales.

#### Scenario: Pasar al segundo paso

- **WHEN** la persona completa el primer paso con datos válidos y continúa
- **THEN** ve el segundo paso

#### Scenario: Volver al primer paso

- **WHEN** la persona vuelve atrás desde el segundo paso
- **THEN** ve el primer paso con lo que había escrito, y al continuar de nuevo el segundo conserva lo elegido

### Requirement: Validación en línea

La app SHALL mostrar junto a cada campo por qué no es válido, con su límite, y no avanzar ni guardar mientras haya errores. Los límites son los del perfil del contrato, con la edad entre 16 y 90.

#### Scenario: Dato fuera de rango

- **WHEN** la persona escribe una edad de 15 y continúa
- **THEN** sigue en el primer paso y el campo de la edad dice entre qué valores tiene que estar

#### Scenario: Dato obligatorio vacío

- **WHEN** la persona continúa sin altura ni experiencia
- **THEN** cada uno muestra que falta, a la vez

#### Scenario: Corregir un campo

- **WHEN** la persona cambia un campo que tenía error
- **THEN** el error de ese campo desaparece

#### Scenario: Guardar sin objetivo

- **WHEN** la persona guarda sin haber elegido objetivo
- **THEN** no se guarda nada y el objetivo muestra que falta

### Requirement: Teclado numérico

Los campos numéricos SHALL abrir un teclado numérico y no admitir otros caracteres. Los decimales se escriben con coma; el punto se acepta y se convierte.

#### Scenario: Letras en un número

- **WHEN** la persona intenta escribir letras en la edad
- **THEN** el campo solo conserva los dígitos

### Requirement: Máximo de músculos prioritarios

La app SHALL permitir marcar como mucho cuatro músculos prioritarios, que es el máximo del contrato.

#### Scenario: Quinto músculo

- **WHEN** la persona tiene cuatro marcados y pulsa un quinto
- **THEN** el quinto no queda marcado y se indica el máximo

#### Scenario: Liberar uno

- **WHEN** la persona desmarca uno de los cuatro
- **THEN** la indicación del máximo desaparece y puede marcar otro

### Requirement: Explicación del objetivo

Al elegir un objetivo, la app SHALL mostrar una tarjeta con qué significa y cómo cambia el plan en repeticiones, volumen, descanso y cardio. El contenido es fijo por objetivo e igual para todas las personas; no lo redacta el modelo.

#### Scenario: Elegir objetivo

- **WHEN** la persona elige un objetivo
- **THEN** aparece la tarjeta de ese objetivo

#### Scenario: Cambiar de objetivo

- **WHEN** la persona elige otro objetivo
- **THEN** la tarjeta pasa a ser la del nuevo

### Requirement: Sin pautas de alimentación

La tarjeta de cualquier objetivo SHALL incluir la nota de que ningún objetivo incluye pautas de alimentación ni suplementos. Ningún texto del perfil contiene consejos de alimentación, de suplementos ni diagnósticos.

#### Scenario: Nota fija

- **WHEN** la persona ve la tarjeta de cualquiera de los cuatro objetivos
- **THEN** la nota está presente

### Requirement: Aviso de privacidad del texto libre

Junto a limitaciones y preferencias, la app SHALL avisar de que lo escrito se envía al generar el plan y de que no hace falta poner datos que identifiquen.

#### Scenario: Ver el aviso

- **WHEN** la persona llega a los campos de texto libre
- **THEN** ve el aviso

### Requirement: Guardar y editar

Al guardar un perfil válido, la app SHALL conservarlo en el dispositivo. Al volver a abrir el perfil, el formulario SHALL aparecer con los datos guardados, y guardar de nuevo sustituye al anterior.

#### Scenario: Guardar

- **WHEN** la persona guarda un perfil válido
- **THEN** el perfil queda guardado

#### Scenario: Dos toques seguidos

- **WHEN** la persona pulsa guardar dos veces seguidas
- **THEN** se guarda una sola vez

#### Scenario: Reabrir

- **WHEN** la persona cierra la app y la vuelve a abrir con un perfil guardado
- **THEN** el formulario muestra los datos guardados

### Requirement: El perfil sigue al consentimiento

Con el aviso aceptado, la app SHALL abrirse en el perfil.

#### Scenario: Tras aceptar

- **WHEN** la persona acepta el aviso
- **THEN** ve el primer paso del perfil y atrás no vuelve a la bienvenida
