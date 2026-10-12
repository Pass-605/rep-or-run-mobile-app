## ADDED Requirements

### Requirement: Restaurarse no genera

Si la app se cierra por su cuenta mientras se espera y la pantalla se restaura después, la app SHALL NOT pedir otro plan sin que la persona lo pida. La pantalla SHALL decir que la generación se interrumpió y SHALL ofrecer «Generar otra vez». Una interrupción SHALL NOT contar como plan inválido para el tope de dos. Salir de la pantalla y volver a entrar SHALL generar, como cualquier entrada.

#### Scenario: El proceso muere durante la espera

- **WHEN** el sistema cierra la app con la espera en pantalla y la persona vuelve a ella
- **THEN** no se ha pedido ni guardado ningún plan nuevo, y la pantalla dice que la generación se interrumpió y ofrece «Generar otra vez»

#### Scenario: Generar tras una interrupción

- **WHEN** la persona pulsa «Generar otra vez» tras una interrupción
- **THEN** se pide un plan, una vez

#### Scenario: Volver a entrar a propósito

- **WHEN** tras una interrupción la persona vuelve al resumen y pide generar
- **THEN** se genera con normalidad

## MODIFIED Requirements

### Requirement: Un fallo del servicio se puede reintentar

Si no se pudo generar por la conexión, el tiempo de respuesta, el límite de peticiones o un fallo del servicio, la pantalla SHALL decir cuál de los cuatro fue y SHALL ofrecer «Reintentar». Si el servicio dijo cuánto esperar, la pantalla SHALL decirlo, y «Reintentar» SHALL quedar deshabilitado, enseñando los segundos que faltan, hasta que pasen; entonces SHALL habilitarse. Mientras dure esa espera la app SHALL NOT pedir el plan. Si el servicio no dijo cuánto esperar, «Reintentar» SHALL estar disponible de inmediato.

#### Scenario: Sin conexión

- **WHEN** la generación falla por la red
- **THEN** la pantalla lo dice y ofrece «Reintentar», que vuelve a pedir el plan

#### Scenario: Demasiadas peticiones

- **WHEN** el servicio pide esperar 30 segundos
- **THEN** la pantalla dice que hay que esperar 30 segundos y «Reintentar» está deshabilitado y cuenta hacia atrás

#### Scenario: Pasa la espera

- **WHEN** pasan los segundos que pidió el servicio
- **THEN** «Reintentar» se habilita y vuelve a pedir el plan

#### Scenario: Demasiadas peticiones sin plazo

- **WHEN** el servicio pide esperar sin decir cuánto
- **THEN** «Reintentar» está disponible de inmediato

### Requirement: Un plan inválido ofrece generar otro, no reintentar

Si el plan que volvió no es válido, la pantalla SHALL decir en lenguaje normal que no está completo y que no se ha guardado, y SHALL ofrecer «Generar otra vez», con un texto que deje claro que es una generación nueva y no la repetición de la fallida. La acción SHALL NOT llamarse «Reintentar». Los problemas encontrados SHALL enseñarse todos, debajo y como detalle técnico, sin presentarse como el mensaje para la persona.

#### Scenario: Plan inválido

- **WHEN** el plan generado incumple una regla
- **THEN** la pantalla lo explica en una frase, ofrece «Generar otra vez» y enseña los problemas debajo, rotulados como detalle técnico
