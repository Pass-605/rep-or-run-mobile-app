## 1. Dominio

- [x] 1.1 `RenameGymUseCase` y `ObserveGymsUseCase` en `GymUseCases.kt`, registrados en `domainModule`; la regla del nombre, compartida con `CreateGymUseCase`
- [x] 1.2 Tests de los casos de uso: 5 (renombrar, nombre vacío, nombre largo, gimnasio que no existe, observar con el activo marcado)

## 2. Design system

- [x] 2.1 `RepOrRunCheckMark` con sus medidas en `RepOrRunDimens`, en el catálogo y en `ContrastTest`
- [x] 2.2 `RepOrRunTopBar`; `feature:profile` deja su barra privada y usa esta

## 3. Feature

- [x] 3.1 `:feature:gym` en `settings.gradle.kts`, con `reporrun.feature`
- [x] 3.2 `GymUiState`, `GymEditorState`, `GymEvent`, `GymEffect` y `GymViewModel`
- [x] 3.3 `GymListScreen` (lista y estado vacío) y `GymEditorScreen` (crear y renombrar); textos en `strings.xml`
- [x] 3.4 `gymGraph()`, `navigateToGymEditor()` y `gymModule`
- [x] 3.5 Tests del ViewModel con Turbine: 16

## 4. Raíz

- [x] 4.1 `RepOrRunNavHost` con el grafo de gimnasios; `onProfileSaved` lleva a él; `gymModule` en `appModules()`
- [x] 4.2 `AppViewModel` elige el inicio con consentimiento y perfil (`Onboarding`, `Profile`, `Gyms`); sus tests pasan de 4 a 8

## 5. Documentación del repo

- [x] 5.1 README (estructura, «Una pantalla nueva») y CHANGELOG
- [x] 5.2 Skill `reporrun-pantalla-figma`: sexta corrección medida

## 6. Verificación

- [x] 6.1 `gradlew.bat :feature:gym:allTests`: 16 tests en verde
- [x] 6.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. `composeApp` 9 (incluido el del grafo, ya con `gymModule`), `feature:profile` 13, `core:designsystem` 23. Los de iOS no corren en Windows
- [x] 6.3 `gradlew.bat :androidApp:installDebug` en el emulador (API 28), con el consentimiento dado y sin perfil: abre en el perfil; guardarlo lleva a los gimnasios, vacíos; «Crear gimnasio» abre el formulario; guardar sin nombre dice «Ponle un nombre» y no crea nada; al escribir, el error se va y la acción queda por encima del teclado; dos toques seguidos crean un solo gimnasio, que sale activo y sin los espacios de alrededor; el segundo no cambia el activo; tocarlo lo activa y desactiva el otro; «Renombrar» abre el formulario con el nombre, atrás lo cierra sin cambios y guardar cambia el nombre sin tocar el activo; tras forzar el cierre la app se abre en los gimnasios, con el nombre nuevo y el mismo activo. Sin `FATAL EXCEPTION` en logcat
- [x] 6.4 Con los datos de la app borrados y el sistema en oscuro: bienvenida, perfil, guardar, gimnasios vacíos; atrás sale de la app en vez de volver al perfil
- [x] 6.5 Capturas en `captures/C-07/` (ignorada por git): diez en claro y cuatro en oscuro. Contraste medido en los dos temas: `onBackground`/`background` 13,26 y 15,00; `onSurfaceVariant`/`background` 5,04 y 7,39; `onSurface`/`surface` 15,00 y 10,30; `onSurfaceVariant`/`surface` 5,70 y 5,08; `accentText`/`surface` 4,61 y 4,54; botón y marca elegida (`inverseOnSurface`/`inverseSurface`) 15,00; borde de la marca sin elegir (`onSurfaceVariant`/`surface`, pide 3) 5,70 y 5,08; chevron (`outline`/`surface`, decorativo) 2,86 y 3,61
- [ ] 6.6 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
