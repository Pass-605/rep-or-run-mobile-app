# CLAUDE.md: rep-or-run-mobile-app

Repo de la **app KMP (Android + iOS)**. Lee también el `CLAUDE.md` del workspace (`../CLAUDE.md`): dominio, contrato, seguridad y flujo de trabajo comunes. La documentación está en `../docs/` (local, fuera de git).

## 0. Cómo se trabaja aquí: OpenSpec

Este repo usa **spec-driven development con OpenSpec** (`schema: spec-driven`). El reparto:

- **`../docs/backlog/` es la hoja de ruta:** los IDs, el orden, las dependencias y el estado.
- **`openspec/changes/<id>-<slug>/` es la unidad de ejecución:** proposal, design, specs y tasks de *un* cambio. El nombre empieza por el ID del backlog para poder cruzarlos.
- **`openspec/config.yaml` inyecta el contexto** en todo artefacto que genere OpenSpec. Apunta a las fuentes de verdad; no las copia. Si añades una fuente nueva, va ahí.
- **`openspec/specs/<capability>/spec.md`** describe comportamiento observable ya establecido. **Nunca** redefine el contrato: el contrato es `../docs/contract/v1/`.

Comandos: `/opsx:explore` para pensar, `/opsx:propose` para crear los artefactos, `/opsx:apply` para implementar, `/opsx:archive` para cerrar.

No todo merece una change: lo mecánico (añadir detekt, subir una versión) se implementa directo. La skill `reporrun-tarea-backlog` tiene el criterio y el procedimiento completo.

**Skills de este repo** (`.claude/skills/`), además de las de OpenSpec:

| Skill | Cuándo |
|---|---|
| `reporrun-tarea-backlog` | Empezar una tarea con ID; decidir si merece una change |
| `reporrun-pantalla-figma` | Construir o retocar una pantalla, un componente o el tema |
| `reporrun-contrato` | Tocar un `.schema.json`, los DTOs de `contract`, `schemaVersion` o los prompts |
| `reporrun-reglas-dominio` | Antes de cerrar algo que toque planes, revisiones, validadores o prompts |

---

## 1. Estructura

```text
rep-or-run-mobile-app/
├── CLAUDE.md  README.md  CHANGELOG.md
├── settings.gradle.kts  build.gradle.kts  gradle/libs.versions.toml
├── build-logic/                 # convention plugins: reporrun.kmp.library, reporrun.kmp.compose, reporrun.feature, reporrun.test
├── androidApp/                  # módulo de aplicación Android: MainActivity, manifest, R8
├── composeApp/                  # UI compartida (librería KMP): navegación raíz, grafo Koin
├── iosApp/                      # proyecto Xcode que consume el framework
├── core/
│   ├── model/                   # modelos de dominio puros
│   ├── domain/                  # casos de uso, ports, validadores, cálculo de fechas
│   ├── data/                    # repositorios + mappers + semilla del catálogo
│   ├── database/                # Room KMP (entidades, DAOs, migraciones)
│   ├── datastore/               # DataStore Preferences
│   ├── network/                 # cliente Ktor base
│   ├── ai/                      # adaptadores del proxy, DTOs del contrato (paquete `contract`), fakes
│   ├── pdf/                     # DocumentModel común + PdfRenderer expect/actual
│   ├── notifications/           # ReminderScheduler expect/actual
│   ├── designsystem/            # RepOrRunTheme, colores, tipografía Outfit, componentes
│   └── ui/                      # utilidades UI compartidas
├── feature/
│   └── onboarding/  profile/  gym/  plansetup/  plan/  review/
├── contract/v1/                 # copia versionada de ../docs/contract (schemas + ejemplos)
├── seed/equipment_catalog.json  # catálogo semilla (se empaqueta en :core:data)
└── scripts/sync-contract.sh     # copia ../docs/contract → contract/v<N>/
```

## 2. Comandos

En Windows usa `gradlew.bat`.

| Acción | Comando |
|---|---|
| Compilar (sin iOS) | `./gradlew build -x iosSimulatorArm64Test` |
| Todos los tests | `./gradlew allTests` |
| Tests de un módulo | `./gradlew :core:domain:allTests` |
| App Android debug | `./gradlew :androidApp:assembleDebug` |
| Lint / formato | `./gradlew detekt spotlessCheck` · corregir con `spotlessApply` |
| Reglas de dependencia | `./gradlew checkModuleDependencies` |
| Todo lo anterior | `./gradlew check` |
| Tests de contrato | `./gradlew :core:ai:allTests` (usa `contract/v1/`) |
| Sincronizar contrato | `scripts/sync-contract.sh` (requiere `../docs/contract`) |
| iOS (solo macOS) | `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` y abrir `iosApp/` en Xcode |

