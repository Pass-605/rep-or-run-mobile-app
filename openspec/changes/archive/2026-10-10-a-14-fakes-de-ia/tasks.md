## 1. Lectura del contrato

- [x] 1.1 `ContractJson` ignora claves desconocidas; instancia estricta solo en tests para `campo-extra`
- [x] 1.2 Test: una respuesta válida con un campo desconocido se lee

## 2. Ports

- [x] 2.1 `:core:domain`, paquete `port`: `PlanGenerator`, `PlanReviewer`, sus peticiones y resultados, y `AiError`

## 3. Mapeo y ejemplos en `:core:ai`

- [x] 3.1 `:core:ai` depende de `:core:domain` y `:core:model`; `GenerateJsonSourceTask` en `build-logic` y los ejemplos como fuente generada
- [x] 3.2 `PlanDto` → días de dominio acumulando errores; `ReviewResultDto` → `ReviewResult`; petición de dominio → `GeneratePlanRequest`
- [x] 3.3 Tests del mapeo: el ejemplo se mapea entero, varios errores salen juntos, todos los enums cruzan

## 4. Fakes

- [x] 4.1 `FakeAiConfig` y `FakePlanGenerator` con la adaptación a inventario, días y cardio
- [x] 4.2 `FakePlanReviewer`
- [x] 4.3 Tests: plan válido contra un inventario dado (exacto, parcial, ajeno, solo cardio), días, códigos, cardio, latencia, modo error, cancelación; revisión insuficiente y suficiente

## 5. Grafo

- [x] 5.1 `aiModule` y su alta en `composeApp`; `AppGraphTest` en verde

## 6. Documentación del repo

- [x] 6.1 README y CHANGELOG

## 7. Verificación

- [x] 7.1 `gradlew.bat :core:ai:allTests`: BUILD SUCCESSFUL. 59 tests en `jvmTest` (22 de contrato, 18 del fake de generación, 7 del de revisión, 12 del mapeo) y 37 en el host de Android (los de `commonTest`), 0 fallos
- [x] 7.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL, 674 tests, 0 fallos. `AppGraphTest` recorre el grafo con `aiModule`. Los de iOS no corren en Windows
- [x] 7.3 `:androidApp:installDebug` en el emulador `Medium_Phone`: la app arranca con el grafo nuevo, sin excepciones en `AndroidRuntime`
- [ ] 7.4 Generación completa en el emulador sin red: **no se puede todavía**, necesita B-06 (`PlanValidator`) y B-07 (`GeneratePlanUseCase`). Nadie llama aún a los ports
- [ ] 7.5 iOS: todo es `commonMain`; sin compilar para iOS (`needs:macos`)
