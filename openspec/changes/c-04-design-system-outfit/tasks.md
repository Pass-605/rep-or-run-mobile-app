## 1. Tipografía

- [x] 1.1 Añadir los TTF estáticos de Outfit 400/500/600/700 a `composeResources/font/` y `LICENSE-OFL.txt` al módulo
- [x] 1.2 `repOrRunTypography` con la escala de los estilos de texto de Figma y `RepOrRunExtendedTypography` (cifras y overline)
- [x] 1.3 Aplicar la tipografía en `RepOrRunTheme`

## 2. Tokens

- [x] 2.1 `iconPrimary` en `RepOrRunExtendedColors`
- [x] 2.2 `RepOrRunDimens` y `RepOrRunAlpha` con las medidas y opacidades del diseño
- [x] 2.3 Exportar de Figma los 16 iconos de las pantallas v2 y exponerlos en `RepOrRunIcons`

## 3. Componentes

- [x] 3.1 `RepOrRunCard`, `RepOrRunEmphasisCard`, `RepOrRunDivider`
- [x] 3.2 `RepOrRunListRow` con chevron
- [x] 3.3 `RepOrRunChip` y `RepOrRunStatusChip`
- [x] 3.4 `RepOrRunField`
- [x] 3.5 `RepOrRunStepper`
- [x] 3.6 `RepOrRunSwitch`
- [x] 3.7 `RepOrRunButton` (principal y de contorno) y `RepOrRunIconButton`
- [x] 3.8 `RepOrRunBottomNav`
- [x] 3.9 `LoadingState` y `ErrorState`

## 4. Catálogo y tests

- [x] 4.1 `RepOrRunCatalog` y un preview por componente en los dos temas
- [x] 4.2 Test de contraste de los pares de color que usan los componentes
- [x] 4.3 Tests de la escala tipográfica y de la lógica del stepper

## 5. Verificación

- [x] 5.1 `gradlew.bat :core:designsystem:allTests`: 22 tests en verde (`testAndroidHostTest`; los de iOS no corren en Windows)
- [x] 5.2 `gradlew.bat check` (detekt, Spotless, `checkModuleDependencies`, tests): BUILD SUCCESSFUL
- [x] 5.3 Capturas del catálogo en claro y en oscuro, en el emulador (API 36), para el PR
- [ ] 5.4 Comprobar la carga de las fuentes y los iconos en iOS — needs:macos