**Verificación mínima por tarea:** `spotlessCheck`, `detekt`, `checkModuleDependencies` y los tests de los módulos tocados, en verde. Si un comando aún no existe, dilo en el PR.

## 3. Arquitectura y dependencias

- **Capas:** UI (Compose) → ViewModel → casos de uso (`:core:domain`) → repositorios/ports → implementaciones (`:core:data`, `:core:ai`, `:core:database`...).
- **Dependencias permitidas:**
  - `feature:*` → `core:domain`, `core:model`, `core:designsystem`, `core:ui`. **Nunca** `core:data`, `core:database`, `core:network`, `core:ai` ni otro `feature:*`.
  - `core:data` → `core:domain`, `core:model`, `core:database`, `core:datastore`, `core:ai`.
  - `core:ai` → `core:network`, `core:domain`, `core:model`.
  - `core:domain` → solo `core:model` (+ coroutines/datetime). Puede usar `koin-core` **únicamente** en su archivo `di/` (`di/DomainModule.kt`, el `val domainModule`); ningún otro archivo del módulo importa Koin: los casos de uso son clases normales con inyección por constructor.
  - `core:model` → nada.
  - `core:designsystem` → nada: ningún módulo del proyecto. Lo que un componente tenga que decir (un error, la descripción de un botón) le llega como `String` desde el feature.
- **DI:** Koin; cada módulo expone `val xxxModule = module { }` y el grafo se arma en `composeApp`.
- **Navegación:** Navigation Compose multiplataforma con rutas `@Serializable`; cada feature expone `NavGraphBuilder.xxxGraph()`.
- **UDF:** `XxxUiState` (data class inmutable) + `XxxEvent` (sealed interface) + `XxxEffect` (one-shot). El ViewModel expone `StateFlow<UiState>` y `Flow<Effect>`; la UI solo llama `onEvent(...)`.
- **Plataforma:** prefiere interfaces en `commonMain` con implementación inyectada; `expect/actual` solo si no hay alternativa (`PdfRenderer`, `ReminderScheduler`).
- **Tiempo y concurrencia:** inyecta `Clock`, `TimeZone` y dispatchers; nunca `Clock.System.now()` ni `Dispatchers.IO` dentro de la lógica.

## 4. IA y red

- La app **solo** habla con la Lambda (`POST /v1/plans:generate`, `POST /v1/plans:review`); nunca con OpenAI.
- Los DTOs viven en `:core:ai` (paquete `contract`) y deben pasar los tests de ida y vuelta con `contract/v1/examples/`.
- La URL del proxy y la `X-App-Key` salen de BuildKonfig/`local.properties`, nunca del código. Por defecto en debug: `http://10.0.2.2:8787` (runner local) o los fakes.
- Las respuestas se validan con `PlanValidator` / `ProposalValidator` antes de guardarse; un plan inválido nunca llega a Room.

## 5. Persistencia

- Room KMP con `exportSchema = true`; los esquemas se versionan en `core/database/schemas/`.
- Tras la primera release interna, nada de migraciones destructivas: cada cambio lleva `Migration` + test.
- Las entidades no salen de `:core:database`/`:core:data`; dominio y UI usan `:core:model`.
- Un plan no se edita en el sitio: una versión nueva es un `Plan` nuevo (`ROR-AAAA-MM-DD-vN`) enlazado por `PlanChange`.

## 6. UI y diseño

- Figma es la fuente de verdad; cuando la pantalla exista allí, implementa desde Figma (Figma MCP).
- Nunca uses colores ni tamaños de texto hardcodeados: todo sale de `RepOrRunTheme` / `MaterialTheme`.

**Dos temas.** `RepOrRunTheme(darkTheme = …)` sigue el ajuste del sistema (`isSystemInDarkTheme()`) y `ThemeMode` (`System` / `Dark` / `Light`) se guarda en DataStore desde Ajustes. Referencia: páginas *Pantallas · v2 · Claro* y *Pantallas · v2 · Oscuro* del archivo de Figma, y el **Anexo A del plan**, que es la tabla normativa de tokens. Si esta tabla y el Anexo A se contradicen, manda el Anexo A.

