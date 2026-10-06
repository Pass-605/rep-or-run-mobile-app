package app.reporrun.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.reporrun.feature.onboarding.OnboardingGraphRoute
import app.reporrun.feature.onboarding.navigateToNotice
import app.reporrun.feature.onboarding.onboardingGraph
import app.reporrun.feature.profile.ProfileGraphRoute
import app.reporrun.feature.profile.navigateToProfileGoal
import app.reporrun.feature.profile.profileGraph

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
                navController.navigate(ProfileGraphRoute) {
                    // La bienvenida sale de la pila: atrás no vuelve a pedir el consentimiento.
                    popUpTo<OnboardingGraphRoute> { inclusive = true }
                }
            },
            onReadNotice = navController::navigateToNotice,
            onBack = { navController.popBackStack() },
        )
        profileGraph(
            // El ViewModel del perfil vive lo que su grafo: los dos pasos lo comparten.
            graphOwner = { navController.getBackStackEntry(ProfileGraphRoute) },
            onContinue = navController::navigateToProfileGoal,
            onBack = { navController.popBackStack() },
            onProfileSaved = {
                // Todavía no hay a dónde seguir: el gimnasio es C-07, que cambia este destino.
                // Hasta entonces se vuelve a entrar al perfil, que se abre con lo guardado.
                navController.navigate(ProfileGraphRoute) {
                    popUpTo<ProfileGraphRoute> { inclusive = true }
                }
            },
        )
    }
}
