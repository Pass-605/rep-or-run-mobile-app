package app.reporrun.feature.plansetup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** La entrada al grafo: las preferencias del bloque. */
@Serializable
data object PlanSetupGraphRoute

@Serializable
internal data object PreferencesRoute

/** Abre las preferencias. `launchSingleTop`: dos toques seguidos no las apilan dos veces. */
fun NavController.navigateToPlanSetup() {
    navigate(PlanSetupGraphRoute) { launchSingleTop = true }
}

/**
 * El grafo de la preparación del plan. Hoy tiene un destino, las preferencias; el resumen previo
 * a generar (C-10) será el segundo.
 *
 * @param onBack volver al destino anterior.
 * @param onPreferencesSaved las preferencias ya están guardadas.
 */
fun NavGraphBuilder.planSetupGraph(onBack: () -> Unit, onPreferencesSaved: () -> Unit) {
    navigation<PlanSetupGraphRoute>(startDestination = PreferencesRoute) {
        composable<PreferencesRoute> {
            PreferencesRoute(
                viewModel = koinViewModel(),
                onBack = onBack,
                onPreferencesSaved = onPreferencesSaved,
            )
        }
    }
}
