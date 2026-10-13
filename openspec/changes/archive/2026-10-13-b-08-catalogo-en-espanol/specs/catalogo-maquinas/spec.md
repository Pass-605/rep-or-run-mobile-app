## ADDED Requirements

### Requirement: El catálogo se lee en español

La app SHALL mostrar cada máquina del catálogo con su nombre en español, en el inventario y en cualquier pantalla que nombre una máquina marcada desde el catálogo. Una máquina marcada SHALL seguir siendo la misma aunque su nombre cambie: lo que la identifica es su clave del catálogo, que no cambia nunca.

#### Scenario: Inventario recién instalado

- **WHEN** la persona abre el inventario de un gimnasio
- **THEN** las 62 máquinas del catálogo aparecen con su nombre en español

#### Scenario: Máquina marcada

- **WHEN** la persona marca una máquina del catálogo
- **THEN** el aparato de su gimnasio toma el nombre en español de esa máquina

### Requirement: Búsqueda en español y en inglés

La app SHALL encontrar una máquina del catálogo por su nombre en español, por su nombre en inglés tal como lo rotula el gimnasio y por sus alias. El nombre en inglés SHALL NOT mostrarse como nombre de la máquina. Ningún término de búsqueda SHALL pertenecer a dos máquinas distintas del catálogo.

#### Scenario: Nombre en inglés

- **WHEN** la persona escribe «leg press» en el buscador
- **THEN** aparece «Prensa de piernas»

#### Scenario: Nombre en español

- **WHEN** la persona escribe «prensa»
- **THEN** aparece «Prensa de piernas»

### Requirement: Un cambio de texto del catálogo llega a quien ya lo tenía

Al arrancar, la app SHALL dejar el nombre y los términos de búsqueda de cada máquina del catálogo como los trae la versión instalada, también si esa máquina ya estaba cargada, y SHALL dar ese nombre a los aparatos que la persona marcó desde el catálogo. Las máquinas personalizadas, el rango de carga de cualquier aparato y los planes guardados SHALL NOT cambiar.

#### Scenario: Instalación con el catálogo en inglés

- **WHEN** la app se actualiza sobre una instalación que tenía marcada «Leg Press»
- **THEN** el inventario, el resumen y la vista del plan la nombran «Prensa de piernas», y sigue marcada con su mismo rango de carga

#### Scenario: Máquina personalizada

- **WHEN** la persona tiene una máquina propia con un nombre escrito por ella
- **THEN** ese nombre no cambia al arrancar

### Requirement: La petición de generación lleva el nombre visible

La app SHALL enviar, para cada aparato del gimnasio, el mismo nombre que la persona ve. La forma de la petición es la de `../docs/contract/v1/`, que esta capacidad no redefine.

#### Scenario: Máquina del catálogo

- **WHEN** se genera un plan para un gimnasio que tiene marcada «Prensa de piernas»
- **THEN** la petición nombra ese aparato «Prensa de piernas»
