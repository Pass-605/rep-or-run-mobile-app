# rep-or-run-mobile-app

App de Rep or Run en Kotlin Multiplatform con Compose Multiplatform (Android e iOS). Genera planes de gimnasio a partir del perfil, el inventario de máquinas y las preferencias de la persona. Local-first y sin cuentas.

La documentación del proyecto —plan, backlog, ADRs, contrato y diseño— vive **fuera de este repo**, en `../docs/`. Las reglas para trabajar aquí están en [`CLAUDE.md`](CLAUDE.md).

## Requisitos

| Qué | Versión |
|---|---|
| JDK | 21 |
| Android Studio | con el SDK 37 instalado |
| Xcode | solo en macOS, para iOS |
| Gradle | lo trae el wrapper (9.3.1) |
| Android mínimo | **API 26 (Android 8.0)** |

Versiones de Kotlin, AGP y Compose Multiplatform: `gradle/libs.versions.toml`, verificadas el 26/09/2026. El par **AGP 9.1.1 + Gradle 9.3.1** es el que compila hoy: los AAR de `androidx.compose` 1.12.1, que arrastra Compose Multiplatform 1.12.1, exigen AGP 9.1.0 o superior y `compileSdk` 37, y AGP 9.1 a su vez exige Gradle 9.3.1 o superior. Android Studio puede proponer versiones más nuevas, y subirlas es seguro mientras se respete la matriz de compatibilidad de KMP.

## Decisiones de compilación

- **`:androidApp` separado de `:composeApp`.** Desde AGP 9 el plugin `com.android.application` no puede convivir con el de Kotlin Multiplatform en el mismo módulo (la build falla al aplicarlo). Y AGP 9 compila Kotlin por su cuenta, así que `:androidApp` **no** aplica `org.jetbrains.kotlin.android`: si se aplica, la build aborta con «no longer required for Kotlin support since AGP 9.0». El bloque `kotlin { }` del módulo lo sigue creando AGP. Así que `:composeApp` es una librería KMP con toda la UI, y `:androidApp` es un módulo Android fino: `MainActivity`, manifest, tema de ventana y R8. En iOS el reparto ya era ese desde el principio: `iosApp/` consume el framework que produce `:composeApp`.
- **compileSdk 37, targetSdk 36, minSdk 26.** Se compila contra 37 porque los AAR de Compose 1.12.1 lo exigen; `targetSdk` se queda en 36 porque subirlo es optar por comportamientos nuevos en tiempo de ejecución y eso se decide aparte, con su propia revisión.
- **minSdk 26 (Android 8.0).** Cubre prácticamente todo el parque activo y trae dos cosas que este proyecto usa: `java.time` en la plataforma (sin *desugaring* para fechas) y los canales de notificación, que hacen falta para los recordatorios de la línea E.
- **JDK 21 para compilar, bytecode 17 para Android.** El *toolchain* fija JDK 21 para que la build sea igual en cualquier máquina; el `jvmTarget` de Android se queda en 17, que es hasta donde D8 llega sin sorpresas. Si algún día se sube, se sube en los tres módulos a la vez.
- **`FAIL_ON_PROJECT_REPOS`**: los repositorios se declaran solo en `settings.gradle.kts`. Si un módulo intenta añadir el suyo, la build falla en vez de resolver dependencias por un camino distinto según el módulo.
- **R8 activado en release** con `proguard-rules.pro` vacío a propósito: las reglas se añaden con la librería que las pide, no antes.
- **Tema de ventana en claro y oscuro** (`values/` y `values-night/`). Es solo el color del arranque, pero sin él abrir la app en oscuro da un destello blanco.
- **Sin accesores tipados de proyecto**: `project(":core:model")` funciona en cualquier versión de Gradle.

## Contrato con la Lambda

`contract/v1/` es una **copia** de `../docs/contract/`, que es la fuente de verdad y vive fuera de git. No se edita aquí: se cambia en `docs/` y se vuelve a copiar.

```bash
scripts/sync-contract.sh           # copia ../docs/contract → contract/v1/
scripts/sync-contract.sh --check   # compara sin escribir; sale con 1 si difieren
```

En Windows, desde Git Bash. Los tests de contrato (`gradlew.bat :core:ai:allTests`) leen la copia del repo, así que no necesitan `../docs/`.

## Red y configuración local

`:core:network` (A-12) es el cliente HTTP. La URL del proxy y la clave de app **no están en el repo**: salen de `local.properties` o, si allí faltan, de una variable de entorno con el mismo nombre. Copia `local.properties.example`:

