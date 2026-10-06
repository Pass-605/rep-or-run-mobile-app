## 1. Dominio

- [x] 1.1 `ValidateProfileUseCase` en `SaveProfileUseCase.kt`, registrado en `domainModule`
- [x] 1.2 Tests del caso de uso: 4, uno de ellos comprueba que coincide con lo que rechaza guardar

## 2. Feature

- [x] 2.1 `:feature:profile` en `settings.gradle.kts`, con `reporrun.feature`
- [x] 2.2 `ProfileUiState`, `ProfileEvent`, `ProfileEffect` y `ProfileViewModel`; limpieza de los campos numéricos en `ProfileFormat.kt`
- [x] 2.3 `ProfileBasicsScreen` y `ProfileGoalScreen` con la tarjeta del objetivo; textos en `strings.xml`; medidas de la tarjeta en `RepOrRunDimens`
- [x] 2.4 `profileGraph()`, `navigateToProfileGoal()` y `profileModule`
- [x] 2.5 Tests del ViewModel con Turbine: 13

## 3. Raíz

- [x] 3.1 `RepOrRunNavHost` con el grafo del perfil; `profileModule` en `appModules()`; el destino con consentimiento es el perfil
- [x] 3.2 Borrar `StartPlaceholderScreen` y sus cuatro textos
- [x] 3.3 `adjustResize` en la actividad de `androidApp`

## 4. Documentación del repo

- [x] 4.1 README (estructura, «Una pantalla nueva») y CHANGELOG

## 5. Verificación

- [x] 5.1 `gradlew.bat :feature:profile:allTests`: 13 tests en verde
- [x] 5.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. `composeApp` 5 (incluido el del grafo, ya con `profileModule`). Los de iOS no corren en Windows
- [x] 5.3 `gradlew.bat :androidApp:installDebug` en el emulador (API 28), con el consentimiento ya dado: abre en el paso 1; «Continuar» con el formulario vacío marca edad, altura y experiencia a la vez; edad 15 dice «Entre 16 y 90»; el teclado de la edad es numérico y la acción queda por encima de él; con datos válidos pasa al paso 2; guardar sin objetivo lo marca y no guarda; el quinto músculo no queda marcado y sale «Elige 4 como máximo»; elegir objetivo enseña su tarjeta con la nota de alimentación; atrás del sistema vuelve al paso 1 con todo lo escrito y al continuar el paso 2 conserva lo elegido; dos toques en «Guardar perfil» guardan una vez; tras forzar el cierre, el perfil se abre precargado en los dos pasos. Sin `FATAL EXCEPTION` en logcat
- [x] 5.4 Capturas en `captures/C-06/` (ignorada por git): nueve en claro y cuatro en oscuro. Contraste medido en los dos temas: `onBackground`/`background` 13,26 y 15,00; `onSurfaceVariant`/`background` 5,04 y 7,39; `onSurface`/`surface` 15,00 y 10,30; `onSurfaceVariant`/`surface` 5,70 y 5,08; `accentText`/`surface` 4,61 y 4,54; botón y chip elegido 15,00; borde y punto de la tarjeta (`primary`/`surface`, no texto, pide 3) 3,41 y 3,02
- [x] 5.5 Con los datos de la app borrados: bienvenida, «Aceptar y empezar» lleva al paso 1 vacío y atrás sale de la app en vez de volver a la bienvenida. Sin `FATAL EXCEPTION`. No se capturaron los errores en tema oscuro: su contraste está medido en 5.4
- [ ] 5.6 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
