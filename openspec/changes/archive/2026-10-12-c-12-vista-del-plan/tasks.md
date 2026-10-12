## 1. Dominio

- [x] 1.1 `PlanUseCases.kt`: `PlanDetails`, `ObservePlanUseCase` (por id y el activo), `ApprovePlanDayUseCase`, `ActivatePlanUseCase`, `HasSavedPlansUseCase`; registrados en `domainModule`
- [x] 1.2 `PlanUseCasesTest`: transiciones, días pendientes, plan o día inexistentes, el activo anterior archivado, el plan con los aparatos de su gimnasio

## 2. Design system

- [x] 2.1 `RepOrRunScreenFrame` y `RepOrRunScrollContent`; `RepOrRunScreenScaffold` montado sobre ellos sin cambiar su firma; `ExerciseIndexSize` en `RepOrRunDimens`
- [x] 2.2 `InventoryScreen`, `NoticeScreen` y `GeneratingScreen` pasan al armazón

## 3. Feature

- [x] 3.1 `PlanContract.kt` y `PlanViewModel` (plan por id o activo, día elegido, hoy, aprobar, activar)
- [x] 3.2 `PlanScreen` y `PlanLabels`; textos en `strings.xml`
- [x] 3.3 `PlanRoute` con id opcional en `planGraph()`; borrados `PlanDraftScreen`, `PlanDraftRoute` y los cinco `plan_draft_*`
- [x] 3.4 `PlanViewModelTest` con Turbine

## 4. Raíz

- [x] 4.1 `StartDestination.Plan` en `AppViewModel` con su test; `RepOrRunNavHost` conecta `onOpenSummary` y `canLeave`

## 5. Documentación del repo

- [x] 5.1 README, CHANGELOG y `CLAUDE.md` si cambia algo de lo que describen
- [x] 5.2 Skill `reporrun-pantalla-figma`: los cuatro nodos de 07 y 07b

## 6. Verificación

- [x] 6.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests, incluido el del grafo de Koin): BUILD SUCCESSFUL. Tests nuevos: 19 de `PlanUseCasesTest`, 20 de `PlanViewModelTest` y 3 más en `AppViewModelTest` (13). Los de iOS no corren en Windows
- [x] 6.2 `:androidApp:assembleDebug` en el emulador `Medium_Phone` (API 28). En claro: resumen → «Generar plan» → vista del plan `ROR-2026-10-12-v1` como borrador, con lunes elegido y marcado «hoy»; aprobar el lunes → «1 de 2 aprobados», «Activar plan» deshabilitado y «Para activarlo falta aprobar: miércoles.»; cerrar y abrir la app → estado vacío. En oscuro: «Preparar un plan» → resumen → generar `v2`, aprobar los dos días, activar → chip «Activo» y sin pie; cerrar y abrir → se abre por el plan activo, con «Preparar otro plan» al final. En la base: `v1` `DRAFT` (un día aprobado), `v2` `ACTIVE` (los dos). Sin `FATAL EXCEPTION` en logcat
- [ ] 6.3 **Con 5 días no se pudo ver.** `FakePlanGenerator` adapta los 3 días del ejemplo del contrato y nunca devuelve más; con 6 días disponibles y 5 máquinas devolvió 2. La fila de chips hace scroll si no caben, pero con 5 a 7 días no se ha visto en el dispositivo
- [ ] 6.4 Sin ver en el dispositivo, porque el plan del fake con ese inventario no los trae: la línea de cardio con datos, la marca «Por lado» y la línea «O elige una». Los textos salen de `PlanLabels`; no tienen test (no hay tests de UI en el MVP)
- [x] 6.5 Armazón: el inventario y la espera recorridos después del cambio (captura `oscuro-00-generando`). **El aviso completo (`NoticeScreen`) no se recorrió**: solo se llega desde la bienvenida y el emulador ya tenía el consentimiento
- [x] 6.6 Capturas en `captures/C-12/` (ignorada por git): siete en claro y ocho en oscuro. Contraste medido en `design.md`
- [ ] 6.7 Sin probar: fuente del sistema grande, lector de pantalla y giro del dispositivo
- [ ] 6.8 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
