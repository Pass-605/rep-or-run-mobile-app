## Context

`:core:designsystem` trae `RepOrRunPalette`, los dos `ColorScheme`, `RepOrRunShapes` y `RepOrRunExtendedColors(accentText)`. El diseño vive en el archivo de Figma `oxGySsrSlyarW78NSmC3aH`, páginas *Pantallas · v2 · Claro* y *· Oscuro*: 14 pantallas hechas con composiciones directas, coloreadas con la colección `Color v2`. Los estilos de texto y los iconos están en la librería *Design System*. La tabla normativa de tokens es el Anexo A del plan.

## Goals / Non-Goals

**Goals:**

- Que una pantalla se pueda construir sin escribir un color, un tamaño de texto ni una medida.
- Que cada patrón repetido exista una sola vez.
- Que el contraste de lo que pintan los componentes esté medido, no supuesto.

**Non-Goals:**

- Los de la propuesta: `ThemeMode`, superficies constantes, `:core:ui`, componentes de una sola pantalla.

## Decisions

**Módulos.** Solo `:core:designsystem`. No hay dependencias nuevas entre módulos.

**Tipografía: la escala sale de Figma, no de Material 3.** Los 13 estilos `Display/Large` … `Label/Small` de la librería se asignan a su hueco de `Typography`; `displayMedium` y `displaySmall`, que Figma no define, conservan el tamaño de Material 3 con el peso de la familia. `Number/*` y `Overline` no tienen hueco en Material 3 y van en `RepOrRunExtendedTypography`, expuesto como `RepOrRunTheme.extendedTypography`, igual que `extendedColors`. La alternativa —meter las cifras en `title*`— obligaba a recordar una convención en cada pantalla.

**`repOrRunTypography(fontFamily)` es una función pura.** Cargar la fuente necesita composición; construir la escala no. Separarlas permite testear pesos y tamaños sin Robolectric.

**`iconPrimary` entra en `RepOrRunExtendedColors`.** El Anexo A tiene `icon/primary` (`#3F3F3F` / `#FFFCF9`) y en claro no coincide con ningún rol de Material 3. `icon/muted` es `outline` e `icon/on-inverse` es `inverseOnSurface`, que ya existen.

**Medidas en `RepOrRunDimens`.** Opciones: constantes privadas en cada componente (más cerca del uso, pero las pantallas necesitan las mismas: margen lateral, separación entre bloques) o un objeto público. Se elige el objeto.

**Iconos: recursos del módulo, no `material-icons`.** El diseño usa 16 iconos, tres de ellos propios (inicio, mancuerna, calendario). Se exportan de Figma como vector drawable en `composeResources/drawable` y se exponen como `RepOrRunIcons.X: Painter`. `material-icons-extended` sería una dependencia nueva de varios megas para trece iconos, y no traería los tres propios. Los componentes reciben `Painter`, así que `Res` no sale del módulo.

**Componentes sobre `Surface` de Material 3, no sobre los componentes de Material 3.** `Button`, `FilterChip`, `Switch` y `NavigationBar` de Material 3 traen elevación tonal, alturas y colores de estado que habría que desactivar uno por uno. `Surface` da la forma, el color de contenido, el ripple y el área táctil mínima de 48 dp sin nada de eso.

**El campo es un `BasicTextField` con `decorationBox`.** La etiqueta, la unidad y el error van dentro de la tarjeta, de modo que toda ella es área de foco. El error va dentro y no debajo porque `accentText` sobre `background` da 4,07:1 en claro.

**El stepper es un `Layout` propio.** Los nodos de los extremos van metidos 24 dp y su texto se centra bajo el nodo sin salirse del ancho, que es lo que dibuja Figma. Con columnas de peso igual los nodos de los extremos quedarían a un sexto del ancho.

**La barra inferior pinta el icono activo en `inverseOnSurface`.** En Figma el icono activo está ligado a `icon/primary`, que sobre el bloque de énfasis en claro casi no se ve; se trata como un error del diseño.

**Separador: `outline` al 20 %.** Es lo que usan las pantallas (`text/muted` con opacidad). El Anexo A nombra `border/subtle` para separadores, pero ninguna pantalla lo usa así.

**Previews: un `@Preview` por componente que lo pinta en los dos temas.** `RepOrRunCatalog` los junta en una pantalla para las capturas.

## Risks / Trade-offs

- **Contraste por debajo de 3:1 en tres elementos no textuales, en claro:** el chevron (2,86), el botón del interruptor apagado (2,53) y el borde del botón de contorno (2,53). Son `#979797` sobre fondo claro, tal como están en Figma. Ninguno es texto ni icono accionable por sí solo, así que no rompen la regla del Anexo A, pero quedan anotados. Alternativa medida si se quiere subir: `onSurfaceVariant`.
- **`RepOrRunCatalog` es público** para poder capturarlo desde la app. No se navega a él; R8 lo elimina en release.
- **El área táctil mínima separa los chips.** Un chip mide 34 dp pero ocupa 48 en el layout; las filas de chips quedan más separadas que en Figma salvo que la pantalla lo compense.
- **Los previews de `commonMain` dependen del IDE.** Android Studio necesita `ui-tooling` en debug para pintarlos; no se añade aquí.
