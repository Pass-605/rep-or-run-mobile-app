package app.reporrun.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.reporrun.scaffold.ScaffoldGraphRoute
import app.reporrun.scaffold.navigateToScaffoldDetail
import app.reporrun.scaffold.scaffoldGraph

/**
 * La navegación raíz: el único sitio que tiene el `NavController` y que conoce todos los
 * grafos. Cada feature aporta su `xxxGraph(...)` y aquí se conectan sus salidas.
 */
@Composable
internal fun RepOrRunNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = ScaffoldGraphRoute, modifier = modifier) {
        scaffoldGraph(
            onGymCreated = navController::navigateToScaffoldDetail,
            onBack = { navController.popBackStack() },
        )
    }
}
