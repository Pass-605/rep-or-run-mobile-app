## 1. Modelo

- [x] 1.1 `PlanId.of(date, version)` y `BlockPreferences.effectiveStartDate(today)` en `:core:model`, con test de `PlanId.of`; la fecha efectiva se prueba desde el resumen y la generación

## 2. Fecha de inicio común

- [x] 2.1 `BuildGenerationSummaryUseCase` recibe `Clock` y `TimeZone` y emite las preferencias con la fecha efectiva; tests
- [x] 2.2 El formulario de preferencias (`feature:plansetup`) usa `effectiveStartDate`

## 3. Contador de versión

- [x] 3.1 `PlanRepository.planIdsOn(date)`: consulta en `PlanDao`, implementación en `:core:data` y test con base en memoria (sin cambio de esquema)

## 4. Caso de uso

- [x] 4.1 `GeneratePlanResult` y `GeneratePlanUseCase`; alta en `domainModule`; KDoc de `AiError` al día con el criterio corregido
- [x] 4.2 Tests: éxito (`DRAFT`, metadatos, id), bloqueos, plan inválido sin guardar y sin segunda llamada, cada `AiError`, `vN` y zona horaria, cancelación durante la espera y con respuesta tardía
- [x] 4.3 En `:core:ai`: el fake de punta a punta con el caso de uso, camino de éxito y modo error de `FakeAiConfig`

## 5. Documentación del repo

- [x] 5.1 README y CHANGELOG

## 6. Verificación

- [x] 6.1 `gradlew.bat :core:domain:allTests`: 160 tests en verde en `jvmTest` y los mismos 160 en `testAndroidHostTest` (19 de `GeneratePlanUseCaseTest`); los de iOS no corren en Windows (`needs:macos`)
- [x] 6.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, tests de todos los módulos, grafo de Koin): BUILD SUCCESSFUL
