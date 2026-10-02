## ADDED Requirements

### Requirement: Tipografía Outfit en toda la app

La app SHALL mostrar todo su texto en Outfit, con los pesos 400, 500, 600 y 700 empaquetados en la propia app, sin depender de fuentes del sistema ni de la red.

#### Scenario: Texto sin conexión

- **WHEN** la app se abre por primera vez sin conexión
- **THEN** los títulos, el cuerpo y las etiquetas se ven en Outfit

#### Scenario: Pesos por tipo de texto

- **WHEN** una pantalla muestra un título, un párrafo, una etiqueta y una cifra
- **THEN** el título va en 600 o 700, el párrafo en 400, la etiqueta en 500 y la cifra en 500 o 600

### Requirement: Dos temas con los mismos componentes

Cada componente SHALL verse en tema claro y en tema oscuro con los colores que el Anexo A del plan asigna a cada tema, sin que la pantalla que lo usa tenga que distinguir el tema.

#### Scenario: Cambio de tema del sistema

- **WHEN** el sistema pasa de claro a oscuro con una pantalla abierta
- **THEN** todos sus componentes cambian de colores y ninguno conserva un color del tema anterior

### Requirement: La acción principal no usa el acento

El botón principal, el chip elegido, el interruptor encendido y el destino activo de la barra inferior SHALL usar el bloque de énfasis (negro en claro, blanco en oscuro) y no el naranja.

#### Scenario: Botón principal

- **WHEN** una pantalla muestra su acción principal
- **THEN** el botón es negro con texto claro en tema claro y blanco con texto oscuro en tema oscuro

#### Scenario: El acento solo marca estado

- **WHEN** una pantalla muestra un chip de estado «en curso», el paso actual de un stepper o un indicador de progreso
- **THEN** ese elemento usa el naranja, y si lleva texto encima el texto es oscuro

### Requirement: Contraste medido

El texto que pinta un componente SHALL alcanzar 4,5:1 sobre su fondo, y los iconos accionables y las marcas de estado 3:1, en los dos temas.

#### Scenario: Texto secundario

- **WHEN** un componente muestra texto secundario sobre una tarjeta o sobre el fondo de la pantalla
- **THEN** su contraste es de 4,5:1 o más en los dos temas y su color no es `#979797`

#### Scenario: Aviso de error de un campo

- **WHEN** un campo muestra un mensaje de error
- **THEN** el mensaje aparece dentro de la tarjeta del campo, con un contraste de 4,5:1 o más

### Requirement: Componentes planos

Ningún componente SHALL tener sombra ni elevación; la separación entre una tarjeta y el fondo la da solo la diferencia de color.

#### Scenario: Tarjeta sobre el fondo

- **WHEN** una tarjeta se muestra sobre el fondo de la pantalla
- **THEN** no proyecta sombra y sus esquinas tienen radio 24

### Requirement: Tamaños y áreas táctiles

Los botones SHALL medir 60 dp de alto, los botones de icono 44 dp y los chips 34 dp, y todo elemento pulsable SHALL tener un área táctil de al menos 48 dp.

#### Scenario: Chip de filtro

- **WHEN** la persona pulsa a 5 dp por encima del borde visible de un chip
- **THEN** el chip responde

### Requirement: Accesibilidad de los componentes

Los componentes SHALL anunciar su función y su estado a los lectores de pantalla y respetar el tamaño de fuente del sistema.

#### Scenario: Botón de icono

- **WHEN** un lector de pantalla enfoca un botón de icono
- **THEN** lee la descripción de su acción

#### Scenario: Barra inferior

- **WHEN** un lector de pantalla recorre la barra inferior
- **THEN** lee el nombre de cada destino, también de los que solo muestran icono, y cuál está seleccionado

#### Scenario: Fuente grande

- **WHEN** el tamaño de fuente del sistema está al máximo
- **THEN** el texto de los componentes crece y no queda cortado dentro de un alto fijo de tarjeta, fila o stepper
