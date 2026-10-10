Cubre la tarea **C-10** de `../docs/backlog/C-diseno-ui.md` (Resumen previo a generar).

## Why

Tras guardar las preferencias no hay a dónde seguir: se vuelve al inventario. `BuildGenerationSummaryUseCase` (B-05) ya sabe qué se va a enviar y qué falta, pero ninguna pantalla lo enseña, y no hay forma de corregir el perfil una vez guardado: su formulario sale de la pila.

## What Changes

- **`:feature:plansetup`**: el resumen previo a generar (pantalla 05 de Figma): tres cifras (días por semana, máquinas, minutos por sesión), una fila por perfil, gimnasio y semana que abre su formulario, y qué se envía. «Generar plan» se deshabilita si falta algo, con todos los motivos a la vista.
- **Raíz**: las salidas de los tres formularios dependen de por dónde se entró. En el alta siguen la cadena, que ahora acaba en el resumen; abiertos desde el resumen, guardar vuelve a él.
- **Preferencias**: el pie dice «Continuar»; los minutos por sesión vienen con 60 marcado (decisión de producto del 08/10).
- **Perfil**: el paso 1 tiene botón de volver cuando hay a dónde.
- **`:core:designsystem`**: `RepOrRunStatCard`, la tarjeta de cifra.
- **Destino provisional** tras «Generar plan», en `composeApp`, hasta C-11.
- Skill `reporrun-pantalla-figma`: los dos nodos de la 05 en la tabla.

## No objetivos

- Generar el plan ni la pantalla Generando (C-11).
- Cambiar qué bloquea la generación: es de `BuildGenerationSummaryUseCase`.
- Ajustes (Q-01), que reutilizará la reapertura del perfil.
- Volver a pedir el consentimiento desde el resumen si falta.
- Cambiar el destino inicial de la app.
- Arreglar Figma (C-01).

## Capabilities

### New Capabilities

- `resumen-previo`: revisar lo que se envía, corregirlo y pedir la generación.

### Modified Capabilities

Ninguna archivada. Cambian de hecho `preferencias-bloque` (`c-09-preferencias`, sin archivar): minutos por defecto y a dónde lleva guardar; y `perfil` (`c-06-perfil`): se puede reabrir.

## Impact

- **Código:** `feature:plansetup`, `feature:profile`, `core:designsystem`, `composeApp`.
- **Dependencias:** ninguna nueva en el catálogo ni entre módulos.
- **Contrato:** no se toca.
- **Plan y Figma:** no se tocan. Las desviaciones de la 05 quedan anotadas para C-01.
- **Textos que necesitan revisión antes de publicar:** los motivos de bloqueo y la tarjeta «Qué se envía», que está cerca de un texto de consentimiento y no copia a Figma palabra por palabra (ver diseño).
