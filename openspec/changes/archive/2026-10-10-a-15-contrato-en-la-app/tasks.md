## 1. Script y copia

- [x] 1.1 `scripts/sync-contract.sh`: copia `../docs/contract` a `contract/v1/` con los ejemplos un nivel más abajo; `--check` compara sin escribir; sin la fuente, dice qué falta y dónde se espera
- [x] 1.2 `contract/v1/`: 8 schemas y 14 ejemplos (6 válidos, 7 inválidos y `expected.json`), copiados con el script

## 2. Módulo y DTOs

- [x] 2.1 `:core:ai` en `settings.gradle.kts`, con `reporrun.kmp.library`, `reporrun.test` y el plugin de serialización. Sin dependencias de proyecto y sin `:core:network`
- [x] 2.2 DTOs en el paquete `contract`: enums, objetos comunes, plan, generación, revisión y error, con los nombres de campo de los schemas
- [x] 2.3 `ContractJson` estricto y `CONTRACT_SCHEMA_VERSION`

## 3. Tests

- [x] 3.1 Ida y vuelta de los 6 ejemplos válidos y de `ApiError` (sintético: el contrato no trae ejemplo de error), más los dos casos de `null` (obligatorio se escribe, opcional se omite) y un test que falla si aparece un ejemplo sin cubrir
- [x] 3.2 Los 7 inválidos contra `expected.json`: 5 rechazados en su ruta y por su motivo; los 2 de `minItems`, leídos y con la lista vacía donde se espera

## 4. Documentación del repo

- [x] 4.1 README y CHANGELOG
- [x] 4.2 Skill `reporrun-contrato`: el script ya existe

## 5. Verificación

- [x] 5.1 `gradlew.bat :core:ai:allTests`: BUILD SUCCESSFUL, 20 tests en `jvmTest` (12 de ida y vuelta, 8 de inválidos), 0 fallos
- [x] 5.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. Los de iOS no corren en Windows
- [x] 5.3 `scripts/sync-contract.sh --check`: «contract/v1/ está al día», salida 0. Con un schema alterado a mano: muestra el diff y sale con 1. Con `CONTRACT_SOURCE` apuntando a una ruta que no existe: nombra las dos carpetas que faltan y sale con 1
- [x] 5.4 Clon aislado de la rama, sin `../docs/` al lado: `gradlew.bat :core:ai:allTests` pasa los 20 tests; `sync-contract.sh --check` falla nombrando las carpetas que faltan y, con `CONTRACT_SOURCE` apuntando a la fuente, dice que la copia del clon está al día (los finales de línea sobreviven al checkout)
- [ ] 5.5 iOS: los DTOs son `commonMain`; sin compilar para iOS (`needs:macos`)
