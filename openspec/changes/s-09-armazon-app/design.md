## Context

B-04 y B-05 dejaron repositorios y casos de uso con todo inyectable por constructor (`Clock`, `IdGenerator`, el contexto de las consultas de Room, el scope de DataStore), pero nadie los construye: `RepOrRunApp()` pinta un texto. C-04 dejó los componentes. Faltan las tres piezas que unen eso con una pantalla, y `CLAUDE.md` §3 ya dice cuáles son: Koin, Navigation Compose y UDF.

Lo que se decida en el ViewModel de referencia se copia ocho veces (C-05 a C-12), así que cada decisión de abajo está pensada para una pantalla de formulario típica: datos observados, texto a medio teclear, una acción que guarda y un salto a otra pantalla.

## Goals / Non-Goals

**Goals:**

- Que una pantalla nueva sea: estado, evento, efecto, ViewModel, pantalla y una línea en un módulo de Koin.
- Que un fallo de cableado se vea en `check`, no al abrir la pantalla.
- Que el ViewModel se pruebe sin Compose, sin Koin y sin reloj real.

**Non-Goals:**

- Los de la propuesta.
- Una clase base de ViewModel o un «framework» de MVI: ver decisiones.

## Decisions

**Módulos que se tocan.** `composeApp` (grafo, navegación, referencia), `androidApp` (clase `Application`), `core:domain`, `core:data`, `core:database`, `core:datastore` (su módulo de Koin) y `build-logic` (`reporrun.feature`, `reporrun.test`). No se crea ningún módulo.

**Versiones.** Koin 4.2.2 (última estable). Navigation Compose 2.9.2 y Lifecycle 2.9.6 de JetBrains: la 2.10 de Navigation sigue en beta, y 2.9.6 es el Lifecycle que ya arrastran Compose Multiplatform 1.12.1 y Koin 4.2.2, así que no entra una segunda versión. Turbine 1.2.1.

**Un `val xxxModule` por módulo, también en `core:domain`.** §3 dice que el dominio depende solo de `core:model` «+ coroutines/datetime» y, a la vez, que cada módulo expone su módulo de Koin. Opciones:

1. *`domainModule` definido en `composeApp`*: el dominio no ve Koin, pero cada caso de uso nuevo obliga a tocar otro módulo, y eso se olvida.
2. *`koin-core` en `core:domain`, usado en un único archivo (`di/DomainModule.kt`)*: los casos de uso siguen siendo clases sin anotaciones ni imports de Koin. **Elegida.** No es una dependencia entre módulos del proyecto, así que la regla de `checkModuleDependencies` no cambia.

`core:model` y `core:designsystem` no tienen nada que inyectar y no exponen módulo.

**Los dispatchers, en un tipo.** `AppDispatchers(main, default, io)` en `core:domain`. La alternativa habitual, `named("io")`, es un texto que el compilador no comprueba y que cada módulo tendría que repetir. El grafo crea la única instancia con los `Dispatchers` reales; los tests pasan uno de prueba.

**`Clock` y `TimeZone`.** `Clock.System` como `single`. `TimeZone` como `factory`: la zona del sistema puede cambiar con la app abierta y un `single` la congelaría en el arranque.

**Lo que depende de la plataforma no usa `expect/actual`.** El builder de Room y el `DataStore` necesitan `Context` en Android y nada en iOS. `composeApp` tiene un `platformModule` en `androidMain` y otro en `iosMain`, y el arranque común recibe el de su plataforma como parámetro. `databaseModule` y `datastoreModule` piden esos dos tipos al grafo y no saben de dónde salen. Sin `koin-android`: el `Context` de aplicación entra como parámetro del módulo de Android y no sale de él.

**Arranque.** Android: `RepOrRunApplication` en `androidApp` llama a `startRepOrRun(context)`, de `composeApp`. Arrancar Koin dentro de la composición (`KoinApplication { }`) se descartó: el proceso sobrevive a la actividad y el grafo tiene que hacerlo también. iOS: `MainViewController()` arranca Koin la primera vez, sin tocar Swift.

**El grafo se comprueba en un test.** `Module.verify()` de `koin-test` recorre los constructores y falla si a alguno le falta una definición. Es API de JVM, así que vive en `androidHostTest` de `composeApp`. Usa un sustituto del módulo de plataforma que declara sus dos tipos sin construirlos, así que el módulo de Android real y el de iOS no pasan por el test: los comprueba el arranque.

**UDF sin clase base.** Opciones:

