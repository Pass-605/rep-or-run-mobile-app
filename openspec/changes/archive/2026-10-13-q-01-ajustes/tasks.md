## 1. Datos del tema

- [x] 1.1 `ThemeMode` en `:core:model`; `SettingsRepository.observeThemeMode()` / `setThemeMode()`; `ObserveThemeModeUseCase` y `SetThemeModeUseCase` en `domainModule`, con test
- [x] 1.2 `SettingsDataSource`: clave `theme_mode` como texto; `SettingsRepositoryImpl` la traduce (sin valor o desconocido → `SYSTEM`); tests de los dos
- [x] 1.3 Los fakes de `SettingsRepository` de los demás módulos

## 2. Design system

- [x] 2.1 `RepOrRunTopBar` con `trailing`
- [x] 2.2 `LICENSE-OFL.txt` → `composeResources/files/outfit_ofl.txt`; `RepOrRunLicenses.outfit()`

## 3. Feature

- [x] 3.1 `:feature:settings`: `build.gradle.kts`, `settings.gradle.kts`, dependencia en `composeApp`
- [x] 3.2 `SettingsContract.kt`, `SettingsViewModel`, `SettingsModule` y `SettingsViewModelTest` con Turbine
- [x] 3.3 `SettingsScreen` y `LicensesScreen`; textos en `strings.xml`
- [x] 3.4 `SettingsNavigation.kt`: `settingsGraph()`, `navigateToSettings()`, `navigateToLicenses()`, `popBackToSettings()`

## 4. Raíz

- [x] 4.1 `AppViewModel` observa el tema, con test; `RepOrRunApp` lo aplica y avisa con `onDarkThemeChange`; `MainActivity` ajusta las barras del sistema
- [x] 4.2 `AppInfo` en los módulos de plataforma; `RepOrRunNavHost` conecta `settingsGraph`, la vuelta del perfil y `onOpenSettings` de la vista del plan

## 5. Documentación del repo

- [x] 5.1 README, CHANGELOG y `CLAUDE.md` si cambia algo de lo que describen
- [x] 5.2 Skill `reporrun-pantalla-figma`: los dos nodos de la pantalla 13

## 6. Verificación

- [x] 6.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests, incluido el del grafo de Koin): BUILD SUCCESSFUL. Tests nuevos: 12 de `SettingsViewModelTest`, 2 de `ThemeUseCasesTest`, 4 más en `AppViewModelTest` (17), 3 en `SettingsRepositoryImplTest` (6) y 3 en `SettingsDataSourceTest` (8). Hizo falta `-Pkotlin.incremental=false`: la compilación incremental de Kotlin falló con un error interno (`FqNames can't be derived from DirtyData`) ajeno al cambio
- [x] 6.2 Emulador `Medium_Phone` (API 28), con el sistema en claro: plan generado → Ajustes; «Oscuro» cambia la pantalla y los iconos de la barra de estado sin reiniciar; el aviso se abre y vuelve a Ajustes; licencias con el OFL íntegro; perfil con la edad de 30 a 31 → al guardar vuelve a **Ajustes** (con el resumen más abajo en la pila) y la fila dice «31 años»; atrás → vista del plan; cerrar y abrir la app → sigue en oscuro; de vuelta a «Claro» y a «Sistema». Versión «Rep or Run v0.1.0». Nueve capturas en `captures/Q-01/` (ignorada por git)
- [ ] 6.4 Sin probar: «Sistema» con el dispositivo cambiando de claro a oscuro con la app abierta, fuente del sistema grande, lector de pantalla y giro
- [ ] 6.5 El texto del OFL se enseña con los saltos de línea del archivo original: se lee, pero corta las líneas a media frase en pantallas estrechas
- [ ] 6.3 iOS: `iosMain` escrito (`AppInfo`), sin compilar ni ejecutar; las barras del sistema no siguen todavía al tema elegido (`needs:macos`)
