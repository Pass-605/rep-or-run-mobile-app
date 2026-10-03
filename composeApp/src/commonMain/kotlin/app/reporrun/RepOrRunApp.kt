package app.reporrun

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.navigation.RepOrRunNavHost

/**
 * Punto de entrada común: el tema, el fondo y la navegación raíz. Koin ya está arrancado
 * cuando esto se compone (`RepOrRunApplication` en Android, `MainViewController` en iOS).
 */
@Composable
fun RepOrRunApp() {
    RepOrRunTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            RepOrRunNavHost()
        }
    }
}
