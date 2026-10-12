## Context

Tres arreglos sobre la pantalla Generando de `c-11-generando`, pedidos en la revisión. El diseño de la pantalla es el de aquella change; aquí solo va lo que cambia. Todo en `:feature:plan`.

## Goals / Non-Goals

**Goals:**

- Que restaurar la pantalla no guarde un plan que nadie pidió.
- Que «Reintentar» no se pueda pulsar cuando se sabe que va a fallar.
- Que los problemas de validación no se lean como el mensaje de la pantalla.

**Non-Goals:**

- Los de la propuesta.

## Decisions

### La marca de «ya se arrancó» va en `SavedStateHandle`

`GeneratingViewModel` recibe el `SavedStateHandle` de su entrada en la pila. En `init`: sin marca, la pone **antes** de pedir el plan y genera; con marca, no genera y expone `GenerationFailure.Interrupted`.

El `SavedStateHandle` de una entrada de navegación sobrevive a la muerte del proceso y no a sacar la entrada de la pila, que es exactamente la frontera que hace falta: restaurarse no genera, volver a entrar sí. Sustituye a la opción «en `init`» de la change anterior sin cambiar lo demás (un giro sigue sin relanzar: el ViewModel no se recrea).

`Interrupted` es un fallo más del estado, con el pie «Generar otra vez» —tampoco aquí se repite nada: se pide otro plan— y **no toca el contador** de planes inválidos.

Koin lo resuelve solo en `viewModelOf`; el test del grafo lo declara en `extraTypes`, porque no sale del grafo.

**Lo que no cubre, y se acepta:** la marca dice «se arrancó», no «se estaba esperando». Si el proceso muere con un **error** en pantalla, al volver se ve «La generación se interrumpió» en vez de aquel error. Y como el tope de dos vive en memoria, tras llegar al tope una muerte del proceso deja generar otra vez. Guardar el fallo y el contador lo arreglaría; el contador en memoria es una decisión de la tarea («no se guarda»), así que se deja y se anota.

### La cuenta atrás, contra un `Clock` inyectado

`GeneratingUiState` gana `secondsUntilRetry`. Con `RateLimited(retryAfterSeconds)` el ViewModel fija la hora a la que se podrá reintentar (`clock.now() + segundos`) y un bucle actualiza cada segundo lo que falta, **calculándolo contra esa hora**, no restando uno: un tic que llegue tarde no alarga la espera. El `Clock` es el del grafo (`appModule`); en el test es uno que avanza con el tiempo virtual.

El pie se deshabilita y dice «Reintentar en N s». El ViewModel además ignora `RetryClicked` mientras `secondsUntilRetry > 0`: que no se pueda pedir no depende de que el botón esté apagado.

Sin `retryAfterSeconds`, o con cero, no hay cuenta y el pie queda como estaba.

### Los problemas, como detalle

El título («El plan que volvió no está completo») y el cuerpo ya explicaban el caso en lenguaje normal. Lo que los presentaba como mensaje era la tarjeta: rótulo «Qué ha fallado», en acento. Pasa a «Detalle técnico», en `onSurfaceVariant`, con una línea debajo: «Lo que devolvió la comprobación del plan, tal cual. No hace falta entenderlo para seguir.» Los `problems` no se tocan.

## Risks / Trade-offs

- **Muerte del proceso con un error en pantalla o tras el tope** → ver arriba; anotado en la tarjeta.
- **El pie deshabilitado lleva texto útil** («Reintentar en 7 s») con el estilo deshabilitado del design system (38 % de opacidad), que no llega al contraste de texto. El plazo también está en el cuerpo, a contraste completo. Un estado «en espera» propio del botón sería de `:core:designsystem`; no entra aquí.
