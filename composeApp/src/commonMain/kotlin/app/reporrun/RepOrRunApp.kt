package app.reporrun

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.model.ThemeMode
import app.reporrun.feature.gym.GymGraphRoute
import app.reporrun.feature.onboarding.OnboardingGraphRoute
import app.reporrun.feature.plan.PlanRoute
import app.reporrun.feature.profile.ProfileGraphRoute
import app.reporrun.navigation.RepOrRunNavHost
import org.koin.compose.viewmodel.koinViewModel

/**
 * Punto de entrada común: el tema, el fondo y la navegación raíz. Koin ya está arrancado
 * cuando esto se compone (`RepOrRunApplication` en Android, `MainViewController` en iOS).
 *
 * El tema es el elegido en Ajustes. Como envuelve a la navegación, cambiarlo recompone lo que
 * se ve sin tocar la pila.
 *
 * @param onDarkThemeChange se llama con el tema ya resuelto, al empezar y cada vez que cambia.
 *   Es para lo que el tema no alcanza: los iconos de las barras del sistema, que son de la
 *   ventana de cada plataforma.
 */
@Composable
fun RepOrRunApp(onDarkThemeChange: (darkTheme: Boolean) -> Unit = {}) {
    val viewModel: AppViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val darkTheme = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val currentOnDarkThemeChange by rememberUpdatedState(onDarkThemeChange)
    LaunchedEffect(darkTheme) { currentOnDarkThemeChange(darkTheme) }

    RepOrRunTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (state.start) {
                // Un instante, lo que tardan DataStore y Room: se ve el fondo, sin indicador.
                StartDestination.Loading -> Unit

                StartDestination.Onboarding -> RepOrRunNavHost(startDestination = OnboardingGraphRoute)

                StartDestination.Profile -> RepOrRunNavHost(startDestination = ProfileGraphRoute)

                StartDestination.Gyms -> RepOrRunNavHost(startDestination = GymGraphRoute)

                StartDestination.Plan -> RepOrRunNavHost(startDestination = PlanRoute())
            }
        }
    }
}