1. *`BaseViewModel<State, Event, Effect>`*: ahorra seis líneas por pantalla y esconde justo lo que hay que entender. No hay dónde ponerla: los features no pueden depender de `composeApp` y `:core:ui` aún no existe.
2. *Patrón explícito, copiado*: tres tipos y un ViewModel que se leen de arriba abajo. **Elegida.**

Dentro del ViewModel:

- **Estado = un `MutableStateFlow` privado que se actualiza en el sitio**, expuesto como `StateFlow`. Lo que viene del dominio se recoge en `init` y se vuelca con `update { copy(...) }`. La alternativa, `combine(dominio, formulario).stateIn(WhileSubscribed)`, es más declarativa y no observa nada sin UI suscrita, pero mete otra corrutina entre la tecla y el campo de texto, y un campo que recibe un valor atrasado mueve el cursor. Seis de las ocho pantallas son formularios: se elige la que no tiene ese riesgo. Coste aceptado: la consulta sigue viva mientras viva el ViewModel, aunque la pantalla esté tapada.
- **Efectos por `Channel` con búfer + `receiveAsFlow()`.** Un efecto se entrega una vez y a un solo colector; si la pantalla está en segundo plano, espera. `SharedFlow` sin réplica lo perdería y con réplica lo repetiría al girar.
- **Los errores esperables son estado, no efectos.** Un nombre inválido se queda en el campo hasta que la persona lo corrige; un efecto es algo que pasa una vez (navegar).
- **El estado guarda el error tipado** (`FieldIssue`), no el texto: el texto lo pone la pantalla desde `strings.xml`, y el ViewModel no conoce recursos.
- **El ViewModel no recibe dispatchers** mientras no haga trabajo propio de CPU: `viewModelScope` ya está en el hilo principal y los casos de uso son seguros de llamar desde ahí (Room corre en el contexto inyectado). En los tests, `Dispatchers.setMain`.

En la UI:

- **`XxxRoute` y `XxxScreen`.** La primera obtiene el ViewModel, recoge el estado con `collectAsStateWithLifecycle` y traduce efectos a lambdas de navegación. La segunda es una función de `(state, onEvent)`, sin ViewModel: es la que se previsualiza y se captura.
- **`CollectEffects`** recoge los efectos solo con la pantalla en `STARTED` y en `Main.immediate`, que es lo que evita perder uno entre el envío y la recogida. Vive en `composeApp`; pasa a `:core:ui` cuando C-05 cree ese módulo.

**Navegación.** Rutas `@Serializable` (`data object` sin argumentos, `data class` con ellos). Cada feature expone `NavGraphBuilder.xxxGraph(...)` y recibe **lambdas**, nunca el `NavController`: un feature no puede conocer las rutas de otro (§3), y solo `composeApp` sabe a dónde lleva cada salida. Los argumentos son primitivos; un id viaja como texto y se envuelve al llegar.

**Los destinos de prueba.** No salen de Figma. Usan `RepOrRunField`, `RepOrRunButton`, `RepOrRunCard`, `RepOrRunListRow` y `RepOrRunIconButton`, con `RepOrRunDimens` y los roles del tema; ningún color ni tamaño propio. El primero crea un gimnasio con `CreateGymUseCase` y muestra lo que falta para generar con `BuildGenerationSummaryUseCase`; el segundo recibe el id por la ruta. Se eligió crear un gimnasio porque es el formulario más pequeño que tiene las cuatro piezas (observar, teclear, guardar con resultado tipado, navegar) y porque pasa por `IdGenerator`, `Clock`, Room y DataStore.

**`reporrun.feature`** añade Koin, Lifecycle, Navigation y el plugin de serialización, para que un feature nuevo no los declare a mano.

## Risks / Trade-offs

- [Los destinos de prueba escriben gimnasios de verdad en la base local] → Solo en instalaciones de desarrollo; se borran con los datos de la app. Desaparecen con C-05.
- [`CollectEffects` queda en `composeApp`, donde un feature no llega] → C-05 lo mueve a `:core:ui` al crear el módulo; es un archivo.
- [Koin falla en ejecución, no al compilar] → El test de `verify()` lo adelanta a `check`.
- [El arranque de iOS no se puede verificar en Windows] → Tarea marcada `needs:macos`; se cierra con S-07.
- [Sin reglas de R8] → Koin por constructores (`singleOf`, `viewModelOf`) y las rutas `@Serializable` pueden necesitarlas en release. Se añaden con la primera build de release, como dice `proguard-rules.pro`.
