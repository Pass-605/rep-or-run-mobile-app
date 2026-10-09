## 1. Dominio

- [x] 1.1 `ObserveGymInventoryUseCase`, `LoadCatalogSeedUseCase`, `UpdateEquipmentUseCase` y `RemoveEquipmentUseCase` en `InventoryUseCases.kt`, registrados en `domainModule`
- [x] 1.2 `CatalogSearch`: normalizar, indexar nombre y alias, casar por palabras
- [x] 1.3 Tests: 13 de los casos de uso y 11 de la búsqueda (acentos, diéresis, ñ, alias, palabras sueltas)

## 2. Design system

- [x] 2.1 `RepOrRunSearchField`, sus medidas y `CardListSpacing` en `RepOrRunDimens`, y su muestra en el catálogo de componentes

## 3. Feature

- [x] 3.1 `InventoryUiState`, `InventoryRow`, `InventoryNotice`, `EquipmentEditorState`, eventos y efectos
- [x] 3.2 `InventoryViewModel`, `EquipmentEditorActions` y las funciones puras de `InventoryEntry.kt` (cruce por clave, filas y cabeceras)
- [x] 3.3 `InventoryScreen` (`LazyColumn` con claves estables) y `EquipmentEditorScreen`; textos en `strings.xml`
- [x] 3.4 `gymGraph()` con el inventario y el formulario de máquina; `navigateToInventory()` y `navigateToEquipmentEditor()`
- [x] 3.5 Lista de gimnasios: «Continuar», fila «Máquinas» y «Añadir gimnasio» como última fila
- [x] 3.6 Tests del ViewModel con Turbine: 32 (filtro, selección, alta, edición, baja, en uso, tope)

## 4. Raíz

- [x] 4.1 `RepOrRunNavHost` conecta las salidas nuevas del grafo
- [x] 4.2 `AppViewModel` carga la semilla del catálogo en cada arranque, fuera del hilo principal; sus tests pasan de 8 a 10

## 5. Documentación del repo

- [x] 5.1 README (estructura, «Una pantalla nueva») y CHANGELOG
- [x] 5.2 Skill `reporrun-pantalla-figma`: sin cambios. No hay séptima corrección medida

## 6. Verificación

- [x] 6.1 `gradlew.bat :feature:gym:allTests`: 48 tests en verde (32 nuevos)
- [x] 6.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, lint y tests): BUILD SUCCESSFUL. `core:domain` 110, `composeApp` 11 (incluido el del grafo), `core:designsystem` 23. Los de iOS no corren en Windows
- [x] 6.3 `gradlew.bat :androidApp:installDebug` en el emulador (API 28), con consentimiento y perfil y sin gimnasios: crear el gimnasio; «Continuar» abre su inventario con el catálogo cargado (por categorías, contador en 0 y el aviso de las 5); marcar tres sube el contador y les pone la fila «Rango de carga»; «maquina pres» encuentra las tres prensas por un alias con acento; mínimo 50 y máximo 10 da el error en el máximo y no guarda; corregido a 100, la fila muestra «50-100 kg»; «+» abre el formulario, y la máquina añadida sale marcada y como personalizada, con «Editar»; atrás vuelve a los gimnasios; tras forzar el cierre la app se abre en los gimnasios con todo guardado. Sin `FATAL EXCEPTION` en logcat
- [x] 6.4 Lo mismo en oscuro: inventario, formulario y lista de gimnasios
- [x] 6.5 Capturas en `captures/C-08/` (ignorada por git): diez en claro y cinco en oscuro. Contraste, con los pares que ya mide `ContrastTest` en los dos temas: `onBackground`/`background` 13,26 y 15,00; `onSurfaceVariant`/`background` (contador, cabeceras, aviso de las 5) 5,04 y 7,39; `onSurface`/`surface` 15,00 y 10,30; `onSurfaceVariant`/`surface` (subtítulos, texto del buscador vacío, chips) 5,70 y 5,08; `accentText`/`surface` (avisos y errores, siempre dentro de tarjeta) 4,61 y 4,54; `inverseOnSurface`/`inverseSurface` 15,00; borde de la marca sin elegir 5,70 y 5,08
- [ ] 6.6 **Aparato en uso por un plan, en el dispositivo**: sin probar. Todavía no hay forma de crear un plan en la app; el aviso está cubierto por tests del ViewModel y de los casos de uso
- [ ] 6.7 **Fluidez con 100 o más filas, medida**: sin cerrar. La estructura es la pedida (`LazyColumn`, claves estables, `contentType`, filtrado fuera de la composición) y hay un test con 182 filas sin claves repetidas, pero el emulador no sirve para medir: `dumpsys gfxinfo` da 200-300 ms por fotograma en el inventario, 200 ms en un formulario corto de la app y 150 ms en los Ajustes del sistema. Hace falta un dispositivo real
- [ ] 6.8 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
