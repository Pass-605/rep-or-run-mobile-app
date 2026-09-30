# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado: [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Añadido

- Esqueleto del proyecto KMP: `androidApp`, `composeApp`, `core:model` y `core:designsystem` (S-02).

- OpenSpec (`schema: spec-driven`) como flujo de trabajo del repo: `openspec/config.yaml` con el contexto del proyecto y las reglas por artefacto, y cuatro skills propias en `.claude/skills/` (`reporrun-tarea-backlog`, `reporrun-pantalla-figma`, `reporrun-contrato`, `reporrun-reglas-dominio`).

- Calidad del build (S-04): detekt (`config/detekt/detekt.yml`), Spotless con ktlint para `*.kt` y `*.kts`, y la tarea `checkModuleDependencies`, que aplica las dependencias permitidas de `CLAUDE.md` §3 como lista blanca: los `feature:*` solo pueden usar `core:domain`, `core:model`, `core:designsystem` y `core:ui`; `core:domain` solo `core:model`; `core:model` nada; y `core:ai` y `core:data`, lo suyo. Un módulo sin regla en §3 no se comprueba. Vienen en los convention plugins `reporrun.quality` y `reporrun.root`, y `./gradlew check` los ejecuta todos.

- Modelos de dominio en `core:model` (B-02): las quince entidades del plan §10, IDs como value classes y los enums que faltaban, alineados con `contract/v1`. Las reglas del plan §4 que se pueden comprobar pieza a pieza son invariantes del tipo: la carga de ensayo es texto (`TrialLoad`) y no existe ningún campo de kilos prescritos; las alternativas no tienen series; las series de un unilateral son por lado y no se duplican; el RIR solo existe con repeticiones; con datos insuficientes un `ReviewResult` no admite propuestas; y una molestia se registra con fecha. Única dependencia: `kotlinx-datetime` 0.8.0, para `LocalDate` y `LocalTime`.
- `reporrun.test` activa los tests en host de Android, que ejecutan `commonTest` en la JVM: en Windows y Linux era el único target donde podían correr.

### Corregido

- La build fallaba al aplicar `com.android.application` sobre el módulo KMP: AGP 9 lo prohíbe. `composeApp` pasa a librería KMP (`com.android.kotlin.multiplatform.library`) y el APK lo produce el nuevo módulo `androidApp`.