| Clave | Valor de desarrollo |
|---|---|
| `ROR_APP_KEY` | la misma `ROR_APP_KEY` del `.env` del runner (`rep-or-run-lambda`) |
| `ROR_BASE_URL_ANDROID` | `http://10.0.2.2:8787` (el host visto desde el emulador) |
| `ROR_BASE_URL_IOS` | `http://localhost:8787` (`10.0.2.2` no existe en el simulador) |

Si falta alguna, `:core:network` **no compila** y el error dice cuál: mejor eso que un `401` en runtime que parece otra cosa. Como `check` compila el módulo, un clon nuevo necesita las tres antes de pasar la verificación.

**La `X-App-Key` no es un secreto.** Viaja dentro del APK: cualquiera que lo descomprima la tiene. Sirve para que el tráfico automático muera en un `401` antes de gastar un token; no autentica a nadie. Aun así **no se commitea**: lo que entra en git queda en el historial, y rotarla no lo limpia.

**Android bloquea HTTP en claro**, y el runner habla HTTP. La build de **debug** lleva `androidApp/src/debug/res/xml/network_security_config.xml`, que lo permite **solo hacia `10.0.2.2`**. Release no lo lleva, y no hay `cleartextTrafficPermitted` global.

**El engine de iOS (Darwin) está escrito pero sin compilar:** necesita un Mac (S-07, `needs:macos`).

## Estructura

```text
androidApp/          módulo de aplicación Android: arranque, manifest y R8
composeApp/          UI compartida (librería KMP) y framework de iOS; grafo de Koin y navegación raíz
core/model/          modelos de dominio, sin dependencias
core/domain/         interfaces de repositorio, ports de IA, casos de uso de captura, GeneratePlanUseCase y PlanValidator (paquete validation)
core/data/           repositorios, mappers entidad↔modelo y carga de la semilla del catálogo
core/database/       Room KMP: entidades, DAOs y RepOrRunDatabase; esquemas en core/database/schemas/
core/datastore/      DataStore Preferences: consentimiento e identificador de instalación
core/ai/             DTOs del contrato con la Lambda (paquete contract), su mapeo al dominio y los fakes de generación y revisión
core/designsystem/   RepOrRunTheme: paleta, tipografía Outfit, formas, iconos y componentes
core/ui/             utilidades de UI que comparten los features (CollectEffects)
feature/onboarding/  bienvenida, aviso y consentimiento
feature/profile/     perfil en dos pasos
feature/gym/         gimnasios (lista, crear, renombrar, activar) y su inventario de máquinas
feature/plansetup/   preferencias del bloque y resumen previo a generar
feature/plan/        pantalla Generando y destino del plan recién generado
iosApp/              proyecto Xcode que consume el framework
contract/v1/         copia versionada del contrato: schemas y ejemplos (no se edita a mano)
scripts/             sync-contract.sh
```

Los demás módulos del plan (`core:network`, `core:pdf`, `core:notifications` y el resto de `feature:*`) se añaden cuando la tarea que los necesita entra en juego, no antes.

## Una pantalla nueva

El modelo es `feature/onboarding/` (C-05), que conserva el patrón del armazón de S-09 con el porqué de cada decisión en los comentarios:

- `build.gradle.kts`: el plugin `reporrun.feature`, que trae las únicas dependencias de proyecto permitidas, y los recursos del módulo. El módulo se añade a `settings.gradle.kts` y a las dependencias de `composeApp`.
- `OnboardingContract.kt`: `XxxUiState`, `XxxEvent` y `XxxEffect`.
- `OnboardingViewModel.kt`: solo casos de uso por constructor, un `StateFlow` de estado, un `Flow` de efectos y `onEvent` como única entrada. Su test, `OnboardingViewModelTest`, es el modelo de test.
- `WelcomeScreen.kt`: `XxxRoute` (con ViewModel) y `XxxScreen` (función de estado y `onEvent`). Los efectos se recogen con `CollectEffects`, de `:core:ui`.
- `OnboardingNavigation.kt`: rutas `@Serializable` y `NavGraphBuilder.xxxGraph(...)`, que recibe lambdas y no el `NavController`.
- `OnboardingModule.kt`: el `val xxxModule` de Koin, que se añade a `appModules()` en `di/AppModule.kt`.
- `composeResources/values/strings.xml`: los textos, que no salen del módulo.

La bienvenida es además el ejemplo de **superficie constante**: se ve igual en los dos temas porque envuelve su contenido en `RepOrRunTheme(darkTheme = true)`. Las demás pantallas no lo hacen.

`feature/profile/` (C-06) es el ejemplo de **formulario**: dos destinos de un mismo grafo que comparten ViewModel (`profileGraph(graphOwner = …)`), los números como texto en el estado y los errores tal como los devuelve el caso de uso (`Map<ProfileField, FieldIssue>`). La pantalla no compara números: pregunta a `ValidateProfileUseCase`.

