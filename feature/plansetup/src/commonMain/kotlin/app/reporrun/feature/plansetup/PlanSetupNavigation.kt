package app.reporrun.feature.plansetup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import app.reporrun.core.model.GymId
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** La entrada al grafo: las preferencias del bloque. */
@Serializable
data object PlanSetupGraphRoute

@Serializable
internal data object PreferencesRoute

@Serializable
internal data object SummaryRoute

/** Abre las preferencias. `launchSingleTop`: dos toques seguidos no las apilan dos veces. */
fun NavController.navigateToPlanSetup() {
    navigate(PlanSetupGraphRoute) { launchSingleTop = true }
}

/** Abre las preferencias encima de donde se esté: es como se editan desde el resumen. */
fun NavController.navigateToPreferences() {
    navigate(PreferencesRoute) { launchSingleTop = true }
}

/**
 * Sigue de las preferencias al resumen, **sacándolas de la pila**: el formulario lee lo guardado
 * una sola vez, y si se quedara debajo, «atrás» desde el resumen lo enseñaría con lo que tenía
 * antes de editarlo desde aquí. Atrás desde el resumen vuelve a lo que hubiera antes de ellas.
 */
fun NavController.navigateToSummary() {
    navigate(SummaryRoute) {
        popUpTo<PreferencesRoute> { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Vuelve al resumen si está en la pila, cerrando lo que tenga encima, y dice si lo ha hecho.
 * Con `false` la pila no se toca: no se entró desde el resumen y quien llama sigue su camino.
 */
fun NavController.popBackToSummary(): Boolean = popBackStack<SummaryRoute>(inclusive = false)

/**
 * El grafo de la preparación del plan: las preferencias y el resumen previo a generar.
 *
 * @param onBack volver al destino anterior.
 * @param onPreferencesSaved las preferencias ya están guardadas.
 * @param onEditProfile abrir el perfil desde el resumen.
 * @param onEditGym abrir el gimnasio activo desde el resumen; `null` si no hay ninguno.
 * @param onEditPreferences abrir las preferencias desde el resumen.
 * @param onGenerate no falta nada y la persona ha pedido generar.
 */
@Suppress("LongParameterList") // Una lambda por salida: es la forma de todos los grafos.
fun NavGraphBuilder.planSetupGraph(
    onBack: () -> Unit,
    onPreferencesSaved: () -> Unit,
    onEditProfile: () -> Unit,
    onEditGym: (GymId?) -> Unit,
    onEditPreferences: () -> Unit,
    onGenerate: () -> Unit,
) {
    navigation<PlanSetupGraphRoute>(startDestination = PreferencesRoute) {
        composable<PreferencesRoute> {
            PreferencesRoute(
                viewModel = koinViewModel(),
                onBack = onBack,
                onPreferencesSaved = onPreferencesSaved,
            )
        }
        composable<SummaryRoute> {
            SummaryRoute(
                viewModel = koinViewModel(),
                actions = SummaryActions(
                    onBack = onBack,
                    onEditProfile = onEditProfile,
                    onEditGym = onEditGym,
                    onEditPreferences = onEditPreferences,
                ),
                onGenerate = onGenerate,
            )
        }
    }
}