| Token semántico | Claro | Oscuro | Uso |
|---|---|---|---|
| `background` | `#F1EEEB` | `#252525` | Fondo de pantalla |
| `surface` | `#FFFCF9` | `#3F3F3F` | Tarjetas, campos, filas |
| `inverseSurface` / `inverseOnSurface` | `#252525` / `#FFFCF9` | `#FFFCF9` / `#252525` | Bloque de énfasis: héroe, botón principal, barra inferior |
| `primary` / `onPrimary` | `#F2522E` / `#232323` | igual | Acento: chip de estado, marcador de hoy, progreso, botón flotante |
| `onBackground` / `onSurface` | `#252525` | `#FFFCF9` | Texto principal |
| `onSurfaceVariant` | `#656565` | `#B4B4B4` | Texto secundario |
| `outline` / `outlineVariant` | `#979797` | `#979797` / `#656565` | Bordes, separadores, iconos inactivos |
| `extendedColors.accentText` | `#DA330E` | `#F48F78` | El acento cuando es texto: overlines, cifras, avisos |

- **La acción principal no es naranja.** Es `inverseSurface`: negra en claro, blanca en oscuro. El naranja marca estado, no manda.
- **El naranja nunca es texto ni lleva texto blanco.** Como texto va `extendedColors.accentText`; encima del naranja va `#232323`.
- **`#979797` no es color de texto** en ninguno de los dos temas: bordes, separadores e iconos inactivos, nada más.
- **Superficies constantes:** la bienvenida, la portada del PDF y la vista previa del PDF dentro de la app se ven igual en los dos temas. En Compose se resuelven con un `RepOrRunTheme(darkTheme = true)` anidado o con colores fijos documentados, no dejándolos al tema.
- **Forma:** radio 24 en tarjetas y campos, 28 en el bloque de énfasis y los botones, 16-18 en chips, círculo en botones de icono. **Sin elevación ni sombras.** Botones de 60 dp, botones de icono de 44 dp, chips de 34 dp.
- **Patrones que se implementan una vez** y se reutilizan en todas las pantallas: tarjeta, fila de lista con chevron, chip de filtro, campo con etiqueta encima, stepper de tres nodos, interruptor, botón de icono circular y barra inferior en pastilla con el ítem activo etiquetado.
- **Contraste:** las reglas del Anexo A salen de medir, no de criterio. Antes de dar por buena una pantalla, compruébalas; las 14 de Figma están auditadas en verde en los dos temas.
- **Tipografía Outfit (OFL 1.1):** TTF estáticos 400/500/600/700 en `core/designsystem/src/commonMain/composeResources/font/`. Escala: display/headline 600-700, title 600, body 400, label 500; las cifras en 500-600.
- Los strings van en `composeResources/values/strings.xml`, en español.
- **Accesibilidad:** `contentDescription` en iconos accionables, áreas táctiles de al menos 48 dp y respeto del tamaño de fuente del sistema.

## 7. Tests

- **Obligatorio:** casos de uso, `PlanValidator`, `ProposalValidator`, `ReviewScheduleCalculator` (con `Clock` falso), mappers, DAOs (base en memoria), contrato (ida y vuelta con los ejemplos) y ViewModels principales (Turbine).
- Fakes escritos a mano antes que librerías de mocks; red con Ktor `MockEngine`.
- Un comportamiento por test; nombres descriptivos (`rejectsExerciseWithUnknownEquipment`).
- Cobertura objetivo: 80 % o más en `:core:domain` y en los validadores. Sin tests de UI automatizados en el MVP; adjunta capturas en el PR.

## 8. Convenciones

- Kotlin idiomático, inmutable por defecto, `sealed interface` para estados y errores, nada de `!!`.
- Errores esperables como resultados tipados, no excepciones que crucen capas.
- KDoc en las APIs públicas de `:core:domain` y `:core:ai`.
- **Nombres:** `XxxUseCase`, `XxxRepository`/`XxxRepositoryImpl`, `XxxViewModel`, `XxxScreen`, `XxxUiState`.
- **Dependencias:** solo vía `gradle/libs.versions.toml` y justificadas en el PR. Nada de RxJava ni librerías de UI de terceros sin ADR.
