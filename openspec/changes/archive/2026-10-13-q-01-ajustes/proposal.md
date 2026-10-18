Cubre la tarea **Q-01** de `../docs/backlog/Q-entrega.md` (Ajustes, recortada). Es la última del alcance comprometido.

## Why

Tres tareas cerradas dejaron un cabo que solo cierra aquí: `navigateToNotice()` (C-05) no tiene quien la llame con el consentimiento ya dado, `ThemeMode` (C-04) no se guarda en ningún sitio, y el perfil, una vez guardado, solo se reabre desde el resumen previo a generar. Además el OFL de Outfit obliga a incluir su aviso con la fuente, y la app no lo enseña.

## What Changes

- **`:feature:settings`**, módulo nuevo (aprobado en la tarjeta) con `reporrun.feature`: la pantalla de ajustes y la de licencias.
  - Tema **Sistema / Oscuro / Claro**, aplicado al momento y guardado.
  - Fila **Perfil**, que abre el formulario de C-06 y vuelve a Ajustes al guardar.
  - Fila **Aviso y consentimiento**, con la fecha de aceptación, que abre el aviso completo.
  - Fila **Licencias** (Outfit con el texto íntegro del OFL 1.1, y las librerías) y la versión de la app.
- **`:core:model`**: `ThemeMode`. **`:core:domain`**: `SettingsRepository` lo observa y lo guarda; `ObserveThemeModeUseCase` y `SetThemeModeUseCase`. **`:core:datastore`** y **`:core:data`**: la clave y su lectura.
- **`composeApp`**: `AppViewModel` observa el tema y `RepOrRunApp` lo aplica; `settingsGraph()` en la navegación raíz; la versión llega del módulo de plataforma. **`androidApp`**: las barras del sistema siguen al tema elegido.
- **`:feature:plan`**: la vista del plan lleva a Ajustes. **`:core:designsystem`**: `RepOrRunTopBar` admite una acción al final; el texto del OFL pasa a empaquetarse con las fuentes.
- Skill `reporrun-pantalla-figma`: los dos nodos de la pantalla 13.

## No objetivos

- **Gestionar recordatorios**: depende de E-04 (`moved:v1.1`).
- **Borrar todos los datos**: función nueva, y su cláusula de recordatorios también cuelga de E-04. v1.1.
- **Retirar el consentimiento.** Esta versión solo lo enseña. Por eso `MissingConsent` sigue sin poder alcanzarse desde la app y el resumen no necesita todavía una acción para ese motivo (ver `design.md`, decisión 6).
- La fila **«Gimnasios»** y el botón de perfil de la cabecera que dibuja Figma: para C-01.
- Barra inferior, entrada a Ajustes desde otras pantallas que no sean el plan.

## Capabilities

### New Capabilities

- `ajustes`: elegir el tema, reabrir el perfil, releer el aviso, y ver licencias y versión.

### Modified Capabilities

Ninguna. `vista-del-plan` gana una salida, no cambia ningún requisito.

## Impact

- **Dependencias:** ninguna nueva en el catálogo. Un módulo nuevo, `:feature:settings`, dentro de la regla de los `feature:*`.
- **Contrato:** no se toca. **Plan:** no se toca. **Room:** no se toca (el tema va en DataStore).
- **Figma:** no se toca. La pantalla 13 **no dibuja** el selector de tema ni la pantalla de licencias; anotado para C-01.
- **Comportamiento que cambia:** guardar el perfil abierto desde Ajustes vuelve a Ajustes.
- **Textos nuevos, pendientes de revisión:** todos los de las dos pantallas.
