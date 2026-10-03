package app.reporrun

import androidx.compose.ui.window.ComposeUIViewController
import app.reporrun.di.iosPlatformModule
import app.reporrun.di.startRepOrRunKoin
import platform.UIKit.UIViewController

// Koin se arranca una vez por proceso. `lazy` lo garantiza aunque SwiftUI cree el
// controlador más de una vez, y evita tener que llamar a nada desde Swift.
private val koin by lazy { startRepOrRunKoin(iosPlatformModule) }

@Suppress("unused", "FunctionName") // lo llama iosApp desde Swift
fun MainViewController(): UIViewController {
    koin
    return ComposeUIViewController { RepOrRunApp() }
}
