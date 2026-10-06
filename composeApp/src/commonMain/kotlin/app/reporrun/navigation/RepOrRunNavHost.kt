package app.reporrun.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.reporrun.feature.gym.GymGraphRoute
import app.reporrun.feature.gym.gymGraph
import app.reporrun.feature.gym.navigateToEquipmentEditor
import app.reporrun.feature.gym.navigateToGymEditor
import app.reporrun.feature.gym.navigateToInventory
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
                navController.navigate(GymGraphRoute) {
                    // El perfil sale de la pila: su formulario ya se guardó y atrás no lo reabre
                    // a medias. Editarlo después es cosa de Ajustes.
                    popUpTo<ProfileGraphRoute> { inclusive = true }
                }
            },
        )
        gymGraph(
            // Los ViewModels de gimnasios viven lo que su grafo: cada lista comparte el suyo con
            // su formulario.
            graphOwner = { navController.getBackStackEntry(GymGraphRoute) },
            onOpenEditor = navController::navigateToGymEditor,
            onOpenInventory = navController::navigateToInventory,
            onOpenEquipmentEditor = navController::navigateToEquipmentEditor,
            onBack = { navController.popBackStack() },
        )
    }
}
