package app.reporrun

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.feature.onboarding.OnboardingGraphRoute
import app.reporrun.navigation.RepOrRunNavHost
import app.reporrun.start.StartPlaceholderRoute
import org.koin.compose.viewmodel.koinViewModel

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
            val viewModel: AppViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            when (state.start) {
                // Un instante, lo que tarda DataStore: se ve el fondo, sin indicador.
                StartDestination.Loading -> Unit

                StartDestination.Onboarding -> RepOrRunNavHost(startDestination = OnboardingGraphRoute)

                StartDestination.Main -> RepOrRunNavHost(startDestination = StartPlaceholderRoute)
            }
        }
    }
}
