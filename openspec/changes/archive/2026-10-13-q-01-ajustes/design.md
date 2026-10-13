## Context

`RepOrRunTheme(darkTheme = …)` ya resuelve los dos temas y hoy siempre recibe `isSystemInDarkTheme()`. `SettingsRepository` (B-04) guarda el consentimiento y el `installId` en DataStore. C-05 dejó `navigateToNotice()` pública; C-10 dejó la reapertura del perfil (`navigate(ProfileGraphRoute) { launchSingleTop = true }`) y la vuelta al origen leyendo la pila (`popBackToSummary()`).

Figma: **13 · Ajustes**, `108:1009` (claro) / `115:1696` (oscuro).

## Goals / Non-Goals

**Goals:** los cuatro criterios de la tarjeta y dejar dicho qué pasa con `MissingConsent`.

**Non-Goals:** los del proposal.

## Decisions

### 1. Qué se toma de Figma y qué se añade

| De Figma | Aquí |
|---|---|
| Título `Display/Large` | Igual, bajo un botón de volver: Figma la dibuja como pestaña de una barra inferior que la app no tiene |
| Overline de sección + tarjeta con filas (icono en círculo, título, subtítulo, chevron) | `overline` en `onSurfaceVariant`, `RepOrRunCard` + `RepOrRunListRow` + `RepOrRunDivider`. Sin componente nuevo |
| «Perfil · 30 años · intermedio» | Igual, con la edad y la experiencia del perfil guardado |
| «Aviso y consentimiento · Aceptado el dd/mm/aaaa» | Igual |
| «Licencias · Outfit (OFL 1.1) y librerías» | Igual |
| «Rep or Run v0.1.0» | Igual, con la versión real del paquete |

No se copia: «Gimnasios», «Recordatorios», «Borrar todos los datos» y el botón de perfil de la cabecera (duplica la fila).

**Lo que Figma no dibuja:** el selector de tema y la pantalla de licencias. El selector es una sección «Apariencia» con tres `RepOrRunChip` sobre el lienzo, que es donde ese chip está medido. Se descartó una fila que abre un diálogo: es otro destino para tres opciones, y el cambio ya no se vería «al momento» detrás.

### 2. `ThemeMode`: dónde vive cada parte

- `ThemeMode { SYSTEM, DARK, LIGHT }` en `:core:model` (`LocalEnums.kt`: estado local que no viaja por el contrato).
- `SettingsRepository.observeThemeMode()` / `setThemeMode()`, con `ObserveThemeModeUseCase` y `SetThemeModeUseCase`.
- `:core:datastore` guarda **texto** (`theme_mode`) y no conoce el enum: no depende de `:core:model`. `:core:data` traduce; sin valor, o con uno que no reconoce, es `SYSTEM`.
- `AppViewModel` lo **observa** (es lo único que observa: el destino inicial se sigue leyendo una vez) y `RepOrRunApp` lo convierte en `darkTheme`. Como el tema envuelve al `NavHost`, cambiarlo recompone sin perder la pila.

Hasta que DataStore contesta se pinta el tema del sistema: quien eligió el contrario ve el otro lienzo un instante al abrir. Es el mismo instante en el que hoy solo se ve el fondo; no se bloquea el arranque por ello.

**Barras del sistema (Android).** `enableEdgeToEdge()` elige los iconos por el modo del *sistema*; con el tema de la app al contrario quedarían oscuros sobre oscuro. `RepOrRunApp` avisa con `onDarkThemeChange` y `MainActivity` vuelve a llamar a `enableEdgeToEdge` con el estilo que toca. Se descartó un `expect/actual` en `composeApp`: la ventana es de la actividad. iOS no lo conecta todavía (`needs:macos`).

### 3. Entrada: desde la vista del plan

`RepOrRunTopBar` gana un hueco `trailing` (el lado derecho ya reservaba 44 dp) y la vista del plan pone ahí el botón de ajustes. No se añade en más sitios: antes del primer plan la persona está en el alta, que es una cadena, y el resumen usa `RepOrRunScreenScaffold`, cuya firma no merece crecer por esto.

### 4. Volver del perfil: la pila, como en C-10

Se reutiliza la ruta de C-10 tal cual. En `onProfileSaved`:

```
popBackToSettings() || popBackToSummary() || seguir el alta
```

Ajustes va **primero** a propósito. Desde Ajustes no se llega al resumen, así que si Ajustes está en la pila es quien abrió el perfil; en cambio el resumen puede estar más abajo (plan → resumen → generar → plan → ajustes → perfil), y preguntar por él antes cerraría Ajustes y el plan de un salto.

### 5. Licencias y versión

- El OFL exige que su texto acompañe a la fuente. `core/designsystem/LICENSE-OFL.txt` **se mueve** a `composeResources/files/outfit_ofl.txt`, junto a los TTF, y `RepOrRunLicenses.outfit()` lo lee: una sola copia, y es la que va en el paquete. Se descartó copiarlo a `:feature:settings`: dos copias que pueden divergir.
- Las librerías que se distribuyen en la app son todas Apache 2.0; se listan por nombre con su licencia. No se añade un plugin de licencias (dependencia nueva, pide ADR).
- La versión es de la plataforma (`versionName` / `CFBundleShortVersionString`): la aporta el módulo de plataforma como `AppInfo` y la navegación raíz la pasa a `settingsGraph(appVersion = …)`. El feature no sabe de dónde sale.

### 6. `MissingConsent` (traspaso de C-10/C-11)

**Esta versión no permite retirar el consentimiento:** Ajustes lo enseña y abre el aviso, nada más. `SettingsRepository.revokeConsent()` sigue sin caso de uso ni pantalla que lo llame, así que `MissingConsent` sigue siendo inalcanzable desde la app y no se crea el estado sin salida. El riesgo queda cerrado sin código, y la condición queda escrita en la spec: quien añada la retirada tiene que dar al resumen una acción para volver a aceptar.

## Contraste

Medido con la fórmula de la skill, en los dos temas (claro / oscuro):

| Texto | Sobre | Claro | Oscuro |
|---|---|---|---|
| Título (`onBackground`) | `background` | 13,26:1 | 15,00:1 |
| Overline y versión (`onSurfaceVariant`) | `background` | 5,04:1 | 7,39:1 |
| Título de fila (`onSurface`) | `surface` | 15,00:1 | 10,30:1 |
| Subtítulo de fila y texto de licencia (`onSurfaceVariant`) | `surface` | 5,70:1 | 5,08:1 |
| Chip sin elegir (`onSurfaceVariant`) | `surface` | 5,70:1 | 5,08:1 |
| Chip elegido (`inverseOnSurface`) | `inverseSurface` | 15,00:1 | 15,00:1 |
| Icono de fila (`iconPrimary`) | círculo `background` | 9,11:1 | 15,00:1 |

Ningún texto en `#979797` ni en naranja; el chevron y el separador sí usan `outline`, que es su sitio.

## Risks / Trade-offs

- **Destello de tema al abrir** con un tema distinto al del sistema → aceptado; decisión 2.
- **Ajustes solo se alcanza con un plan guardado** → aceptado para el MVP; con el fake se llega en un minuto. C-01 decide la barra inferior.
- **Siete fakes de `SettingsRepository`** crecen dos métodos cada uno → coste de los fakes a mano por módulo, ya asumido.
