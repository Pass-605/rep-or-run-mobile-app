Cubre la tarea **B-06** de `../docs/backlog/B-dominio-datos.md` (PlanValidator).

## Why

Un plan generado tiene la forma del modelo de dominio, pero nadie ha cruzado todavía sus ejercicios con el inventario, sus días con los disponibles ni su volumen con los minutos de sesión. El mapeo de `:core:ai` (A-14) solo mira cada pieza por separado. Sin esta segunda red, B-07 no tiene con qué decidir si un plan se puede guardar.

## What Changes

- `PlanValidator` en `:core:domain`: aplica las reglas del contrato (`../docs/contract/README.md`, reglas 1 a 6) que exigen cruzar datos: inventario (1), días (2), códigos (4) y volumen frente a los minutos (6). Las reglas 3 y 5 son invariantes de `:core:model` y las comprueba el mapeo antes de construir; aquí no se repiten.
- Resultado tipado con **todas** las infracciones: regla, día, ejercicio, ruta del campo y un mensaje listo para un prompt de reparación.
- La regla 6 es una heurística; su fórmula, supuestos y margen quedan en `design.md`.
- `ModelErrors` y `Mapped` dejan de estar duplicados en `:core:ai` y `:core:data`: una sola definición, pública, en `:core:domain`.
- El plan sin red se recorta para caber en los minutos de sesión: con 45 minutos, el ejemplo del contrato (pensado para 75) no pasa la regla 6 tal cual.
- La letra del día de los códigos pasa a tener una sola definición, en `:core:domain`.

## No objetivos

- `ProposalValidator` y la regla 7 (revisión): v1.1.
- Orquestar la generación, descartar el plan o informar a la persona: B-07.
- Leer el descanso de cada ejercicio (`rest` es texto libre) para estimar el tiempo.
- Reglas que el contrato no numera. Un plan sin días o un día sin ejercicios **no incumple ninguna de las seis**; se anota como hueco del contrato, no se inventa aquí.
- Medir cobertura con una herramienta: el repo no tiene Kover ni JaCoCo, y añadirlo es una dependencia que se decide aparte.

## Capabilities

### New Capabilities

- `validacion-plan`: qué plan generado acepta la app y qué dice cuando lo rechaza.

### Modified Capabilities

- `ia-sin-red`: el plan sin red cabe en los minutos de sesión de la persona.

## Impact

- **Código:** `:core:domain` (paquete nuevo `validation`, `domainModule`), `:core:ai` (imports, recorte del fake, sus tests) y `:core:data` (imports). Sin dependencias nuevas, ni entre módulos ni en el catálogo.
- **Contrato:** no se toca. Se aplica tal cual; la copia `contract/v1/` no cambia.
- **Plan y Figma:** no se tocan.
