package app.reporrun.feature.gym

import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** La entrada al grafo de gimnasios: la lista, con el estado vacío si aún no hay ninguno. */
@Serializable
data object GymGraphRoute

@Serializable
internal data object GymListRoute

@Serializable
internal data object GymEditorRoute

/** Abre el formulario del nombre. `launchSingleTop`: dos toques seguidos no lo apilan dos veces. */
fun NavController.navigateToGymEditor() {
    navigate(GymEditorRoute) { launchSingleTop = true }
}

/**
 * El grafo de gimnasios: la lista y el formulario del nombre, con **un** ViewModel, como el
 * perfil. Que el formulario sea un destino hace que el «atrás» del sistema lo cierre sin código
 * propio.
 *
 * @param graphOwner el dueño del ViewModel compartido: la entrada de [GymGraphRoute] en la pila.
 *   La da `composeApp`, que es quien tiene el `NavController`.
 * @param onOpenEditor el formulario está listo para mostrarse.
 * @param onCloseEditor volver del formulario a la lista, al guardar o con el botón de volver.
 */
fun NavGraphBuilder.gymGraph(
    graphOwner: () -> ViewModelStoreOwner,
    onOpenEditor: () -> Unit,
    onCloseEditor: () -> Unit,
) {
    navigation<GymGraphRoute>(startDestination = GymListRoute) {
        composable<GymListRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            GymListRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onOpenEditor = onOpenEditor,
                onCloseEditor = onCloseEditor,
            )
        }
        composable<GymEditorRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            GymEditorRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onOpenEditor = onOpenEditor,
                onCloseEditor = onCloseEditor,
            )
        }
    }
}
