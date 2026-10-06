## ADDED Requirements

### Requirement: Lista de gimnasios con el activo destacado

La app SHALL mostrar todos los gimnasios de la persona, del más antiguo al más reciente, e indicar cuál es el activo con una marca y con texto. Solo uno está activo.

#### Scenario: Varios gimnasios

- **WHEN** la persona abre los gimnasios y tiene dos guardados
- **THEN** ve los dos por su nombre y solo uno aparece como activo

### Requirement: Estado vacío guiado

Sin ningún gimnasio, la app SHALL explicar qué es un gimnasio en la app, que el plan usa las máquinas del activo y que se empieza por ponerle un nombre, y ofrecer crearlo.

#### Scenario: Primer uso

- **WHEN** la persona abre los gimnasios y no tiene ninguno
- **THEN** ve la explicación y la acción de crear, y ninguna lista

### Requirement: Crear un gimnasio

La app SHALL crear un gimnasio a partir de un nombre. El primer gimnasio queda activo; los siguientes no cambian cuál lo está.

#### Scenario: Primer gimnasio

- **WHEN** la persona crea su primer gimnasio
- **THEN** vuelve a la lista, lo ve en ella y aparece como activo

#### Scenario: Segundo gimnasio

- **WHEN** la persona crea otro gimnasio teniendo ya uno activo
- **THEN** el nuevo aparece en la lista y el activo sigue siendo el de antes

#### Scenario: Dos toques seguidos

- **WHEN** la persona pulsa crear dos veces seguidas
- **THEN** se crea un solo gimnasio

### Requirement: Validación del nombre

La app SHALL rechazar un nombre vacío o de más de 80 caracteres, que es el máximo del contrato, mostrando el motivo junto al campo, y no guardar nada mientras no valga. Los espacios de alrededor no cuentan.

#### Scenario: Nombre vacío

- **WHEN** la persona guarda sin escribir un nombre
- **THEN** sigue en el formulario, el campo dice que falta y no se crea nada

#### Scenario: Corregir

- **WHEN** la persona cambia el nombre después de un error
- **THEN** el error desaparece

### Requirement: Renombrar un gimnasio

La app SHALL permitir cambiar el nombre de un gimnasio. El formulario se abre con el nombre actual. Renombrar no cambia cuál está activo ni nada de lo que cuelga del gimnasio.

#### Scenario: Renombrar el activo

- **WHEN** la persona renombra el gimnasio activo
- **THEN** la lista muestra el nombre nuevo y ese gimnasio sigue activo

#### Scenario: Salir sin guardar

- **WHEN** la persona abre el formulario y vuelve atrás
- **THEN** ve la lista sin cambios

### Requirement: Activar un gimnasio

La app SHALL dejar elegir como activo cualquier gimnasio de la lista. Activar uno desactiva el anterior. No modifica ningún plan.

#### Scenario: Cambiar de activo

- **WHEN** la persona toca un gimnasio que no está activo
- **THEN** ese pasa a ser el activo y el anterior deja de serlo

### Requirement: Los gimnasios siguen al perfil

Al guardar el perfil, la app SHALL llevar a los gimnasios. Al abrirse, la app SHALL empezar por el primer paso que falte: la bienvenida sin consentimiento, el perfil sin perfil guardado y los gimnasios con los dos.

#### Scenario: Tras guardar el perfil

- **WHEN** la persona guarda su perfil
- **THEN** ve los gimnasios y atrás no vuelve al formulario del perfil

#### Scenario: Reabrir con perfil

- **WHEN** la persona cierra la app y la vuelve a abrir con el aviso aceptado y el perfil guardado
- **THEN** la app se abre en los gimnasios, no en el perfil

#### Scenario: Reabrir sin perfil

- **WHEN** la persona vuelve a abrir la app con el aviso aceptado y sin perfil
- **THEN** la app se abre en el perfil
