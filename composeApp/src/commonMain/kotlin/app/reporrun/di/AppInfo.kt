package app.reporrun.di

/**
 * Lo que la app sabe de su propio paquete. Lo aporta el módulo de plataforma, que es quien lo
 * puede leer; la navegación raíz se lo pasa a Ajustes.
 *
 * @property versionName la versión instalada (`versionName` en Android,
 *   `CFBundleShortVersionString` en iOS); vacía si la plataforma no la da.
 */
internal class AppInfo(val versionName: String)
