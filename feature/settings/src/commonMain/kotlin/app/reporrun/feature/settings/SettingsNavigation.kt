package app.reporrun.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** La entrada al grafo: la pantalla de ajustes. */
@Serializable
data object SettingsGraphRoute

@Serializable
internal data object SettingsRoute

@Serializable
internal data object LicensesRoute

/** Abre Ajustes. `launchSingleTop`: dos toques seguidos no lo apilan dos veces. */
fun NavController.navigateToSettings() {
    navigate(SettingsGraphRoute) { launchSingleTop = true }
}

/** Abre las licencias, encima de Ajustes. */
fun NavController.navigateToLicenses() {
    navigate(LicensesRoute) { launchSingleTop = true }
}

/**
 * Vuelve a Ajustes si está en la pila, cerrando lo que tenga encima, y dice si lo ha hecho. Con
 * `false` la pila no se toca: no se entró desde Ajustes y quien llama sigue su camino.
 */
fun NavController.popBackToSettings(): Boolean = popBackStack<SettingsRoute>(inclusive = false)

/**
 * El grafo de Ajustes: la pantalla y sus licencias.
 *
 * @param appVersion la versión instalada, tal como la da la plataforma.
 * @param showDeveloperOptions si se enseña la sección de desarrollo. Solo en una build de
 *   depuración: lo decide la raíz de composición.
 * @param onBack volver al destino anterior.
 * @param onEditProfile abrir el perfil para editarlo.
 * @param onReadNotice abrir el aviso completo.
 * @param onOpenLicenses abrir las licencias.
 */
fun NavGraphBuilder.settingsGraph(
    appVersion: String,
    showDeveloperOptions: Boolean,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onReadNotice: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    navigation<SettingsGraphRoute>(startDestination = SettingsRoute) {
        composable<SettingsRoute> {
            SettingsRoute(
                viewModel = koinViewModel(),
                appVersion = appVersion,
                showDeveloperOptions = showDeveloperOptions,
                actions = SettingsActions(
                    onBack = onBack,
                    onEditProfile = onEditProfile,
                    onReadNotice = onReadNotice,
                    onOpenLicenses = onOpenLicenses,
                ),
            )
        }
        composable<LicensesRoute> {
            LicensesScreen(onBack = onBack)
        }
    }
}
