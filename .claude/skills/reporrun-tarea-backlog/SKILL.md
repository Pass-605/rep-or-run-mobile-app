---
name: reporrun-tarea-backlog
description: Conecta una tarea del backlog de Rep or Run con una change de OpenSpec y la cierra en los dos sitios. Úsala al empezar a trabajar en una tarea con ID (S-03, C-04, A-16…) o cuando haya que decidir si algo merece una change.
---

# Del backlog a una change

Hay dos sistemas y cada uno tiene un trabajo distinto. Si se confunden, acabas con un
backlog que dice una cosa y unas changes que dicen otra.

- **`../docs/backlog/` es la hoja de ruta.** Los IDs, el orden, las dependencias, el
  estado y las estimaciones. Es lo que se mira para decidir qué toca ahora.
- **`openspec/changes/` es la unidad de ejecución.** El proposal, el diseño, las specs y
  las tareas de *un* cambio concreto.

Una change **no** sustituye a la tarea del backlog: la implementa.

## ¿Esta tarea necesita una change?

Sí cuando cambia comportamiento observable, toca el contrato, o hay más de una forma
razonable de hacerla. Ahí la spec paga.

No cuando es mecánica y su criterio de aceptación ya es el enunciado: añadir detekt y
spotless, subir una versión, generar el wrapper. Envolver eso en cuatro artefactos es
ceremonia. Implementa directo y anota la verificación en el PR.

Si dudas, mira el criterio de aceptación de la tarea en el backlog: si ya describe el
resultado sin ambigüedad, no hace falta spec.

## Procedimiento

1. **Una sola tarea.** Comprueba en `../docs/backlog/` que sus dependencias están en
   `done` y que no está `blocked:accounts` ni `needs:macos` sin un Mac delante. Mira el
   campo **Repo**: si no es `rep-or-run-mobile-app`, no se trabaja aquí.
2. **Crea la change con el ID en el nombre**, en kebab-case:
   `openspec new change "c-04-design-system-outfit"`. El ID por delante hace que change y
   backlog se puedan cruzar de un vistazo, en los dos sentidos.
3. **Genera los artefactos** con `/opsx:propose` (o la skill `openspec-propose`). El
   contexto y las reglas los inyecta `openspec/config.yaml`; no los repitas a mano.
4. **Anota en la tarea del backlog** la ruta de la change, y pásala a `in progress`.
5. **Rama** `feat/<ID>-slug` en este repo, commits pequeños en español.
6. **Implementa** con `/opsx:apply`. Si la implementación revela que el diseño estaba mal,
   actualiza el artefacto: para eso existe. No sigas contra un diseño que ya sabes falso.
7. **Verifica** con los comandos del `CLAUDE.md` §2 que apliquen.
8. **Cierra en los dos sitios:** archiva la change (`/opsx:archive`) y marca la tarea
   `done` en `../docs/backlog/`. Una change archivada con la tarea aún en `in progress` es
   el fallo más fácil de cometer aquí.

## Reglas que no se negocian

- **Nunca una change que abarque los dos repos.** Un cambio de contrato se divide en una
  change por repo. `reporrun-lambda` todavía no tiene OpenSpec.
- **Una change no edita `../docs/`.** Ni el plan, ni el backlog más allá del estado de su
  propia tarea, ni el contrato. Son fuentes de verdad del workspace, no de este repo. Si
  hace falta cambiarlas, el proposal lo dice y se hace aparte.
- **Las specs no redefinen el contrato.** Describen comportamiento; el contrato es
  `../docs/contract/v1/`.
- Si al escribir el proposal no encuentras la tarea del backlog que corresponde, para: o
  falta la tarea, o la change no debería existir todavía.
