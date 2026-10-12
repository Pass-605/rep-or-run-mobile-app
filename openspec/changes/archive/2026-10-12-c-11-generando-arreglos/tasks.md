## 1. Restaurarse no genera

- [x] 1.1 Marca `generation_started` en `SavedStateHandle`; `GenerationFailure.Interrupted` con su título, su cuerpo y el pie «Generar otra vez»; `SavedStateHandle` en los `extraTypes` de `AppGraphTest`
- [x] 1.2 Tests: restaurado con la marca no pide plan y expone `Interrupted`; desde ahí la persona puede generar; la interrupción no cuenta para el tope; entrar de nuevo genera

## 2. Espera tras un límite de peticiones

- [x] 2.1 `secondsUntilRetry` en el estado; cuenta atrás contra el `Clock` inyectado; pie deshabilitado con «Reintentar en N s»; el ViewModel ignora el reintento mientras dure
- [x] 2.2 Tests con reloj y tiempo virtuales: arranca en los segundos pedidos, baja con el reloj, no pide antes de tiempo, deja reintentar al terminar, sin segundos reintenta de inmediato, una espera nueva no hereda la cuenta

## 3. Problemas como detalle técnico

- [x] 3.1 Tarjeta «Detalle técnico», sin acento, con su línea de aviso

## 4. Verificación

- [x] 4.1 `gradlew.bat check`: BUILD SUCCESSFUL. `:feature:plan:allTests`: 27 tests, 0 fallos
- [x] 4.2 Emulador `Medium_Phone` (API 28), build normal: «Generar plan», inicio, `am kill app.reporrun` hasta que no hay proceso, con la espera en pantalla. Tabla `plan` antes, 6 s después de morir y 9 s después de volver: las mismas dos filas. La pantalla dice «La generación se interrumpió» y ofrece «Generar otra vez», que guarda `v3`; volver al resumen y generar de nuevo guarda `v4`
- [x] 4.3 Con `FakeAiConfig(failure = AiError.RateLimited(9))` puesto a mano y retirado: pie deshabilitado «Reintentar en 9 s», pulsarlo no hace nada («…en 7 s» en la captura siguiente), y a los 9 s «Reintentar» habilitado
- [x] 4.4 Con `FakeAiConfig(failure = AiError.InvalidResponse(…))`, igual de temporal: tarjeta «Detalle técnico» en claro y en oscuro, con uno y con dos planes inválidos
- [x] 4.5 Sin `FATAL EXCEPTION` en logcat. Capturas en `captures/C-11/`
- [ ] 4.6 Sin probar: «No conservar actividades», muerte del proceso con un error en pantalla, y la cuenta atrás con la app en segundo plano
- [ ] 4.7 iOS: todo es `commonMain`; sin compilar ni ejecutar (`needs:macos`)
