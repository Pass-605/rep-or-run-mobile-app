# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado: [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Añadido

- Esqueleto del proyecto KMP: `androidApp`, `composeApp`, `core:model` y `core:designsystem` (S-02).

- OpenSpec (`schema: spec-driven`) como flujo de trabajo del repo: `openspec/config.yaml` con el contexto del proyecto y las reglas por artefacto, y cuatro skills propias en `.claude/skills/` (`reporrun-tarea-backlog`, `reporrun-pantalla-figma`, `reporrun-contrato`, `reporrun-reglas-dominio`).

- Calidad del build (S-04): detekt (`config/detekt/detekt.yml`), Spotless con ktlint para `*.kt` y `*.kts`, y la tarea `checkModuleDependencies`, que falla si un `feature:*` depende de `core:data`, `core:database`, `core:network`, `core:ai` u otro `feature:*`. Vienen en los convention plugins `reporrun.quality` y `reporrun.root`, y `./gradlew check` los ejecuta todos.

### Corregido

- La build fallaba al aplicar `com.android.application` sobre el módulo KMP: AGP 9 lo prohíbe. `composeApp` pasa a librería KMP (`com.android.kotlin.multiplatform.library`) y el APK lo produce el nuevo módulo `androidApp`.
