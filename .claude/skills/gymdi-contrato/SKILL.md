---
name: gymdi-contrato
description: Cambia el contrato entre la app y la Lambda (JSON Schemas de docs/contract) sin romper ninguno de los dos repos ni el modo estricto de OpenAI. Úsala en cuanto una tarea toque un .schema.json, los DTOs del paquete contract, schemaVersion o los prompts.
---

# Cambiar el contrato

**El orden canónico está en `../CLAUDE.md`, sección 4.** Esta skill es el cómo y las
trampas. La fuente de verdad es `../docs/contract/`; las copias `contract/v1/` de cada
repo son eso, copias.

## Antes de tocar nada

Pregunta. Cambiar el contrato está en la lista de "pregunta antes de" del workspace
(`../CLAUDE.md` §9). No es una formalidad: un cambio de contrato obliga a un PR por repo
y a desplegar la Lambda antes de publicar la app.

## Las dos capas, y por qué importan

- **El schema cubre la forma.** Las reglas de dominio las aplican `PlanValidator` y
  `ProposalValidator` en la app. No intentes expresar una regla de dominio en el schema:
  no cabe, y el modo estricto lo prohíbe.
- **Los schemas que se envían a OpenAI son estrictos**: `v1/plan.schema.json` y
  `v1/review-result.schema.json`. Sobre ellos, Structured Outputs exige:
  - autocontenidos, sin `$ref` externos;
  - **todas** las propiedades en `required`;
  - `additionalProperties: false` en todos los objetos;
  - nullable con unión de tipos (`["string", "null"]`), no con `nullable`;
  - **nada** de `minimum`, `maximum`, `pattern`, `minItems` ni `maxItems`.
- Los sobres de transporte (`generate-request`, `generate-response`, `review-request`,
  `review-response`, `error`) **no** van al modelo y sí pueden usar `$ref` y validaciones.
  El modelo no produce `usage` ni `model`: esos campos los pone la Lambda.

La trampa: añadir un `minItems` a `plan.schema.json` no falla en ningún test local. Falla
en la primera llamada real a OpenAI. Por eso `validate.py` lo comprueba.

## Procedimiento

1. Edita `../docs/contract/v1/`. Si rompe compatibilidad, sube `schemaVersion`.
   `schemaVersion` y `promptVersion` son independientes: el primero describe la forma, el
   segundo el texto del prompt. Cambiar un prompt no sube `schemaVersion`.
2. Añade o ajusta ejemplos en `../docs/contract/examples/`. Si el cambio introduce una
   forma nueva de fallar, añade el caso a `examples/invalid/` **y** su entrada en
   `examples/invalid/expected.json`, con la ruta y la keyword que deben fallar.
3. `python3 ../docs/contract/validate.py` en verde. No sigas si no lo está. Valida los
   ejemplos, comprueba que los inválidos fallan donde se espera, y pasa el lint de modo
   estricto sobre los dos schemas del LLM.
4. `scripts/sync-contract.sh` en **los dos repos**. Ojo: ese script todavía no existe;
   mientras no exista, copia a mano y dilo en el PR.
5. Adapta los DTOs de `:core:ai` (paquete `contract`) y sus tests de ida y vuelta contra
   `contract/v1/examples/`. En la Lambda, lo equivalente.
6. Un PR por repo, con referencia cruzada al otro.
7. Si rompe compatibilidad, escribe el ADR.

## Cuándo no está terminado

Una tarea que toca el contrato **no se marca `done`** hasta que los dos repos pasan sus
tests de contrato con la misma versión. Un repo en verde y el otro sin tocar es el estado
peor posible: compila, y falla en producción.

## Con OpenSpec

Una change de este repo **no edita `../docs/contract/`** por su cuenta. Si una change
necesita un cambio de contrato, su proposal lo dice, y el cambio se hace como su propia
change dividida por repo.
