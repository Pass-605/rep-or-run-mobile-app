## 1. Una sola definición de `ModelErrors`

- [x] 1.1 `ModelErrors` y `Mapped` públicos en `:core:domain`, paquete `validation`; se borran las copias de `:core:ai` y `:core:data` y se actualizan sus imports

## 2. Validador

- [x] 2.1 `DayOfWeek.codeLetter` en `:core:domain`; el fake deja su tabla privada
- [x] 2.2 `SessionVolume`: estimación y tope de la regla 6
- [x] 2.3 `PlanValidator`, `PlanValidation`, `PlanViolation` y `PlanRule`; alta en `domainModule`
- [x] 2.4 Tests: un caso por regla, los tres de la regla 4, límites de la regla 6, alternativas y unilaterales, varias infracciones a la vez, forma del mensaje

## 3. Fake

- [x] 3.1 `FakePlanGenerator` recorta cada día a `sessionMinutes`
- [x] 3.2 Tests del fake contra `PlanValidator` (sustituye a `assertValidFor`), con 20, 45, 60 y 75 minutos; el ejemplo del contrato pasa con 60

## 4. Documentación del repo

- [x] 4.1 README y CHANGELOG

## 5. Verificación

- [x] 5.1 `gradlew.bat :core:domain:allTests`: 139 tests en verde en `jvmTest` y los mismos 139 en `testAndroidHostTest` (24 de `PlanValidatorTest`); los de iOS no corren en Windows
- [x] 5.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, tests de todos los módulos): BUILD SUCCESSFUL
