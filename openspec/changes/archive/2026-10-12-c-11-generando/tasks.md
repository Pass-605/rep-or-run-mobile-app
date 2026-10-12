## 1. Dominio

- [x] 1.1 KDoc de `AiError.InvalidResponse` y de `GeneratePlanResult.InvalidPlan` corregidos, en su propio commit: un plan inválido admite otra generación; el único definitivo es `InvalidInput`. El test `aRejectedPlanFromTheServiceIsDefinitive` pasa a llamarse `aRejectedPlanFromTheServiceIsNotAskedForAgain`

## 2. Design system

- [x] 2.1 `RepOrRunButtonStyle.Tonal` con su muestra en el catálogo, y las medidas de la espera en `RepOrRunDimens` (`WaitRingSize`, `WaitRingStroke`, `WaitTopSpacing`, `WaitDotSize`)

## 3. Feature

- [x] 3.1 Módulo `:feature:plan` (`settings.gradle.kts`, `build.gradle.kts`, `planModule`)
- [x] 3.2 `GeneratingUiState`, `GenerationFailure`, eventos y efectos; `GeneratingViewModel` sobre `GeneratePlanUseCase`, con cancelación y tope de dos
- [x] 3.3 `GeneratingScreen` y `GeneratingLabels` (mensajes de la espera; título, cuerpo y acción de cada error); textos en `strings.xml`
- [x] 3.4 `PlanDraftScreen`, destino mínimo del plan generado, y `planGraph()` con `navigateToGenerating()` y `navigateToGeneratedPlan()`
- [x] 3.5 Tests del ViewModel con Turbine y tiempo virtual: 18 (pide una vez, espera, abre el plan; cancelar no guarda, cancelar dos veces, cerrar con el plan ya guardado; red y reintento, segundos del límite, no pedir dos veces; plan inválido, `422` del servicio, tope de dos, nada pide tras el tope, salida al resumen, la cuenta se reinicia con otro fallo y al volver a entrar; datos que no sirven; bloqueos)

## 4. Raíz

- [x] 4.1 Borrados `GeneratingPlaceholderScreen`, `GeneratingPlaceholderRoute` y sus tres textos; `onGenerate` abre `:feature:plan`; `planModule` en el grafo de Koin

## 5. Documentación del repo

- [x] 5.1 README y CHANGELOG
- [x] 5.2 Skill `reporrun-pantalla-figma`: los nodos de la 06 (`106:1004` y `115:1395`) en la tabla. Sin corrección medida nueva; el contenedor de «Cancelar» queda anotado en `design.md` para C-01

## 6. Verificación

- [x] 6.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests, incluido el del grafo de Koin): BUILD SUCCESSFUL. Los de iOS no corren en Windows
- [x] 6.2 `:androidApp:assembleDebug` e instalación en el emulador `Medium_Phone` (API 28), en claro: resumen → «Generar plan» → espera con anillo y mensaje → destino del borrador con `ROR-2026-10-12-v1`; atrás desde él lleva al resumen. La rotación, medida en una grabación: primer mensaje 2,1 s, segundo 1,2 s
- [x] 6.3 Cancelar a mitad con «Cancelar»: vuelve al resumen y, pasados 5 s, la tabla `plan` sigue con una fila. Lo mismo con atrás del sistema (en oscuro)
- [x] 6.4 Con `FakeAiConfig(failure = AiError.RateLimited(30))` puesto a mano en `aiModule` y retirado después: «Demasiadas peticiones», «…esperar 30 segundos…» y «Reintentar», que vuelve a la espera y al mismo error
- [x] 6.5 Con `FakeAiConfig(failure = AiError.InvalidResponse(…))`, igual de temporal: primer plan inválido con sus dos problemas y «Generar otra vez»; el segundo, «Dos planes seguidos han vuelto incompletos» y solo «Volver al resumen»; al volver y generar de nuevo la cuenta empieza de cero. La tabla `plan` no cambia
- [x] 6.6 Lo mismo en oscuro: espera, éxito (`v2`), error reintentable, un inválido y dos inválidos. Sin `FATAL EXCEPTION` en logcat
- [x] 6.7 Capturas en `captures/C-11/` (ignorada por git): siete en claro y siete en oscuro. Contraste medido en `design.md`
- [ ] 6.8 Sin recorrer en el dispositivo: sin conexión, tiempo agotado, fallo del servicio, datos que no sirven (`InvalidInput`) y bloqueos; cubiertos por los tests del ViewModel. El `InvalidPlan` que sale de `PlanValidator` (no del servicio) tampoco: el fake no devuelve planes inválidos
- [ ] 6.9 Sin probar: muerte del proceso con la espera en pantalla (ver `design.md`), fuente del sistema grande y lector de pantalla
- [ ] 6.10 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
