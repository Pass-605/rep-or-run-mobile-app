## Context

S-09 dejó el grafo, la navegación raíz y un ViewModel de referencia dentro de `composeApp`. B-04 dejó `SettingsRepository` con el consentimiento como `Instant?`, y B-05 hizo que `BuildGenerationSummaryUseCase` lo exija. Falta la pantalla que lo pide. El cierre de S-09 lista lo que esta tarea hereda: crear `:core:ui`, mover `CollectEffects`, borrar `scaffold` y registrar el módulo del feature.

Diseño: pantalla **01 · Bienvenida** del archivo `oxGySsrSlyarW78NSmC3aH`, nodos `104:836` (v2 · Claro) y `115:1109` (v2 · Oscuro). Son idénticas: es superficie constante (Anexo A).

## Goals / Non-Goals

**Goals:**

- Que el consentimiento se pueda dar, se guarde y decida el destino inicial.
- Que `:feature:onboarding` sea el modelo que copian C-06 a C-12, ya como módulo de feature real.

**Non-Goals:**

- Los de la propuesta.

## Decisions

**Módulos.** Nuevos: `:feature:onboarding` (`reporrun.feature`) y `:core:ui` (`reporrun.kmp.compose`). Tocados: `composeApp`, `core:domain`, `core:designsystem`. Dependencias entre módulos: `composeApp` → `feature:onboarding`; `feature:onboarding` → las cuatro que permite §3. `:core:ui` no depende de ningún módulo del proyecto. Ninguna sale de §3, así que no hay ADR.

**Patrón UDF: el de S-09, tal cual.** `OnboardingUiState` / `OnboardingEvent` / `OnboardingEffect`, estado en un `MutableStateFlow` actualizado en el sitio, efectos por `Channel` con búfer, `onEvent` como única entrada, sin clase base. Los comentarios que explicaban cada decisión en `ScaffoldViewModel` se conservan en `OnboardingViewModel`, que pasa a ser la referencia.

**Dos casos de uso nuevos.** El ViewModel solo recibe casos de uso, nunca un repositorio:

- `ObserveConsentUseCase`: `Flow<Instant?>`.
- `AcceptConsentUseCase`: sella la aceptación con el `Clock` del grafo. Si ya había una, conserva la primera fecha: aceptar dos veces no reescribe cuándo se consintió.

**Quién decide el destino inicial.** Opciones:

1. *La bienvenida siempre es el inicio y navega sola si ya hay consentimiento*: se ve un fotograma de bienvenida en cada arranque.
2. *La raíz lee el consentimiento una vez y elige el `startDestination`*: **elegida.** `AppViewModel` en `composeApp` toma el primer valor de `ObserveConsentUseCase` y expone `StartDestination` (`Loading` / `Onboarding` / `Main`). Mientras carga se pinta solo el fondo. Se lee una vez y no se observa: si el `startDestination` cambiara al aceptar, `NavHost` reconstruiría el grafo y competiría con la navegación que dispara el efecto.

**Navegación del feature.** `OnboardingGraphRoute` pública; las rutas de dentro, `internal`. `onboardingGraph(onConsentAccepted, onReadNotice, onBack)` recibe lambdas. `NavController.navigateToNotice()` es pública: es la entrada que usará Ajustes. Al aceptar, `composeApp` navega al destino principal sacando el grafo de onboarding de la pila, para que atrás no vuelva a la bienvenida.

**El aviso completo no tiene ViewModel.** Es texto fijo con un botón de volver, como el segundo destino de S-09. Se acepta desde la bienvenida, no desde el aviso: un solo sitio donde se consiente.

**Destino provisional tras aceptar.** No hay perfil hasta C-06. `composeApp` añade un destino mínimo (`StartPlaceholderScreen`) con una fila que abre el aviso; demuestra que es alcanzable fuera del primer arranque. Se borra con C-06, igual que `scaffold` se borra aquí.

**Superficie constante.** La bienvenida envuelve su contenido en `RepOrRunTheme(darkTheme = true)`, la primera de las dos formas que admite `CLAUDE.md` §6. Todos los fills del diseño caen en roles del tema oscuro:

