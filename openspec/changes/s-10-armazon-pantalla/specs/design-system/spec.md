## ADDED Requirements

### Requirement: Las pantallas de formulario comparten estructura

Una pantalla de formulario SHALL mostrar una barra de arriba con su título, el contenido con desplazamiento vertical y una acción principal fija al pie, con las mismas medidas y separaciones en todas ellas.

#### Scenario: La acción sigue al alcance con el teclado abierto

- **WHEN** la persona abre el teclado en un campo de una pantalla de formulario
- **THEN** la acción del pie queda visible encima del teclado y el contenido sigue desplazándose

#### Scenario: Primer destino sin vuelta

- **WHEN** la pantalla es el primer destino de su flujo
- **THEN** la barra no muestra botón de volver y el título sigue centrado

#### Scenario: Botón de volver

- **WHEN** un lector de pantalla enfoca el botón de volver de una pantalla de formulario
- **THEN** anuncia la descripción que esa pantalla le dio

### Requirement: Los controles de sección son los mismos en todas las pantallas

El título de una sección, su texto de apoyo, su grupo de chips y su error SHALL verse igual en el perfil, los gimnasios y las preferencias del bloque.

#### Scenario: Chips que no caben en una línea

- **WHEN** un grupo de chips no cabe en el ancho de la pantalla
- **THEN** los chips saltan a la línea siguiente con la misma separación entre ellos y entre líneas

#### Scenario: Título de sección con un apunte

- **WHEN** una sección tiene un apunte corto, como el número de elementos elegidos
- **THEN** el apunte va a la derecha del título, en el color del texto secundario

#### Scenario: Título de sección como encabezado

- **WHEN** un lector de pantalla recorre una pantalla de formulario
- **THEN** cada título de sección se anuncia como encabezado

### Requirement: El error de una sección se lee en los dos temas

El error de una sección SHALL mostrarse dentro de una tarjeta, debajo del control al que se refiere, y cumplir el contraste del Anexo A en claro y en oscuro.

#### Scenario: Sección con error

- **WHEN** la persona confirma un formulario con una sección obligatoria sin elegir
- **THEN** bajo esa sección aparece una tarjeta con el motivo

#### Scenario: Sección sin error

- **WHEN** la sección no tiene ningún error
- **THEN** no se muestra ninguna tarjeta ni queda un hueco en su lugar

### Requirement: Unificar los componentes no cambia las pantallas

Las pantallas del perfil, los gimnasios, el formulario de máquinas y las preferencias del bloque SHALL conservar sus textos, su orden, sus errores y sus estados de carga.

#### Scenario: Comparación con las capturas anteriores

- **WHEN** se recorren esas pantallas en claro y en oscuro con los mismos datos que en las capturas de C-06, C-07, C-08 y C-09
- **THEN** cada pantalla se ve igual que en su captura
