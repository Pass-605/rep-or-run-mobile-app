Cubre la tarea **C-04** de `../docs/backlog/C-diseno-ui.md` (Design system en código).

## Why

`:core:designsystem` tiene desde S-02 la paleta, los dos temas y las formas, pero no la tipografía ni un solo componente. Todas las pantallas de la línea C (C-05 en adelante) dependen de C-04: sin ella cada feature reinventaría la tarjeta, el chip y el botón, con colores y tamaños escritos a mano.

## What Changes

- Tipografía **Outfit** (TTF estáticos 400/500/600/700, OFL 1.1) empaquetada en el módulo y aplicada a toda la escala de Material 3, con los tamaños de los estilos de texto de Figma. Estilos extra para cifras y overline.
- Componentes del lenguaje v2, una vez y para todas las pantallas: tarjeta, bloque de énfasis, separador, fila de lista con chevron, chip de filtro, chip de estado, campo con etiqueta encima, stepper, interruptor, botón (principal y de contorno), botón de icono circular, barra inferior en pastilla, estado de carga y estado de error.
- Los 16 iconos de las pantallas v2, exportados de Figma.
- Token `iconPrimary` (`icon/primary` del Anexo A), que el tema no exponía.
- Medidas del diseño en un único objeto, para que ninguna pantalla escriba un número.
- Catálogo de componentes con previews en los dos temas.
- Test que mide el contraste de los pares de color que usan los componentes.

## No objetivos

- `ThemeMode` y su lectura de DataStore: el Anexo A lo sitúa en Ajustes (Q-01) y toca `core:domain`, `core:datastore` y `core:data`. `RepOrRunTheme` sigue recibiendo `darkTheme`.
- Las superficies constantes (bienvenida, portada y vista previa del PDF): se resuelven en su pantalla con un `RepOrRunTheme(darkTheme = true)` anidado.
- El módulo `:core:ui`: no existe y nada de C-04 lo necesita todavía.
- Buscador, tarjeta de ejercicio y de propuesta: son de una pantalla.
- Tests de UI automatizados (fuera del MVP).

## Capabilities

### New Capabilities

- `design-system`: tema visual y componentes comunes que usan todas las pantallas.

### Modified Capabilities

Ninguna.

## Impact

- **Código:** solo `:core:designsystem`. `composeApp` ya aplica `RepOrRunTheme`.
- **Dependencias:** `components-resources` (ya en uso en `composeApp`) para las fuentes y los iconos, y `ui-tooling-preview` de Compose Multiplatform, solo por la anotación `@Preview`. Sin versiones nuevas en el catálogo.
- **Contrato:** no se toca.
- **Plan:** no se toca.
- **Figma:** no se toca. Quedan anotados hallazgos de contraste para C-01.
