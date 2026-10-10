## 1. Componentes en `:core:designsystem`

- [x] 1.1 `RepOrRunScreenScaffold`: recibe `backContentDescription`, `onBack` nulable y `actionIcon` opcional
- [x] 1.2 `RepOrRunSectionTitle` (con `trailing`), `RepOrRunSupportingText`, `RepOrRunChipGroup` y `RepOrRunSectionError` (`String?`)
- [x] 1.3 Muestra de las piezas de sección en `RepOrRunCatalog`

## 2. Features

- [x] 2.1 `gym`: fuera `GymScaffold`, `SupportingText` y las copias privadas de `EquipmentEditorScreen`; las tres pantallas pasan `gym_back`
- [x] 2.2 `plansetup`: fuera `PlanSetupComponents.kt`
- [x] 2.3 `profile`: `ProfileStepScaffold` queda como envoltorio (título y stepper); el error se traduce con `FieldIssue.message()` antes de pasarlo

## 3. Regla de dependencias

- [x] 3.1 `":core:designsystem" to emptySet()` en `ModuleDependencyRules`, con su test en `build-logic`
- [x] 3.2 `CLAUDE.md` §3

## 4. Documentación

- [x] 4.1 Skill `reporrun-pantalla-figma`: tabla de nodos por pantalla y aviso sobre `get_metadata`
- [x] 4.2 CHANGELOG

## 5. Verificación

- [x] 5.1 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. Sin tests de comportamiento nuevos; ninguno de los existentes se tocó. Los de iOS no corren en Windows
- [x] 5.2 `gradlew.bat -p build-logic test`: BUILD SUCCESSFUL, con el test nuevo de la regla
- [x] 5.3 Emulador `Medium_Phone` (API 28), claro y oscuro: el mismo recorrido con el APK de antes del refactor (`1a2193e`) y con el de después, comparando cada captura píxel a píxel sin la barra de estado. 32 pares, 32 iguales: lista de gimnasios, renombrar, gimnasio nuevo con error, inventario, máquina nueva (arriba, abajo y con error), preferencias (vacías, con errores y abajo), y el perfil desde una instalación limpia (paso 1, paso 1 con errores, paso 2, paso 2 con errores, con objetivo elegido y al final del scroll). Capturas en `captures/S-10/`
- [ ] 5.4 Lector de pantalla: que los títulos de sección del perfil y del formulario de máquinas se anuncien como encabezado no se comprobó con TalkBack; sale del código
- [ ] 5.5 iOS: sin compilar (`needs:macos`)
