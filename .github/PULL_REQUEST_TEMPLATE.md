## Qué hace

<!-- Dos o tres frases. El por qué, no el listado de archivos. -->

**Tarea:** <!-- ID del backlog, p. ej. S-04. Si no hay tarea, explica por qué. -->

## Verificación

<!-- Los comandos que ejecutaste y su resultado. No "todo en verde": el comando y su salida. -->

- [ ] `gradlew.bat spotlessCheck detekt`
- [ ] `gradlew.bat checkModuleDependencies`
- [ ] Tests de los módulos tocados
- [ ] `:androidApp:installDebug` arranca

## Capturas

<!-- Si hay UI: claro y oscuro. Si no hay UI, borra esta sección. -->

## Comprobaciones

- [ ] Sin secretos, sin datos personales reales, sin payloads en logs.
- [ ] Reglas de dominio respetadas (ver skill `gymdi-reglas-dominio`), o no aplican.
- [ ] Sin versiones de AGP, Kotlin, Compose Multiplatform ni SDK cambiadas. Si cambian,
      explica por qué aquí.
- [ ] `docs/` actualizado: estado de la tarea en el backlog y, si aplica, ADR o contrato.

## Riesgos y pendientes

<!-- Lo que sabes que queda flojo. Es más útil que decir que no hay riesgos. -->
