## 1. Semilla

- [x] 1.1 `seed/equipment_catalog.json`: `name` en español y `nameEn` en las 62 entradas, sin tocar ninguna `key`; `catalogVersion` 2
- [x] 1.2 `seed/README.md`: formato, reglas y lo que comprueba el test

## 2. Lectura y carga

- [x] 2.1 `SeedCatalog`: `nameEn` obligatorio y único entre entradas, `name` dentro de `Equipment.NAME_LENGTH`, alias opcionales; el inglés sale como alias
- [x] 2.2 `EquipmentDao`: `upsertCatalog` en lugar de `insertCatalog` y `syncCatalogNames`; `EquipmentCatalogRepositoryImpl.loadSeed` los llama
- [x] 2.3 KDoc de `EquipmentCatalogItem`, `CatalogSearch`, `EquipmentCatalogRepository` y `LoadCatalogSeedUseCase`

## 3. Tests

- [x] 3.1 `SeedCatalogTest`: el campo nuevo (falta, vacío, repetido en otra entrada), el nombre que no cabe, entradas sin alias y la semilla empaquetada encontrada por los dos idiomas
- [x] 3.2 `GymDaoTest` y `EquipmentCatalogRepositoryImplTest`: la recarga cambia el texto de una clave conocida y el nombre de los aparatos marcados, no el de los personalizados

## 4. Verificación

- [x] 4.1 `./gradlew :core:data:allTests :feature:gym:allTests` y `./gradlew check`
- [x] 4.2 Inventario en el emulador: los 62 en español y el buscador en los dos idiomas
- [x] 4.3 `CHANGELOG.md`
