# Convenciones de este repo

Repo remoto: `github.com/Pass-605/rep-or-run-mobile-app`.

Las reglas de arquitectura y de dominio están en `CLAUDE.md` (este repo) y en el del
workspace. Este archivo es solo el trato con git y con GitHub.

## Ramas

- **`main` es la única rama permanente** y siempre está verde. No hay `develop`: un solo
  entorno, un solo desarrollador y la entrega a cuatro días no justifican dos ramas
  permanentes que haya que sincronizar.
- Una rama por tarea del backlog, con su **ID por delante**:
  `feat/S-04-calidad`, `fix/C-07-chip-contraste`, `chore/S-08-rename`, `docs/A-16-rangos`.
- El ID no es decorativo: es lo que permite cruzar rama, PR, commit y tarea en los dos
  sentidos. Una rama sin ID es una rama que nadie sabe de dónde salió.
- La rama muere con el PR. Borrar al mergear.

Prefijos: `feat` (comportamiento nuevo), `fix` (corrige algo roto), `chore`
(infraestructura, dependencias, build), `docs` (solo documentación), `refactor` (sin
cambio de comportamiento).

## Commits

**Conventional Commits en español.** El código va en inglés; los mensajes, no.

```
feat(domain): valida equipos contra el inventario del gimnasio

PlanValidator rechazaba solo los equipos sin key. Ahora comprueba
que la key exista en el gimnasio activo, no solo en el catálogo.

Tarea: E-05
```

- Asunto en **imperativo, minúscula, sin punto final**, 72 caracteres o menos.
- **Scope = el módulo o el área**: `build`, `domain`, `data`, `ai`, `ui`, `designsystem`,
  `pdf`, `notifications`, `ci`, `deps`, `docs`.
- El cuerpo explica **por qué**, no qué: el qué ya está en el diff.
- Footer `Tarea: <ID>` cuando la hay.
- Commits pequeños. Un commit que toca build, dominio y UI a la vez no se puede revertir.

## Pull requests

Uno por tarea. **El título del PR es el mensaje del commit de squash**, así que se escribe
con el mismo formato que un commit.

Se mergean con **squash**, siempre. El historial de `main` queda en un commit por tarea,
que es exactamente el grano del backlog. Sin merge commits y sin rebase merge: un solo
modo, sin decidir cada vez.

Puedes mergear tus propios PR — trabajas solo. Pero **lee el diff completo antes**, sobre
todo si lo escribió un agente. Ya pasó una vez que subieron AGP, Gradle y `compileSdk` sin
avisar; el diff es donde eso se ve en dos segundos.

## Protección de `main`

En *Settings → Branches → Add rule* para `main`:

- [x] Require a pull request before merging
- [x] **Required approvals: 0** — con 1 te bloqueas a ti mismo, porque no puedes aprobar tu
      propio PR
- [x] Require status checks to pass — añade el check de CI cuando exista (S-05)
- [x] Require linear history
- [x] Block force pushes
- [x] Restrict deletions
- [ ] Allow squash merging **solo** (desactiva merge commits y rebase merging en
      *Settings → General → Pull Requests*)

## Versiones y tags

**SemVer**, tags con `v` delante: `v0.1.0`. Solo desde `main`, solo cuando hay una build
que alguien va a instalar. El `CHANGELOG.md` sigue [Keep a Changelog]: lo que entra va a
`[Sin publicar]` y se corta al poner el tag.

`versionCode` sube de uno en uno en cada build interna; `versionName` sigue al tag.

[Keep a Changelog]: https://keepachangelog.com/es-ES/1.1.0/

## Qué se versiona y qué no

Sí, aunque sorprenda:

- **`.claude/skills/`** — las skills del proyecto son parte del repo. Si viven solo en tu
  máquina, el siguiente agente no las tiene.
- **`openspec/`** — las changes y las specs son el historial de decisiones de cada tarea.
- **`contract/v<N>/`** — la copia del contrato, para que la CI no dependa de `../docs/`.

No:

- **`../docs/`** — documentación local por decisión registrada (ADR 0007). El plan, el
  backlog y los ADR no entran en el repo.
- `local.properties`, `.env`, cualquier key, `.gradle/`, `build/`, `.idea/`.

Antes de cada push, `git status` limpio de cosas que no toquen. Un secreto subido a un repo
público no se borra: se rota.
