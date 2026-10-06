## 1. Lo que hereda de S-09

- [x] 1.1 `:core:ui` con `CollectEffects`, movido desde `composeApp`
- [x] 1.2 `:feature:onboarding` en `settings.gradle.kts`, con `reporrun.feature`
- [x] 1.3 Borrar el paquete `scaffold`, sus tests y sus textos

## 2. Dominio

- [x] 2.1 `ObserveConsentUseCase` y `AcceptConsentUseCase`, en `domainModule`
- [x] 2.2 Tests de los dos casos de uso: 5

## 3. Design system

- [x] 3.1 `RepOrRunWatermark` y las medidas de la bienvenida en `RepOrRunDimens`

## 4. Feature

- [x] 4.1 `OnboardingUiState`, `OnboardingEvent`, `OnboardingEffect` y `OnboardingViewModel`
- [x] 4.2 `WelcomeScreen` (superficie constante) y `NoticeScreen`, con sus textos en `strings.xml`
- [x] 4.3 `onboardingGraph()`, `navigateToNotice()` y `onboardingModule`
- [x] 4.4 Tests del ViewModel con Turbine: 6

## 5. Raíz

- [x] 5.1 `AppViewModel`: destino inicial según el consentimiento, con su test (4)
- [x] 5.2 `RepOrRunNavHost` con el grafo de onboarding y el destino provisional; `onboardingModule` en `appModules()`

## 6. Documentación del repo

- [x] 6.1 `CLAUDE.md` §3: `koin-core` en `core:domain` solo para `di/`
- [x] 6.2 README («Una pantalla nueva», estructura) y CHANGELOG

## 7. Verificación

- [x] 7.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. `core:domain` 77 tests (5 nuevos), `feature:onboarding` 6, `composeApp` 5 (4 de `AppViewModel` y el del grafo). Los de iOS no corren en Windows. La primera pasada falló en `:androidApp:spotlessKotlin` con «Could not read path … mergeDebugResources»: Spotless leyó `build/` mientras otra tarea lo escribía; al repetir no se reprodujo y no es de esta change
- [x] 7.2 `gradlew.bat :androidApp:installDebug` en el emulador `Medium_Phone`, con los datos de la app borrados: arranca en la bienvenida; «Leer aviso completo» abre el aviso y atrás vuelve a la bienvenida sin consentimiento; dos toques seguidos en «Aceptar y empezar» navegan una vez al destino provisional y `consent_accepted_at` queda en DataStore; atrás sale de la app en vez de volver a la bienvenida; tras forzar el cierre, el segundo arranque abre el destino provisional; desde su fila se abre el aviso y se vuelve. Cambiar el tema con el aviso abierto no pierde la pantalla. Sin `FATAL EXCEPTION` en logcat
- [x] 7.3 Capturas en `captures/C-05/` (ignorada por git): bienvenida, aviso y destino provisional, en claro y en oscuro. La bienvenida es idéntica en los dos; con el sistema en claro los iconos de la barra de estado salen oscuros sobre ella (riesgo anotado en el diseño). Contraste medido: bienvenida `onBackground`/`background` 15,0, `onSurfaceVariant`/`background` 7,39, `onSurface`/`surface` 10,3, `accentText`/`surface` 4,54 (icono, pide 3), botón principal 15,0, borde del secundario 5,25; aviso y provisional, texto secundario sobre tarjeta 5,70 en claro y 5,08 en oscuro, sobre el lienzo 5,04 y 7,39
- [ ] 7.4 iOS: sin compilar ni ejecutar (`needs:macos`)
