package app.reporrun.di

/**
 * Lo que la app sabe de su propio paquete. Lo aporta el módulo de plataforma, que es quien lo
 * puede leer; la navegación raíz se lo pasa a Ajustes.
 *
 * @property versionName la versión instalada (`versionName` en Android,
 *   `CFBundleShortVersionString` en iOS); vacía si la plataforma no la da.
 * @property isDebug si la build es de depuración (`FLAG_DEBUGGABLE` en Android,
 *   `Platform.isDebugBinary` en iOS). Solo entonces existen las opciones de desarrollo de
 *   Ajustes y se puede generar con los ejemplos sin red.
 */
internal class AppInfo(val versionName: String, val isDebug: Boolean)
