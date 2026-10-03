## 1. Base

- [x] 1.1 Port `IdGenerator`, con implementación por defecto sobre `kotlin.uuid.Uuid`
- [x] 1.2 Reglas de captura (`ProfileRules`, `PreferencesRules`, `EquipmentRules`): validación pura de perfil, preferencias y aparato, con sus errores tipados

## 2. Perfil y preferencias

- [x] 2.1 `SaveProfileUseCase` y `ObserveProfileUseCase`
- [x] 2.2 `SavePreferencesUseCase`

## 3. Gimnasios e inventario

- [x] 3.1 `CreateGymUseCase` (el primero queda activo) y `SetActiveGymUseCase` (comprueba que existe)
- [x] 3.2 `AddEquipmentUseCase` (aparato propio)
- [x] 3.3 `ToggleEquipmentUseCase` (catálogo; `InUse` tipado al desmarcar)

## 4. Resumen

- [x] 4.1 `BuildGenerationSummaryUseCase` con `GenerationBlocker`

## 5. Tests

- [x] 5.1 Repositorios fake escritos a mano en `commonTest`
- [x] 5.2 Un test por regla y por caso de uso; test de límites que comprueba que lo que la validación acepta, el constructor de `:core:model` también

## 6. Verificación

- [x] 6.1 `gradlew.bat :core:domain:allTests`: 72 tests en verde en `jvmTest` y los mismos 72 en `testAndroidHostTest` (los de iOS no corren en Windows)
- [x] 6.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, tests): BUILD SUCCESSFUL
