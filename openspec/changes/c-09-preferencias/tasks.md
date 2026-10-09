## 1. Dominio

- [x] 1.1 `ObservePreferencesUseCase` y `ObserveCardioOptionsUseCase`, registrados en `domainModule`
- [x] 1.2 Tests: 5 de las opciones de cardio (solo `CARDIO`, solo el gimnasio activo, sin repetir, recorte a 40)

## 2. Feature

- [x] 2.1 Módulo `:feature:plansetup` con `reporrun.feature`, en `settings.gradle.kts` y en `composeApp`
- [x] 2.2 `PreferencesUiState`, eventos y efecto; `PreferencesViewModel`
- [x] 2.3 `PreferencesScreen` y textos en `strings.xml`
- [x] 2.4 `planSetupGraph()`, `navigateToPlanSetup()` y `planSetupModule`
- [x] 2.5 Tests del ViewModel con Turbine: 19 (carga, precarga, cardio desde el inventario, guardar, errores del caso de uso)

## 3. Inventario y raíz

- [x] 3.1 El pie del inventario es «Continuar» y lleva a las preferencias (`onInventoryContinue` en `gymGraph()`)
- [x] 3.2 `RepOrRunNavHost` conecta el grafo nuevo; `appModules()` incluye `planSetupModule`

## 4. Documentación del repo

- [x] 4.1 README (estructura, «Una pantalla nueva», flujo) y CHANGELOG
- [ ] 4.2 Skill `reporrun-pantalla-figma`: **sin revisar**. No se puede decir si hay séptima corrección sin leer la pantalla 04

## 5. Verificación

- [x] 5.1 `gradlew.bat :feature:plansetup:allTests`: 19 tests en verde
- [x] 5.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests, incluido el del grafo de Koin): BUILD SUCCESSFUL. Los de iOS no corren en Windows
- [x] 5.3 `gradlew.bat :androidApp:installDebug`: instalada en el teléfono conectado, que estaba bloqueado; el recorrido se hizo en el emulador `Medium_Phone` (API 28) con el mismo APK: inventario → «Continuar» → preferencias vacías con «14 días» y sin fecha; guardar vacío da «Marca al menos un día» y «Falta este dato» y no guarda; sin máquinas de cardio la pantalla lo dice; tras marcar tres en el inventario salen como chips; 5 minutos da «Entre 20 y 180»; con 60 guarda y vuelve al inventario; tras forzar el cierre, las preferencias se abren precargadas. Sin `FATAL EXCEPTION` en logcat
- [x] 5.4 Lo mismo en oscuro: inventario y preferencias
- [x] 5.5 Capturas en `captures/C-09/` (ignorada por git): seis en claro y tres en oscuro. Contraste: la pantalla solo usa pares que ya mide `ContrastTest` en los dos temas (`onBackground`/`background`, `onSurfaceVariant`/`background`, `onSurface`/`surface`, `onSurfaceVariant`/`surface`, `accentText`/`surface`, `inverseOnSurface`/`inverseSurface`)
- [ ] 5.6 **Leer la pantalla 04 de Figma en claro y en oscuro y ajustar la composición**: sin hacer, el MCP de Figma estaba sin sesión. La tarea no se cierra sin esto
- [ ] 5.7 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
