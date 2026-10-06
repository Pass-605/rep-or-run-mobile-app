Cubre la tarea **C-05** de `../docs/backlog/C-diseno-ui.md` (Onboarding: bienvenida, aviso y consentimiento).

## Why

La app arranca en dos destinos de prueba. No hay primera experiencia ni forma de dar el consentimiento que `BuildGenerationSummaryUseCase` ya exige para generar (`GenerationBlocker.MissingConsent`): hoy nadie puede quitar ese bloqueo.

## What Changes

- **`:feature:onboarding`**, el primer feature: la pantalla 01 de Figma (bienvenida con el resumen del aviso) y un segundo destino con el aviso completo.
- **Aceptar** guarda en DataStore el instante de la aceptación (decisión de B-04) y lleva al resto de la app. Sin aceptar no se sale de la bienvenida.
- **Solo en el primer arranque:** la raíz decide el destino inicial según haya o no consentimiento guardado.
- **El aviso completo es alcanzable después**, desde fuera del feature, por una función de navegación pública. Ajustes (Q-01) la usará; mientras no exista, la usa un destino provisional de `composeApp`.
- **`:core:ui`**, que `reporrun.feature` ya exige, con `CollectEffects` movido desde `composeApp`.
- Dos casos de uso en `:core:domain`: observar el consentimiento y aceptarlo con el `Clock` del grafo.
- Se borra el paquete `scaffold` de S-09 con sus textos y sus tests. El modelo de pantalla pasa a ser este feature.
- `RepOrRunWatermark` en `:core:designsystem`: la marca de agua de la bienvenida, que también usará la portada del PDF.
- `CLAUDE.md` §3: `core:domain` puede usar `koin-core` solo en su archivo `di/` (resuelto el 05/10 en el cierre de S-09).

## No objetivos

- Ajustes (Q-01, `moved:v1.1`) y retirar el consentimiento desde la UI.
- Perfil y lo que viene después (C-06 en adelante): tras aceptar se llega a un destino provisional.
- `ThemeMode` desde DataStore (criterio abierto de C-04).
- Empaquetar Outfit Black (900) para la marca de agua: se pinta con el 700 que ya está.
- Arreglar Figma: las desviaciones respecto al Anexo A se corrigen en código y se anotan para C-01.

## Capabilities

### New Capabilities

- `onboarding`: bienvenida, aviso y consentimiento antes de que ningún dato salga del teléfono.

### Modified Capabilities

Ninguna. `armazon-app` (S-09) sigue sin archivar; sus destinos de prueba eran provisionales por definición.

## Impact

- **Código:** módulos nuevos `feature:onboarding` y `core:ui` (los dos previstos en el plan §9 y en `CLAUDE.md` §1); `composeApp`, `core:domain`, `core:designsystem`, `settings.gradle.kts`.
- **Dependencias:** ninguna nueva en el catálogo.
- **Contrato:** no se toca.
- **Figma:** no se toca. Se lee la pantalla 01 en las dos páginas v2.
- **Plan:** no se toca.
- **Textos de consentimiento:** los tres puntos de la bienvenida son los de Figma, literales. El aviso completo es texto nuevo y necesita revisión antes de publicar (`CLAUDE.md` del workspace §9).
