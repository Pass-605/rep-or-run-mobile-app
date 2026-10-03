package app.reporrun.core.domain

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Los dispatchers de la app, como un solo tipo que se inyecta. La lógica nunca escribe
 * `Dispatchers.IO`: pide este tipo y usa el que le toca, y así un test puede pasar el suyo.
 *
 * Es una clase y no tres definiciones con nombre (`named("io")`) porque un nombre es un texto
 * que el compilador no comprueba.
 *
 * @property main el hilo de la UI.
 * @property default trabajo de CPU: ordenar, mapear listas grandes, validar.
 * @property io lo que bloquea: disco y red.
 */
class AppDispatchers(val main: CoroutineDispatcher, val default: CoroutineDispatcher, val io: CoroutineDispatcher) {
    /** Los tres iguales: para tests, con un `StandardTestDispatcher` o un `UnconfinedTestDispatcher`. */
    constructor(all: CoroutineDispatcher) : this(main = all, default = all, io = all)
}