`feature/gym/` (C-07) es el ejemplo de **lista que se observa**: el ViewModel recoge `ObserveGymsUseCase` y pinta lo guardado, sin tocar la lista a mano al crear, renombrar o activar. El formulario del nombre es otro destino del mismo grafo y comparte ViewModel, como en el perfil. Borrar un gimnasio no está: no tiene caso de uso y arrastra en cascada inventario y planes.

El inventario (C-08), en el mismo módulo, es el ejemplo de **lista larga con búsqueda**: `LazyColumn` con claves estables y `contentType`, las filas ya filtradas y agrupadas en el estado (`InventoryRow`), y la regla de búsqueda en `:core:domain` (`CatalogSearch`). Su ViewModel comparte dueño con el de gimnasios y recibe el gimnasio con un evento `Opened(gymId)` que manda el destino, no por constructor. Marca por la clave del catálogo, nunca por el nombre.

`feature/plansetup/` (C-09) es el ejemplo de **formulario de una sola pantalla**: las preferencias guardadas se leen una vez, como el perfil, y las opciones de cardio se observan, porque salen del inventario del gimnasio activo (`ObserveCardioOptionsUseCase`) y no de una lista escrita. Recibe `Clock` y `TimeZone` por constructor para ofrecer las fechas de inicio.

El resumen (C-10), en el mismo módulo, es el ejemplo de **pantalla que solo observa**: su estado es el `GenerationSummary` de `BuildGenerationSummaryUseCase`, sin traducir, y la pantalla no decide qué falta para generar. También es el ejemplo de **salidas que dependen de por dónde se entró**: perfil, inventario y preferencias avisan de que han guardado y `RepOrRunNavHost` pregunta a la pila con `popBackToSummary()`; si el resumen está debajo se vuelve a él, y si no, se sigue el alta. Ningún formulario sabe desde dónde lo abrieron.

`feature/plan/` (C-11) es el ejemplo de **operación larga que se puede cancelar**: `GeneratingViewModel` pide el plan al crearse, dentro de `viewModelScope`, así que un giro no lo pide dos veces y cancelar o salir cancela la corrutina. **Restaurarse no es entrar:** la primera generación deja una marca en `SavedStateHandle`, y si al crearse ya está —el proceso murió con la espera delante— no pide nada y deja que la persona decida. Qué se puede reintentar no lo decide la pantalla: lo lee del tipo de `GeneratePlanResult`, y `GeneratingLabels` le pone la palabra —«Reintentar» si no hubo servicio, «Generar otra vez» si el plan volvió inválido, que es otra llamada—. Lo suyo es el tope de dos planes inválidos seguidos, en memoria, y la cuenta atrás tras un límite de peticiones, contra el `Clock` inyectado. No usa `RepOrRunScreenScaffold`: es una espera a pantalla completa.

El flujo hoy es bienvenida → perfil → gimnasios → máquinas → preferencias → resumen → generando → vista del plan, y `AppViewModel` abre la app por el primer paso que falte (con gimnasio creado, la lista de gimnasios; **con algún plan guardado en el gimnasio activo, la vista del plan**) y carga el catálogo semilla en cada arranque. La vista del plan (C-12, `PlanRoute` con id opcional) enseña el plan recién generado o, sin id, el activo; deja aprobar cada día y, con todos aprobados, activarlo. Sin plan activo dice que no lo hay y lleva al resumen, que desde ahí es la puerta al perfil, el gimnasio y las preferencias. Una pantalla que no cabe en `RepOrRunScreenScaffold` (sin barra, con cabecera fija o con un pie que cambia) usa `RepOrRunScreenFrame` y `RepOrRunScrollContent`. Para ver un error en el dispositivo, `FakeAiConfig(failure = …)` en `aiModule`.

## Comandos

En Windows usa `gradlew.bat`.

| Acción | Comando |
|---|---|
| Compilar Android | `./gradlew :androidApp:assembleDebug` |
| Instalar en el emulador | `./gradlew :androidApp:installDebug` |
| Tests comunes | `./gradlew allTests` |
| Framework iOS (solo macOS) | `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` |

## iOS

La carpeta `iosApp/` tiene el código Swift, pero **el proyecto de Xcode todavía no está creado**: hace falta un Mac. Ver la tarea S-02 del backlog, etiquetada `needs:macos`.

## Primera vez

1. Abre la carpeta en Android Studio y deja que sincronice; descargará Gradle 9.3.1, los plugins y la plataforma android-37.
2. `local.properties` lo genera Android Studio con la ruta del SDK. **No se versiona.** Añade las tres claves de `local.properties.example` (ver [Red y configuración local](#red-y-configuración-local)).
3. Ejecuta `:androidApp:installDebug` en un emulador con API 26 o superior.
