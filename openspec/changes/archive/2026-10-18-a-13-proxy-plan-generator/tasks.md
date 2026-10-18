## 1. Dominio y ajustes

- [x] 1.1 `AiBackend` en `:core:model`; `observeAiBackend`/`setAiBackend` en `SettingsRepository`, `SettingsDataSource` (`ai_backend`) y `SettingsRepositoryImpl`, con tests
- [x] 1.2 `ObserveAiBackendUseCase`, `SetAiBackendUseCase`; port `SampleGymSource` y `LoadSampleGymUseCase`, con tests

## 2. `:core:ai`

- [x] 2.1 Dependencia de `:core:network` y `ktor-client-mock` en tests
- [x] 2.2 `ProxyPlanGenerator` con la tabla de errores del design
- [x] 2.3 `SelectablePlanGenerator` y `AiBackendSelection`
- [x] 2.4 `ContractSampleGymSource` desde `ContractExampleSources`
- [x] 2.5 Quitar el recorte del fake (`SessionFit.kt`) y sus tests
- [x] 2.6 `aiModule`: cliente HTTP, proxy, fake y selector

## 3. App

- [x] 3.1 `AppInfo.isDebug` (Android `FLAG_DEBUGGABLE`, iOS `isDebugBinary`); `NetworkConfig` y `AiBackendSelection` con el cerrojo en `composeApp`
- [x] 3.2 Ajustes: sección de desarrollo con el interruptor y el gimnasio de ejemplo, solo con `showDeveloperOptions`

## 4. Tests

- [x] 4.1 `ProxyPlanGeneratorTest` con `MockEngine` y los ejemplos del contrato: `200`, `200` inválido, cada `AiError` (`401` y `404` en `Upstream`), cancelación
- [x] 4.2 `SelectablePlanGeneratorTest`, `SettingsViewModelTest`, grafo de Koin

## 5. Verificación

- [x] 5.1 `gradlew.bat check` en verde. `:core:ai` 92 tests en JVM (21 de `ProxyPlanGeneratorTest`), `:core:domain` 184, `:core:data` 76
- [x] 5.2 Runner de A-09 con `FAKE_OPENAI=true` y `Medium_Phone`: gimnasio real → `422 PLAN_INVALID` (la app enseña «plan no completo» con los `details` y «Generar otra vez»); gimnasio de ejemplo → `200` y borrador `ROR-2026-10-18-v2` con L · X · S. El interruptor sobrevive al reinicio
- [x] 5.3 README, CHANGELOG y backlog
- [ ] 5.4 iOS: `Platform.isDebugBinary` escrito y **sin compilar** (`needs:macos`, S-07)
