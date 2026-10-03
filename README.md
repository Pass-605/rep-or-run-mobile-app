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

## Estructura

```text
androidApp/          módulo de aplicación Android: arranque, manifest y R8
composeApp/          UI compartida (librería KMP) y framework de iOS; grafo de Koin y navegación raíz
core/model/          modelos de dominio, sin dependencias
core/domain/         interfaces de repositorio y casos de uso de captura (más adelante, validadores)
core/data/           repositorios, mappers entidad↔modelo y carga de la semilla del catálogo
core/database/       Room KMP: entidades, DAOs y RepOrRunDatabase; esquemas en core/database/schemas/
core/datastore/      DataStore Preferences: consentimiento e identificador de instalación
core/designsystem/   RepOrRunTheme: paleta, tipografía Outfit, formas, iconos y componentes
iosApp/              proyecto Xcode que consume el framework
```

Los demás módulos del plan (`core:network`, `core:ai`, `core:pdf`, `core:notifications`, `feature:*`) se añaden cuando la tarea que los necesita entra en juego, no antes.

## Una pantalla nueva

El modelo es `composeApp/src/commonMain/kotlin/app/reporrun/scaffold/` (S-09), con el porqué de cada decisión en los comentarios:

- `ScaffoldContract.kt`: `XxxUiState`, `XxxEvent` y `XxxEffect`.
- `ScaffoldViewModel.kt`: solo casos de uso por constructor, un `StateFlow` de estado, un `Flow` de efectos y `onEvent` como única entrada. Su test, `ScaffoldViewModelTest`, es el modelo de test.
- `ScaffoldScreen.kt`: `XxxRoute` (con ViewModel) y `XxxScreen` (función de estado y `onEvent`).
- `ScaffoldNavigation.kt`: rutas `@Serializable` y `NavGraphBuilder.xxxGraph(...)`, que recibe lambdas y no el `NavController`.
- `ScaffoldModule.kt`: el `val xxxModule` de Koin, que se añade a `appModules()` en `di/AppModule.kt`.

Los dos destinos de prueba se borran cuando entre la primera pantalla real.

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
2. `local.properties` lo genera Android Studio con la ruta del SDK. **No se versiona.**
3. Ejecuta `:androidApp:installDebug` en un emulador con API 26 o superior.
