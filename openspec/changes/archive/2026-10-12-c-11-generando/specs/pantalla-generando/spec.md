## ADDED Requirements

### Requirement: Esperar con algo que mirar

Al pedir un plan desde el resumen, la app SHALL enseñar una espera a pantalla completa con un progreso sin porcentaje y un mensaje que cambia mientras dura. Los mensajes SHALL describir la espera y lo que la app hace con la respuesta; SHALL NOT prometer nada sobre el contenido del plan ni contener consejos de nutrición, suplementos o salud. La generación SHALL empezar una sola vez por cada vez que se pide.

#### Scenario: Se pide un plan

- **WHEN** la persona pulsa «Generar plan» con todo completo
- **THEN** ve la espera, con progreso indeterminado y un mensaje que rota, y se pide un plan una vez

#### Scenario: Se gira el dispositivo durante la espera

- **WHEN** la pantalla se recrea mientras se espera
- **THEN** la espera sigue y no se pide otro plan

### Requirement: Al terminar se abre el plan generado

Cuando el plan queda guardado, la app SHALL abrir ese plan y SHALL sacar la espera de la pila: volver atrás no la enseña ni genera de nuevo.

#### Scenario: Generación correcta

- **WHEN** el plan se guarda como borrador
- **THEN** se abre el destino de ese plan, y atrás desde él lleva al resumen

### Requirement: Cancelar detiene la generación

Mientras se espera, la persona SHALL poder cancelar. Cancelar SHALL detener la generación, no solo salir de la pantalla, y SHALL volver al resumen. Salir con el gesto de atrás del sistema SHALL tener el mismo efecto. Tras cancelar SHALL NOT aparecer un plan nuevo, salvo que su guardado ya se hubiera confirmado en el instante de cancelar.

#### Scenario: Cancelar a mitad

- **WHEN** la persona cancela mientras se espera al servicio
- **THEN** vuelve al resumen y no queda ningún plan nuevo guardado

#### Scenario: Atrás del sistema

- **WHEN** la persona sale con atrás mientras se espera
- **THEN** la generación se detiene y no queda ningún plan nuevo guardado

### Requirement: Un fallo del servicio se puede reintentar

Si no se pudo generar por la conexión, el tiempo de respuesta, el límite de peticiones o un fallo del servicio, la pantalla SHALL decir cuál de los cuatro fue y SHALL ofrecer «Reintentar». Si el servicio dijo cuánto esperar, la pantalla SHALL decirlo.

#### Scenario: Sin conexión

- **WHEN** la generación falla por la red
- **THEN** la pantalla lo dice y ofrece «Reintentar», que vuelve a pedir el plan

#### Scenario: Demasiadas peticiones

- **WHEN** el servicio pide esperar 30 segundos
- **THEN** la pantalla dice que hay que esperar 30 segundos y ofrece «Reintentar»

### Requirement: Un plan inválido ofrece generar otro, no reintentar

Si el plan que volvió no es válido, la pantalla SHALL decir que no se ha guardado, SHALL enseñar todos los problemas encontrados y SHALL ofrecer «Generar otra vez», con un texto que deje claro que es una generación nueva y no la repetición de la fallida. La acción SHALL NOT llamarse «Reintentar».

#### Scenario: Plan inválido

- **WHEN** el plan generado incumple una regla
- **THEN** la pantalla enseña los problemas y ofrece «Generar otra vez»

### Requirement: Tope de dos planes inválidos seguidos

Tras dos planes inválidos seguidos, la pantalla SHALL dejar de ofrecer generar y SHALL ofrecer solo volver al resumen, con los problemas del último a la vista. La cuenta SHALL ser de esa visita a la pantalla: no se guarda y empieza de cero al volver a entrar. Un resultado que no sea un plan inválido SHALL ponerla a cero.

#### Scenario: Segundo plan inválido

- **WHEN** tras un plan inválido la persona genera otra vez y también es inválido
- **THEN** la pantalla no ofrece generar, enseña los problemas y ofrece volver al resumen

#### Scenario: Volver a entrar

- **WHEN** tras llegar al tope la persona vuelve al resumen y pide generar de nuevo
- **THEN** la cuenta empieza de cero

### Requirement: Datos que no sirven llevan al resumen

Si el servicio no acepta lo enviado, la pantalla SHALL decir qué falla, SHALL NOT ofrecer ni reintentar ni generar otra vez y SHALL ofrecer «Revisar mis datos», que vuelve al resumen. Si la generación no llegó a enviarse porque faltaba algo, la app SHALL volver al resumen, que es quien dice qué falta.

#### Scenario: El servicio rechaza lo enviado

- **WHEN** la generación falla porque los datos no sirven para generar
- **THEN** la pantalla enseña el motivo y solo ofrece «Revisar mis datos»

#### Scenario: Falta algo

- **WHEN** al generar resulta que falta el consentimiento
- **THEN** la app vuelve al resumen sin enseñar un error propio
