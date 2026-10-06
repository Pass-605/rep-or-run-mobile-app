package app.reporrun.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.reporrun.feature.onboarding.OnboardingGraphRoute
import app.reporrun.feature.onboarding.navigateToNotice
import app.reporrun.feature.onboarding.onboardingGraph
import app.reporrun.start.StartPlaceholderRoute
import app.reporrun.start.StartPlaceholderScreen

/**
 * La navegación raíz: el único sitio que tiene el `NavController` y que conoce todos los
 * grafos. Cada feature aporta su `xxxGraph(...)` y aquí se conectan sus salidas.
 *
 * @param startDestination la ruta por la que se empieza; la decide `AppViewModel`.
 */
@Composable
internal fun RepOrRunNavHost(
    startDestination: Any,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        onboardingGraph(
            onConsentAccepted = {
                navController.navigate(StartPlaceholderRoute) {
                    // La bienvenida sale de la pila: atrás no vuelve a pedir el consentimiento.
                    popUpTo<OnboardingGraphRoute> { inclusive = true }
                }
            },
            onReadNotice = navController::navigateToNotice,
            onBack = { navController.popBackStack() },
        )
        composable<StartPlaceholderRoute> {
            StartPlaceholderScreen(onOpenNotice = navController::navigateToNotice)
        }
    }
}
