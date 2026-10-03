## 1. Catálogo y build

- [x] 1.1 Koin, Navigation Compose, Lifecycle y Turbine en `gradle/libs.versions.toml`
- [x] 1.2 `reporrun.test` añade Turbine; `reporrun.feature` añade Koin, Lifecycle, Navigation y serialización
- [x] 1.3 Dependencias de `composeApp` (grafo, navegación, recursos) y `koin-core` en los módulos `core` que exponen módulo

## 2. Grafo

- [x] 2.1 `AppDispatchers` en `core:domain`
- [x] 2.2 `domainModule`, `databaseModule`, `datastoreModule` y `dataModule`
- [x] 2.3 `appModule` en `composeApp` (`Clock`, `TimeZone`, dispatchers, base de datos) y `platformModule` de Android e iOS
- [x] 2.4 Arranque en Android: `RepOrRunApplication` en `androidApp`
- [ ] 2.5 Arranque en iOS desde `MainViewController`: escrito, sin compilar ni ejecutar (`needs:macos`, se cierra con S-07)

## 3. Navegación y referencia de UDF

- [x] 3.1 `NavHost` raíz y `scaffoldGraph()` con rutas `@Serializable`
- [x] 3.2 `ScaffoldUiState`, `ScaffoldEvent`, `ScaffoldEffect` y `ScaffoldViewModel`, comentado
- [x] 3.3 `CollectEffects`, los dos destinos de prueba y sus textos en `strings.xml`

## 4. Tests

- [x] 4.1 `ScaffoldViewModel` con Turbine y repositorios fake: 8 tests
- [x] 4.2 El grafo está completo (`verify()`): 1 test, en `androidHostTest`

## 5. Verificación

- [x] 5.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. Los 9 tests de `composeApp` corren en `testAndroidHostTest`; los de iOS no corren en Windows
- [x] 5.2 `gradlew.bat :androidApp:installDebug` en el emulador `Medium_Phone`: arranca en el primer destino; con el nombre vacío el campo muestra «Este dato es obligatorio» y no navega; con nombre, crea el gimnasio y abre el segundo destino con su id; vuelve con el botón de la pantalla y con el atrás del sistema, y el primer destino ya muestra el gimnasio activo. Sin cierres en logcat. El `FeatureConventionPlugin` nuevo compila pero ningún módulo lo aplica todavía
- [x] 5.3 README y CHANGELOG
