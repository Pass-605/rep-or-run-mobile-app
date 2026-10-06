package app.reporrun.feature.profile

import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** La entrada al grafo del perfil: para crearlo la primera vez y para editarlo después. */
@Serializable
data object ProfileGraphRoute

@Serializable
internal data object ProfileBasicsRoute

@Serializable
internal data object ProfileGoalRoute

/** Pasa al paso 2. `launchSingleTop`: dos toques seguidos en «Continuar» no lo apilan dos veces. */
fun NavController.navigateToProfileGoal() {
    navigate(ProfileGoalRoute) { launchSingleTop = true }
}

/**
 * El grafo del perfil: dos destinos, uno por paso, con **un** ViewModel. Que sean dos destinos
 * hace que el «atrás» del sistema vuelva del paso 2 al 1 sin código propio; que compartan
 * ViewModel hace que lo tecleado no se pierda al ir y volver.
 *
 * @param graphOwner el dueño del ViewModel compartido: la entrada de [ProfileGraphRoute] en la
 *   pila. La da `composeApp`, que es quien tiene el `NavController`.
 * @param onContinue el paso 1 está completo.
 * @param onBack volver del paso 2 al 1.
 * @param onProfileSaved el perfil ya está guardado.
 */
fun NavGraphBuilder.profileGraph(
    graphOwner: () -> ViewModelStoreOwner,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
) {
    navigation<ProfileGraphRoute>(startDestination = ProfileBasicsRoute) {
        composable<ProfileBasicsRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            ProfileBasicsRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onContinue = onContinue,
                onBack = onBack,
                onProfileSaved = onProfileSaved,
            )
        }
        composable<ProfileGoalRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            ProfileGoalRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onContinue = onContinue,
                onBack = onBack,
                onProfileSaved = onProfileSaved,
            )
        }
    }
}
