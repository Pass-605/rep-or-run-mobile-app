Cubre la tarea **C-06** de `../docs/backlog/C-diseno-ui.md` (Perfil, formulario en 2 pasos).

## Why

Tras aceptar el aviso se llega a un destino provisional que solo dice que el perfil no existe. `SaveProfileUseCase` (B-05) valida y guarda, pero ninguna pantalla lo llama, y sin perfil `BuildGenerationSummaryUseCase` no deja generar.

## What Changes

- **`:feature:profile`**, módulo nuevo: la pantalla 02 de Figma repartida en dos pasos. Paso 1: alias, edad, altura, peso, cintura, cadera y experiencia. Paso 2: objetivo, músculos prioritarios, limitaciones y preferencias.
- **Tarjeta del objetivo**: al elegirlo explica qué significa y cómo cambia el plan (repeticiones, volumen, descanso y cardio), con la nota fija de que ningún objetivo incluye pautas de alimentación. Son textos constantes de la app, uno por objetivo.
- **Validación en línea**: los errores salen junto a su campo. Los límites los decide el caso de uso; la pantalla solo los enseña.
- **Edición**: con un perfil guardado, el formulario se abre precargado.
- **`ValidateProfileUseCase`** en `:core:domain`: las mismas reglas que guardar, sin guardar, para poder avanzar de paso y para el tope de prioridades.
- **Raíz**: el perfil es el destino tras el consentimiento. Se borra `StartPlaceholderScreen` con sus cuatro textos.
- `android:windowSoftInputMode="adjustResize"` en la actividad: es la primera pantalla con campos y sin él el teclado tapa la acción.

## No objetivos

- El paso siguiente del flujo (gimnasio, C-07): al guardar se vuelve a entrar al perfil.
- Ajustes (Q-01) y la entrada «editar perfil» desde allí. Hasta entonces, el aviso completo deja de tener quien lo abra después de aceptar.
- Cambiar límites: son los de `ProfileRules` y `UserProfile`. El tope de 4 prioridades es del contrato.
- Exigir en el prompt los rangos que promete la tarjeta (pendiente 3 de `../docs/design/README.md`).
- Arreglar Figma.

## Capabilities

### New Capabilities

- `perfil`: capturar y editar el perfil de la persona en dos pasos.

### Modified Capabilities

Ninguna archivada. Cambia de hecho el «destino siguiente» de `onboarding` (C-05, sin archivar): ahora es el perfil.

## Impact

- **Código:** módulo nuevo `feature:profile` (previsto en el plan §9 y en `CLAUDE.md` §1); `composeApp`, `core:domain`, `core:designsystem` (medidas), `androidApp` (manifest), `settings.gradle.kts`.
- **Dependencias:** ninguna nueva en el catálogo.
- **Contrato:** no se toca.
- **Figma:** no se toca. Se lee la pantalla 02 en las dos páginas v2.
- **Plan:** no se toca.
- **Textos que necesitan revisión antes de publicar:** los de la tarjeta para hipertrofia, fuerza y salud general (Figma solo trae recomposición) y el aviso de privacidad del texto libre.
