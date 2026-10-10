## 1. Lectura del contrato

- [ ] 1.1 `ContractJson` ignora claves desconocidas; instancia estricta solo en tests para `campo-extra`
- [ ] 1.2 Test: una respuesta válida con un campo desconocido se lee

## 2. Ports

- [ ] 2.1 `:core:domain`, paquete `port`: `PlanGenerator`, `PlanReviewer`, sus peticiones y resultados, y `AiError`

## 3. Mapeo y ejemplos en `:core:ai`

- [ ] 3.1 `:core:ai` depende de `:core:domain` y `:core:model`; `GenerateJsonSourceTask` en `build-logic` y los ejemplos como fuente generada
- [ ] 3.2 `PlanDto` → días de dominio acumulando errores; `ReviewResultDto` → `ReviewResult`; petición de dominio → `GeneratePlanRequest`
- [ ] 3.3 Tests del mapeo: el ejemplo se mapea entero, varios errores salen juntos, todos los enums cruzan

## 4. Fakes

- [ ] 4.1 `FakeAiConfig` y `FakePlanGenerator` con la adaptación a inventario, días y cardio
- [ ] 4.2 `FakePlanReviewer`
- [ ] 4.3 Tests: plan válido contra un inventario dado (exacto, parcial, ajeno, solo cardio), días, códigos, cardio, latencia, modo error, cancelación; revisión insuficiente y suficiente

## 5. Grafo

- [ ] 5.1 `aiModule` y su alta en `composeApp`; `AppGraphTest` en verde

## 6. Documentación del repo

- [ ] 6.1 README y CHANGELOG

## 7. Verificación

- [ ] 7.1 `gradlew.bat :core:ai:allTests :core:domain:allTests`
- [ ] 7.2 `gradlew.bat check`
- [ ] 7.3 Generación completa en el emulador sin red: **no se puede todavía**, necesita B-06 y B-07
- [ ] 7.4 iOS (`needs:macos`)
