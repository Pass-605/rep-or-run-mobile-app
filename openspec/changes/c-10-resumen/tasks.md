## 1. Design system

- [x] 1.1 `RepOrRunStatCard` (cifra, unidad opcional, etiqueta, destacada o no), sus dos medidas en `RepOrRunDimens` y su muestra en `RepOrRunCatalog`

## 2. Feature

- [x] 2.1 `SummaryUiState`, evento, efecto y `SummaryActions`; `SummaryViewModel` sobre `BuildGenerationSummaryUseCase`, registrado en `planSetupModule`
- [x] 2.2 `SummaryScreen` y `SummaryLabels` (líneas de perfil, gimnasio y semana; motivos de bloqueo); textos en `strings.xml`
- [x] 2.3 `planSetupGraph()` con el resumen; `navigateToSummary()`, `navigateToPreferences()` y `popBackToSummary()`
- [x] 2.4 Preferencias: pie «Continuar» con flecha y 60 minutos por defecto
- [x] 2.5 Tests del ViewModel con Turbine: 8 del resumen (carga, datos, todos los bloqueos, sin gimnasio, deja de bloquear, vuelve a bloquear, pedir generar, no pedirlo con algo pendiente); 3 de las preferencias ajustados al valor por defecto

## 3. Perfil y raíz

- [x] 3.1 `profileGraph(canLeave = …)`: el paso 1 tiene botón de volver cuando hay a dónde
- [x] 3.2 `RepOrRunNavHost`: las tres salidas preguntan por el resumen en la pila; las tres filas del resumen abren su formulario
- [x] 3.3 Destino provisional `GeneratingPlaceholderScreen` en `composeApp`, con sus tres textos. Lo borra C-11

## 4. Documentación del repo

- [x] 4.1 README y CHANGELOG
- [x] 4.2 Skill `reporrun-pantalla-figma`: los nodos de la 05 (`106:937` y `115:1344`) en la tabla. **No hay séptima corrección**

## 5. Verificación

- [x] 5.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests, incluido el del grafo de Koin): BUILD SUCCESSFUL. Los de iOS no corren en Windows
- [x] 5.2 `gradlew.bat :androidApp:installDebug` y recorrido en el emulador `Medium_Phone` (API 28), en claro: inventario con 4 máquinas → «Continuar» → preferencias con 60 marcado → dos días → «Continuar» → resumen con «Falta algo antes de generar», el motivo «tu gimnasio tiene 4 y hacen falta 5» y «Generar plan» deshabilitado; fila del gimnasio → inventario → marcar una → «Continuar» → resumen con 5 y «Todo listo»; fila del perfil → paso 1 precargado y con botón de volver → paso 2 → cambiar el objetivo → guardar → resumen con el objetivo nuevo; fila de la semana → 75 minutos → «Continuar» → resumen con 75; abrir la semana y volver atrás deja en el resumen; «Generar plan» → destino provisional → vuelve al resumen; atrás desde el resumen lleva al inventario, no a las preferencias. Sin `FATAL EXCEPTION` en logcat
- [x] 5.3 Lo mismo en oscuro: resumen bloqueado, las tres ediciones y el destino provisional
- [x] 5.4 Capturas en `captures/C-10/` (ignorada por git): seis en claro y tres en oscuro. Contraste medido en `design.md`, «Lectura de Figma»
- [ ] 5.5 Sin recorrer en el dispositivo: el resumen sin perfil, sin gimnasio activo, sin preferencias o sin consentimiento (el alta no deja llegar así; cubierto por los tests del ViewModel) y la tarjeta con más de un motivo
- [ ] 5.6 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
