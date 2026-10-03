## ADDED Requirements

### Requirement: El perfil solo se guarda dentro de los límites

La app SHALL guardar el perfil únicamente si la edad está entre 16 y 90 años, la talla entre 120 y 230 cm, el peso (si se indica) entre 30 y 300 kg, la cintura y la cadera (si se indican) entre 40 y 200 cm, hay experiencia y objetivo elegidos, no hay más de 4 grupos musculares prioritarios y los textos libres caben en los límites del contrato (`../docs/contract/v1/common.schema.json`, `Profile`).

#### Scenario: Perfil válido

- **WHEN** la persona guarda un perfil con todos los datos dentro de los límites
- **THEN** el perfil queda guardado con la fecha del momento y sustituye al anterior

#### Scenario: Edad fuera de rango

- **WHEN** la persona indica 15 o 91 años
- **THEN** el perfil no se guarda y la app indica que la edad está fuera de 16-90

#### Scenario: Varios datos incorrectos a la vez

- **WHEN** la persona deja la talla vacía e indica un peso de 20 kg
- **THEN** el perfil no se guarda y la app señala los dos campos, no solo el primero

#### Scenario: Medidas opcionales vacías

- **WHEN** la persona no indica peso, cintura ni cadera
- **THEN** el perfil se guarda sin esas medidas

### Requirement: Un error de captura nunca cierra la app

Ningún dato introducido por la persona en perfil, gimnasios, inventario o preferencias SHALL provocar un fallo de la app: un dato inválido se rechaza con un motivo que identifica el campo y el límite, y lo ya guardado no cambia.

#### Scenario: Dato inválido

- **WHEN** la persona intenta guardar cualquier formulario con un dato fuera de sus límites
- **THEN** la app sigue en la misma pantalla, señala el campo y conserva lo guardado antes

#### Scenario: El motivo no repite el dato

- **WHEN** la app rechaza un dato de salud
- **THEN** el motivo nombra el campo y el límite, sin incluir el valor introducido

### Requirement: Crear y activar gimnasios

La app SHALL crear un gimnasio con un nombre de 1 a 80 caracteres sin contar espacios sobrantes, y mantener como mucho un gimnasio activo.

#### Scenario: Primer gimnasio

- **WHEN** la persona crea un gimnasio y no hay ninguno activo
- **THEN** el gimnasio nuevo queda activo

#### Scenario: Segundo gimnasio

- **WHEN** la persona crea un gimnasio habiendo ya uno activo
- **THEN** el activo sigue siendo el anterior

#### Scenario: Nombre en blanco

- **WHEN** la persona crea un gimnasio con un nombre vacío o solo de espacios
- **THEN** el gimnasio no se crea

#### Scenario: Activar otro gimnasio

- **WHEN** la persona activa un gimnasio que existe
- **THEN** ese pasa a ser el único activo

#### Scenario: Activar un gimnasio que ya no existe

- **WHEN** se pide activar un gimnasio que no está en la lista
- **THEN** el gimnasio activo no cambia

### Requirement: Inventario desde el catálogo

La persona SHALL poder marcar y desmarcar aparatos del catálogo en un gimnasio. Marcar añade el aparato al inventario con el nombre, la categoría y el tipo de carga del catálogo; desmarcar lo quita.

#### Scenario: Marcar

- **WHEN** la persona marca un aparato del catálogo que su gimnasio no tiene
- **THEN** el aparato aparece en el inventario de ese gimnasio

#### Scenario: Desmarcar

- **WHEN** la persona desmarca un aparato que ningún plan usa
- **THEN** el aparato desaparece del inventario

#### Scenario: Desmarcar un aparato en uso

- **WHEN** la persona desmarca un aparato que usa algún plan, archivado o no
- **THEN** el aparato sigue en el inventario y la app indica que está en uso y en cuántos sitios

#### Scenario: Inventario lleno

- **WHEN** la persona marca un aparato y el gimnasio ya tiene 120
- **THEN** el aparato no se añade

### Requirement: Aparatos propios

La persona SHALL poder añadir un aparato que no está en el catálogo, con nombre de 2 a 80 caracteres, categoría y tipo de carga, y opcionalmente carga mínima, carga máxima, incremento y notas dentro de los límites del contrato (`Equipment`).

#### Scenario: Aparato propio válido

- **WHEN** la persona añade un aparato con nombre, categoría y tipo de carga
- **THEN** el aparato queda en el inventario marcado como propio

#### Scenario: Mínimo mayor que máximo

- **WHEN** la persona indica una carga mínima mayor que la máxima
- **THEN** el aparato no se añade

#### Scenario: Nombre repetido

- **WHEN** la persona añade un aparato con el nombre de otro del mismo gimnasio, aunque cambien las mayúsculas
- **THEN** el aparato no se añade

### Requirement: Preferencias del bloque

La app SHALL guardar las preferencias solo con al menos un día disponible, entre 20 y 180 minutos por sesión, un bloque de 14 días, como mucho 6 modos de cardio de hasta 40 caracteres y un texto de ejercicios a evitar de hasta 400.

#### Scenario: Sin días

- **WHEN** la persona guarda las preferencias sin ningún día disponible
- **THEN** las preferencias no se guardan

#### Scenario: Modos de cardio repetidos o vacíos

- **WHEN** la persona indica «Caminadora», « caminadora » y un modo vacío
- **THEN** se guarda un único modo «Caminadora»

#### Scenario: Preferencias válidas

- **WHEN** la persona guarda días, minutos y cardio dentro de los límites
- **THEN** las preferencias quedan guardadas y sustituyen a las anteriores

### Requirement: Solo se puede generar con los datos completos

La app SHALL permitir generar un plan únicamente cuando hay perfil, gimnasio activo con al menos 5 aparatos, preferencias guardadas y consentimiento aceptado. Mientras falte algo, SHALL decir qué falta.

#### Scenario: Todo completo

- **WHEN** hay perfil, gimnasio activo con 5 aparatos, preferencias y consentimiento
- **THEN** la app permite generar

#### Scenario: Pocos aparatos

- **WHEN** el gimnasio activo tiene 4 aparatos
- **THEN** la app no permite generar e indica que tiene 4 y hacen falta 5

#### Scenario: Sin consentimiento

- **WHEN** todo lo demás está completo pero el consentimiento no está aceptado
- **THEN** la app no permite generar e indica que falta el consentimiento

#### Scenario: Faltan varias cosas

- **WHEN** no hay perfil ni gimnasio activo
- **THEN** la app indica las dos cosas

#### Scenario: El resumen sigue los cambios

- **WHEN** la persona añade el quinto aparato con el resumen abierto
- **THEN** el motivo del inventario desaparece sin recargar la pantalla
