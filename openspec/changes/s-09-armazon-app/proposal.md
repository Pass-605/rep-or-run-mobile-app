Cubre la tarea **S-09** de `../docs/backlog/S-base.md` (Armazón de la app: Koin, navegación y patrón UDF).

## Why

`composeApp` es una pantalla fija con un texto. No hay grafo de dependencias, ni navegación, ni un ViewModel del que copiar. Si C-05 arranca así, la primera pantalla absorbe toda esa infraestructura dentro de su hora y media, y las siete siguientes heredan lo que salga con prisa.

## What Changes

- **Koin** entra en el catálogo. Cada módulo con algo que aportar expone su `val xxxModule` (`domainModule`, `databaseModule`, `datastoreModule`, `dataModule`) y `composeApp` arma el grafo y lo arranca en Android e iOS.
- `Clock`, `TimeZone`, los dispatchers y el `IdGenerator` salen del grafo. Los dispatchers viajan en un tipo propio (`AppDispatchers`), no en calificadores de texto.
- **Navigation Compose multiplataforma** con rutas `@Serializable`: `NavHost` raíz en `composeApp` y un `NavGraphBuilder.scaffoldGraph()` que hace de modelo para los de cada feature.
- **Un ViewModel de referencia** con el patrón UDF completo (estado inmutable, evento sellado, efecto de un solo uso, `StateFlow` + `Flow<Effect>`, la UI solo llama `onEvent`), comentado con el porqué de cada decisión, y sus tests con Turbine.
- **Dos destinos de prueba** que navegan entre sí, con un argumento tipado en la ruta. El primero usa casos de uso reales de B-05, así que arrancar la app ejercita el grafo entero (Room, DataStore, reloj, ids).
- Un test que comprueba que el grafo está completo, para que una definición olvidada falle en `check` y no al abrir una pantalla.
- `reporrun.feature` añade a los features las dependencias que el patrón necesita.

## No objetivos

- Ninguna pantalla real: C-05 en adelante. Los destinos de prueba se borran cuando entre la primera.
- Crear `:core:ui` o algún `feature:*`: entran con C-05.
- Barra inferior, pila de pestañas y enlaces profundos.
- `ThemeMode` desde DataStore (criterio abierto de C-04).
- Cargar la semilla del catálogo en el arranque: va con el inventario (C-08).
- Reglas de R8 para release: no hay build de release todavía.

## Capabilities

### New Capabilities

- `armazon-app`: la app arranca, muestra un destino inicial y se mueve entre destinos conservando lo que la persona dejó a medias.

### Modified Capabilities

Ninguna.

## Impact

- **Código:** `composeApp`, `androidApp` (clase `Application`), `core:domain`, `core:data`, `core:database`, `core:datastore` (un archivo `di/` en cada uno, más `AppDispatchers` en `core:domain`) y `build-logic`.
- **Dependencias nuevas en el catálogo:** Koin 4.2.2 (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-test`), Navigation Compose 2.9.2 y Lifecycle 2.9.6 de JetBrains, y Turbine 1.2.1 (tests). Koin y Navigation las manda `CLAUDE.md` §3; Turbine, §7.
- **Entre módulos:** `composeApp` pasa a depender de `core:domain`, `core:data`, `core:database` y `core:datastore`: es la raíz de composición y §3 no la restringe. `core:domain` sigue dependiendo solo de `core:model`.
- **Contrato, Figma y plan:** no se tocan. Los destinos de prueba no salen de Figma; usan componentes y tokens de `:core:designsystem`.
- **iOS:** el arranque de Koin en `iosMain` se escribe, pero no se verifica sin Mac (`needs:macos`).
