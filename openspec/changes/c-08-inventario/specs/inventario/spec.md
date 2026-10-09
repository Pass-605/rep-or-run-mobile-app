## ADDED Requirements

### Requirement: Catálogo agrupado con lo que tiene el gimnasio

La app SHALL mostrar el catálogo de máquinas agrupado por categoría y, dentro de cada grupo, por nombre, indicando cuáles tiene el gimnasio, junto con las máquinas propias de ese gimnasio. Una máquina del catálogo cuenta como marcada por su clave del catálogo, nunca por su nombre.

#### Scenario: Primer uso

- **WHEN** la persona abre las máquinas de un gimnasio recién creado
- **THEN** ve todo el catálogo por categorías, ninguna máquina marcada y el contador en cero

#### Scenario: Mismo nombre, otra máquina

- **WHEN** el gimnasio tiene una máquina propia que se llama igual que una del catálogo
- **THEN** la del catálogo no aparece marcada

### Requirement: Búsqueda por nombre y alias

La app SHALL filtrar la lista según se escribe, buscando en el nombre y en los alias de cada máquina, sin distinguir mayúsculas ni acentos. La ñ SHALL tratarse como una letra distinta de la n. Las palabras de la búsqueda pueden ir en cualquier orden.

#### Scenario: Alias sin acento

- **WHEN** la persona escribe «maquina» y una máquina tiene un alias con «máquina»
- **THEN** esa máquina aparece

#### Scenario: La ñ no es una n

- **WHEN** la persona escribe «pina» y la única coincidencia posible es «piña»
- **THEN** no aparece ninguna máquina

#### Scenario: Sin resultados

- **WHEN** ninguna máquina coincide
- **THEN** la app lo dice y recuerda que se puede añadir una propia

### Requirement: Filtro por categoría

La app SHALL permitir ver una sola categoría o todas. El filtro se combina con la búsqueda.

#### Scenario: Una categoría

- **WHEN** la persona elige una categoría
- **THEN** solo ve las máquinas de esa categoría

### Requirement: Marcar y desmarcar

La app SHALL añadir al gimnasio una máquina del catálogo al tocarla y quitarla al tocarla de nuevo, y mostrar cuántas máquinas tiene el gimnasio. Con menos de las necesarias para generar un plan, SHALL decir cuántas hacen falta.

#### Scenario: Marcar

- **WHEN** la persona toca una máquina sin marcar
- **THEN** queda marcada y el contador sube en uno

#### Scenario: Dos toques seguidos

- **WHEN** la persona toca dos veces seguidas la misma máquina
- **THEN** el gimnasio no la tiene dos veces

### Requirement: Una máquina en uso no se quita

La app SHALL conservar una máquina que usa algún plan guardado, archivados incluidos, y decir junto a ella que no se puede quitar y cuántos ejercicios la usan. No modifica ningún plan.

#### Scenario: Desmarcar una en uso

- **WHEN** la persona toca una máquina marcada que usa un plan
- **THEN** sigue marcada y la app explica por qué

### Requirement: Tope de máquinas

La app SHALL rechazar una máquina más cuando el gimnasio ya tiene 120, que es el máximo del contrato, y decirlo.

#### Scenario: Gimnasio lleno

- **WHEN** la persona marca o añade una máquina con 120 ya en el gimnasio
- **THEN** no se añade y la app dice que es el máximo

### Requirement: Máquina personalizada

La app SHALL permitir añadir una máquina que no está en el catálogo con nombre, categoría y forma de carga, y con rango de carga e incremento opcionales. SHALL rechazar, mostrando el motivo junto al campo, un nombre vacío, demasiado corto, demasiado largo o repetido en el gimnasio, valores fuera de los límites del contrato y un mínimo mayor que el máximo. El rango y el incremento son datos de la máquina, no una prescripción de carga.

#### Scenario: Añadir

- **WHEN** la persona añade una máquina con nombre y tipo válidos
- **THEN** vuelve al inventario, la ve marcada como personalizada y el contador sube

#### Scenario: Datos que no valen

- **WHEN** la persona guarda sin nombre y con el mínimo mayor que el máximo
- **THEN** sigue en el formulario, ve los dos motivos y no se añade nada

### Requirement: Editar una máquina del gimnasio

La app SHALL permitir cambiar el rango de carga y el incremento de cualquier máquina del gimnasio y, si es personalizada, también su nombre. La categoría y la forma de carga SHALL NOT poder cambiarse, y la app SHALL decir por qué: los planes guardados leen los kilos según ellas.

#### Scenario: Rango de una máquina del catálogo

- **WHEN** la persona pone el rango de carga de una máquina marcada y guarda
- **THEN** el inventario muestra ese rango junto a la máquina, que sigue marcada

### Requirement: Quitar una máquina personalizada

La app SHALL permitir quitar del gimnasio una máquina personalizada desde su formulario, con las mismas reglas que desmarcar: si la usa algún plan, se conserva y la app lo dice.

#### Scenario: Quitar

- **WHEN** la persona quita una máquina personalizada que ningún plan usa
- **THEN** vuelve al inventario y la máquina ya no está

### Requirement: Del gimnasio a sus máquinas

Con al menos un gimnasio, la lista de gimnasios SHALL ofrecer continuar a las máquinas del activo y abrir las de cualquier gimnasio. Volver del inventario SHALL llevar a la lista.

#### Scenario: Continuar

- **WHEN** la persona pulsa continuar en la lista de gimnasios
- **THEN** ve las máquinas del gimnasio activo

### Requirement: El catálogo está disponible desde el primer arranque

La app SHALL cargar el catálogo de máquinas al arrancar, sin duplicar entradas en arranques posteriores.

#### Scenario: Instalación limpia

- **WHEN** la persona abre el inventario por primera vez tras instalar
- **THEN** el catálogo tiene máquinas
