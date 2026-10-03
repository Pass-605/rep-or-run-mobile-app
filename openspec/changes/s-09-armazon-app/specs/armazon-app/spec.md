## ADDED Requirements

### Requirement: La app arranca en un destino inicial

Al abrirse, la app SHALL mostrar su destino inicial con los datos guardados en el dispositivo, sin necesidad de red ni de cuentas.

#### Scenario: Primer arranque

- **WHEN** la persona abre la app recién instalada
- **THEN** ve el destino inicial y la app no se cierra

#### Scenario: Arranque con datos guardados

- **WHEN** la persona abre la app después de haber guardado datos en una sesión anterior
- **THEN** el destino inicial refleja esos datos

### Requirement: Navegación entre destinos

La app SHALL permitir ir de un destino a otro y volver, llevando al destino de llegada el dato que identifica lo que se abre.

#### Scenario: Ir a otro destino

- **WHEN** la persona completa en el destino inicial la acción que lleva al segundo
- **THEN** la app muestra el segundo destino con el elemento que se acaba de crear

#### Scenario: Volver

- **WHEN** la persona pulsa atrás en el segundo destino, con el botón de la pantalla o con el gesto del sistema
- **THEN** la app vuelve al destino inicial

#### Scenario: Una navegación ocurre una sola vez

- **WHEN** la persona vuelve al destino inicial o gira el dispositivo después de haber navegado
- **THEN** la app no vuelve a navegar por su cuenta

### Requirement: Lo que la persona deja a medias se conserva

Un destino SHALL conservar lo que la persona ha escrito y lo que muestra cuando el dispositivo gira o cambia de tema.

#### Scenario: Giro con un texto a medias

- **WHEN** la persona ha escrito un texto sin guardarlo y gira el dispositivo
- **THEN** el texto sigue ahí

### Requirement: Un dato rechazado se señala en su campo

Cuando una acción se rechaza por un dato inválido, la app SHALL quedarse en el mismo destino y señalar el campo con el motivo, hasta que la persona lo cambie.

#### Scenario: Dato inválido

- **WHEN** la persona confirma una acción con un dato obligatorio vacío
- **THEN** la app no navega y el campo muestra el motivo

#### Scenario: La persona corrige

- **WHEN** la persona modifica el campo señalado
- **THEN** el motivo desaparece

### Requirement: Los datos en pantalla siguen a los guardados

Un destino que muestra datos guardados SHALL actualizarse cuando esos datos cambian, sin que la persona recargue.

#### Scenario: Cambio visible al volver

- **WHEN** la persona crea un elemento, pasa al segundo destino y vuelve
- **THEN** el destino inicial ya refleja el elemento creado