| Figma | Rol en el tema anidado |
|---|---|
| `bg/inverse` `#252525` (fondo) | `background` |
| `palette/gray-800` `#3F3F3F` (tarjeta) | `surface` → `RepOrRunCard` |
| `text/on-inverse` `#FFFCF9` | `onBackground` / `onSurface` |
| `text/on-secondary` `#FDFDFD` (marca de agua) | `onBackground` con opacidad; no es un rol y difiere de `#FFFCF9` en dos unidades por canal, invisibles al 3-8 % |
| borde `#979797` del botón secundario | `outline` → `RepOrRunButton(Outline)` |

El aviso completo **no** es superficie constante: sigue el tema de la app.

**Tres desviaciones de Figma, corregidas en código** (manda el Anexo A):

1. *Subtítulo en `text/muted` (`#979797`)*: es el caso medido en C-04. Aquí, sobre `#252525`, mide 5,25:1 y pasaría el umbral, pero la regla no depende del fondo: `#979797` no es color de texto. Va `onSurfaceVariant`, que en el tema anidado es `#B4B4B4` (7,39:1).
2. *«Aceptar y empezar» en `bg/accent`*: la acción principal nunca es naranja. Va `RepOrRunButton(Primary)`, que es `inverseSurface`: blanco sobre la superficie oscura. **No estaba en las «Correcciones medidas» de la skill**; se anota para C-01.
3. *Checks en `text/accent` con el frame en modo Light* (`#DA330E` sobre `#3F3F3F`, 2,24:1): el token es correcto, el modo no. En el tema anidado `extendedColors.accentText` resuelve a `#F48F78` (4,54:1).

**Marca de agua.** Componente `RepOrRunWatermark` en `:core:designsystem`, porque la portada del PDF la reutiliza. Seis filas de «REP OR RUN» a 200 dp, alternando relleno (3 % efectivo) y contorno de 2 dp (8 % efectivo), con los desplazamientos del componente de Figma. Figma usa Outfit Black en las filas rellenas; solo se empaquetan 400-700 y añadir un peso es un cambio de assets que no decide esta tarea: se pinta en 700. El tamaño va en dp convertidos a sp, para que no crezca con la fuente del sistema: es decoración, no texto. Sin semántica.

**Tipografía y medidas.** Estilos del tema: `titleMedium` (marca), `extendedTypography.overline`, `displayLarge` (titular), `bodyMedium` (subtítulo), `labelMedium` y `bodySmall` (tarjeta). Las medidas propias de la pantalla entran en `RepOrRunDimens`. El botón mide 60 dp (Anexo A), no los 56 del frame. El contenido hace scroll: con fuente grande o pantalla baja el botón de aceptar tiene que seguir siendo alcanzable.

**Textos.** En `composeResources/values/strings.xml` del feature. Los tres puntos de la tarjeta son los de Figma. El aviso completo dice qué se envía según `../docs/contract/v1/` (`generate-request` y `review-request`), sin redefinirlo: perfil, inventario del gimnasio activo, preferencias y, al revisar, el plan y los check-ins. No promete nada sobre lo que hace OpenAI con los datos.

**`CLAUDE.md` §3.** `core:domain` → `core:model` (+ coroutines/datetime) y `koin-core` solo para `di/`; ningún otro archivo del módulo importa Koin.

## Risks / Trade-offs

- [El aviso completo es texto de consentimiento nuevo, escrito sin revisión legal] → Marcado en el PR como pendiente de revisar antes de cualquier publicación.
- [Tras aceptar se llega a un destino provisional con un texto de desarrollo] → Se borra con C-06.
- [«Accesible desde Ajustes» no se puede cumplir literalmente: Ajustes es Q-01, en v1.1] → La entrada pública existe y se ejercita desde el destino provisional.
- [Con el sistema en claro, los iconos de la barra de estado salen oscuros sobre la bienvenida oscura] → Lo decide `enableEdgeToEdge()` en `androidApp` según el tema del sistema; ajustarlo por pantalla es código de plataforma y queda anotado.
- [Si la escritura en DataStore falla, la excepción llega al ViewModel] → Igual que en el resto de casos de uso de B-05; no se introduce aquí un tratamiento distinto.
- [La regla «Koin solo en `di/`» no la comprueba nada] → `checkModuleDependencies` solo mira dependencias de proyecto. Queda como la dejó el cierre de S-09.
- [iOS] → `iosMain` no cambia; el feature es todo `commonMain`. Sin verificar en Mac (`needs:macos`).
