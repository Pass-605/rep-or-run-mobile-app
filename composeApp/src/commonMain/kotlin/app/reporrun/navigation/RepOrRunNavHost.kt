package app.reporrun.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.reporrun.di.AppInfo
import app.reporrun.feature.gym.GymGraphRoute
import app.reporrun.feature.gym.gymGraph
import app.reporrun.feature.gym.navigateToEquipmentEditor
import app.reporrun.feature.gym.navigateToGymEditor
import app.reporrun.feature.gym.navigateToInventory
import app.reporrun.feature.onboarding.OnboardingGraphRoute
import app.reporrun.feature.onboarding.navigateToNotice
import app.reporrun.feature.onboarding.onboardingGraph
import app.reporrun.feature.plan.navigateToGeneratedPlan
import app.reporrun.feature.plan.navigateToGenerating
import app.reporrun.feature.plan.planGraph
import app.reporrun.feature.plansetup.navigateToPlanSetup
import app.reporrun.feature.plansetup.navigateToPreferences
import app.reporrun.feature.plansetup.navigateToSummary
import app.reporrun.feature.plansetup.planSetupGraph
import app.reporrun.feature.plansetup.popBackToSummary
import app.reporrun.feature.profile.ProfileGraphRoute
import app.reporrun.feature.profile.navigateToProfileGoal
import app.reporrun.feature.profile.profileGraph
import app.reporrun.feature.settings.navigateToLicenses
import app.reporrun.feature.settings.navigateToSettings
import app.reporrun.feature.settings.popBackToSettings
import app.reporrun.feature.settings.settingsGraph
import org.koin.compose.koinInject

/**
 * La navegación raíz: el único sitio que tiene el `NavController` y que conoce todos los
 * grafos. Cada feature aporta su `xxxGraph(...)` y aquí se conectan sus salidas.
 *
 * **Las salidas de los tres formularios dependen de por dónde se entró**, y eso se lee de la
 * pila, no de ningún estado: si el resumen previo a generar está debajo, se entró a editar desde
 * él y guardar vuelve a él (`popBackToSummary()`); si no, es el alta y se sigue la cadena perfil
 * → gimnasios → inventario → preferencias → resumen. Los formularios no saben cuál de los dos
 * casos es: solo avisan de que han guardado.
 *
 * El perfil tiene un tercer origen, Ajustes, y se resuelve igual: mirando la pila.
 *
 * @param startDestination la ruta por la que se empieza; la decide `AppViewModel`.
 */
@Composable
internal fun RepOrRunNavHost(
    startDestination: Any,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val appInfo = koinInject<AppInfo>()
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
            // En el alta el perfil es lo primero de la pila y su paso 1 no tiene a dónde volver;
            // abierto desde el resumen o desde Ajustes, sí.
            canLeave = { navController.previousBackStackEntry != null },
            onContinue = navController::navigateToProfileGoal,
            onBack = { navController.popBackStack() },
            onProfileSaved = navController::leaveSavedProfile,
        )
        gymGraph(
            // Los ViewModels de gimnasios viven lo que su grafo: cada lista comparte el suyo con
            // su formulario. Si el grafo está dos veces en la pila (el del alta y el que se abre
            // desde el resumen), `getBackStackEntry` da el de arriba, que es el que se ve.
            graphOwner = { navController.getBackStackEntry(GymGraphRoute) },
            onOpenEditor = navController::navigateToGymEditor,
            onOpenInventory = navController::navigateToInventory,
            onOpenEquipmentEditor = navController::navigateToEquipmentEditor,
            onBack = { navController.popBackStack() },
            onInventoryContinue = { if (!navController.popBackToSummary()) navController.navigateToPlanSetup() },
        )
        planSetupGraph(
            onBack = { navController.popBackStack() },
            onPreferencesSaved = { if (!navController.popBackToSummary()) navController.navigateToSummary() },
            onEditProfile = { navController.navigate(ProfileGraphRoute) { launchSingleTop = true } },
            // Con gimnasio activo se va a sus máquinas, que es lo que cuenta el resumen; sin
            // ninguno, a la lista, donde se crea o se elige.
            onEditGym = { gymId ->
                if (gymId == null) {
                    navController.navigate(GymGraphRoute) { launchSingleTop = true }
                } else {
                    navController.navigateToInventory(gymId)
                }
            },
            onEditPreferences = navController::navigateToPreferences,
            // Abrir la espera es pedir el plan: la generación empieza con ella.
            onGenerate = navController::navigateToGenerating,
        )
        planGraph(
            // Como destino inicial el plan no tiene a dónde volver; tras generar, sí: al resumen.
            canLeave = { navController.previousBackStackEntry != null },
            onBack = { navController.popBackStack() },
            // A la espera solo se llega desde el resumen, así que está justo debajo.
            onBackToSummary = { navController.popBackStack() },
            onPlanGenerated = navController::navigateToGeneratedPlan,
            // Desde el plan, el resumen es la puerta a todo lo demás: el perfil, el gimnasio, las
            // preferencias y generar otro.
            onOpenSummary = navController::navigateToSummary,
            onOpenSettings = navController::navigateToSettings,
        )
        settingsGraph(
            appVersion = appInfo.versionName,
            showDeveloperOptions = appInfo.isDebug,
            onBack = { navController.popBackStack() },
            // La misma ruta que usa el resumen: no hay una segunda forma de editar el perfil.
            onEditProfile = { navController.navigate(ProfileGraphRoute) { launchSingleTop = true } },
            onReadNotice = navController::navigateToNotice,
            onOpenLicenses = navController::navigateToLicenses,
        )
    }
}

/**
 * A dónde se va con el perfil ya guardado: a quien lo abrió, o al paso siguiente del alta.
 *
 * Ajustes se pregunta primero: desde él no se llega al resumen, así que si está en la pila es
 * quien abrió el perfil. El resumen, en cambio, puede estar más abajo (plan → resumen → generar
 * → plan → ajustes), y volver a él desde aquí cerraría Ajustes y el plan de un salto.
 */
private fun NavHostController.leaveSavedProfile() {
    if (popBackToSettings() || popBackToSummary()) return
    navigate(GymGraphRoute) {
        // El perfil sale de la pila: su formulario ya se guardó y atrás no lo reabre a medias.
        // Se vuelve a abrir desde el resumen o desde Ajustes.
        popUpTo<ProfileGraphRoute> { inclusive = true }
    }
}
