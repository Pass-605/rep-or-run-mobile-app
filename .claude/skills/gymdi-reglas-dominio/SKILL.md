---
name: gymdi-reglas-dominio
description: Verifica código, specs o prompts contra las reglas del dominio de entrenamiento de Gymdi. Úsala antes de cerrar cualquier trabajo que toque generación de planes, revisión, validadores, el catálogo de equipos o los prompts de la Lambda.
---

# Reglas de dominio

Estas reglas vienen del paquete de entrenamiento del que se derivó la app. **No son
preferencias de producto: son la diferencia entre un plan utilizable y uno que manda a
alguien a hacer un ejercicio que su gimnasio no tiene, o a subir carga sin datos.**

## Lo primero: leerlas de la fuente

La lista completa está en **`../docs/plan/PLAN_TRABAJO_MVP.md`, sección 4** (catorce
reglas). El resumen de siete está en `../CLAUDE.md` §6. Esta skill **no** las reproduce a
propósito: una tercera copia se desincronizaría y sería la que alguien lea el día que
importa.

Antes de dar por terminado el trabajo, abre la sección 4 y recórrela entera.

## Qué hacer con ellas

Para cada regla, una de tres respuestas, por escrito en el PR:

1. **Aplica y está cubierta** — di dónde: el test, el validador o la línea del prompt.
2. **No aplica** — di por qué en media línea.
3. **Aplica y no está cubierta** — entonces el trabajo no está terminado.

No vale "se respetan las reglas de dominio". Eso es exactamente lo que se escribe cuando
no se han mirado.

## Dónde vive cada cosa

| Qué | Dónde se aplica |
|---|---|
| Forma de la respuesta del LLM | JSON Schema, en la Lambda |
| Reglas de dominio sobre un plan recibido | `PlanValidator`, en `:core:domain` |
| Reglas de dominio sobre una revisión | `ProposalValidator`, en `:core:domain` |
| Que el modelo no se las salte de entrada | Prompts de la Lambda (`prompts/*.md`) |

Las mismas reglas se aplican **dos veces** a propósito: en el prompt para que el modelo
haga lo correcto, y en el validador porque todo lo que viene del LLM es no confiable. Si
una regla solo está en el prompt, no está.

## Las que más se incumplen

- **Inventario.** Ningún ejercicio puede requerir una máquina que no esté en el gimnasio
  activo. El cruce se hace por `equipmentId` contra la `key` del catálogo
  (`seed/equipment_catalog.json`), **nunca por el nombre del ejercicio**.
- **Series.** Las alternativas no suman series. Los unilaterales cuentan series por lado.
  Es el error clásico al calcular volumen semanal.
- **Cargas.** Son de ensayo, en texto. Nada calcula kilos a partir de edad, talla o peso,
  ni en el código ni en el prompt ni en el PDF.
- **Convención por `LoadType`.** Un selector, unos discos totales, un Smith, un carro, una
  mancuerna por unidad y un peso corporal no se expresan igual. La tabla está en la
  sección 4 del plan.
- **Aprobación.** Una revisión produce **propuestas**. Solo la aprobación crea una versión
  nueva, y la anterior se conserva. Nada muta un plan en el sitio.
- **Datos insuficientes.** La revisión lo dice y no propone subir cargas. El silencio o un
  "mantén" genérico no es lo mismo que decirlo.
- **Sin nutrición ni diagnóstico.** En la UI, los prompts y el PDF. Incluye los textos de
  relleno y los ejemplos.

## Privacidad

El paquete de origen contiene datos personales reales. Los fixtures y los ejemplos son
**sintéticos**, siempre. Nunca copies nombres, medidas, edad ni notas de salud a un repo
ni a `docs/`.
