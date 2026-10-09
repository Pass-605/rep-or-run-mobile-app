package app.reporrun.feature.gym

import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import app.reporrun.core.model.GymId
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** La entrada al grafo de gimnasios: la lista, con el estado vacío si aún no hay ninguno. */
@Serializable
data object GymGraphRoute

@Serializable
internal data object GymListRoute

@Serializable
internal data object GymEditorRoute

/** El inventario de un gimnasio. El id viaja como texto: una ruta solo lleva tipos simples. */
@Serializable
internal data class InventoryRoute(val gymId: String)

@Serializable
internal data object EquipmentEditorRoute

/** Abre el formulario del nombre. `launchSingleTop`: dos toques seguidos no lo apilan dos veces. */
fun NavController.navigateToGymEditor() {
    navigate(GymEditorRoute) { launchSingleTop = true }
}

/** Abre el inventario de [gymId], encima de la lista. */
fun NavController.navigateToInventory(gymId: GymId) {
    navigate(InventoryRoute(gymId.value)) { launchSingleTop = true }
}

/** Abre el formulario de una máquina, encima del inventario. */
fun NavController.navigateToEquipmentEditor() {
    navigate(EquipmentEditorRoute) { launchSingleTop = true }
}

/**
 * El grafo de gimnasios: la lista y el formulario del nombre, con **un** ViewModel, y el
 * inventario de un gimnasio y el formulario de una máquina, con otro. Que cada formulario sea un
 * destino hace que el «atrás» del sistema lo cierre sin código propio.
 *
 * @param graphOwner el dueño de los ViewModels compartidos: la entrada de [GymGraphRoute] en la
 *   pila. La da `composeApp`, que es quien tiene el `NavController`.
 * @param onOpenEditor el formulario del nombre está listo para mostrarse.
 * @param onOpenInventory ver las máquinas de ese gimnasio.
 * @param onOpenEquipmentEditor el formulario de una máquina está listo para mostrarse.
 * @param onBack volver al destino anterior: de un formulario a su lista, al guardar o con el
 *   botón de volver, y del inventario a los gimnasios.
 * @param onInventoryContinue el inventario está como la persona quiere: se sigue con el paso
 *   siguiente.
 */
fun NavGraphBuilder.gymGraph(
    graphOwner: () -> ViewModelStoreOwner,
    onOpenEditor: () -> Unit,
    onOpenInventory: (GymId) -> Unit,
    onOpenEquipmentEditor: () -> Unit,
    onBack: () -> Unit,
    onInventoryContinue: () -> Unit,
) {
    navigation<GymGraphRoute>(startDestination = GymListRoute) {
        composable<GymListRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            GymListRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onOpenEditor = onOpenEditor,
                onCloseEditor = onBack,
                onOpenInventory = onOpenInventory,
            )
        }
        composable<GymEditorRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            GymEditorRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onOpenEditor = onOpenEditor,
                onCloseEditor = onBack,
            )
        }
        composable<InventoryRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            val gymId = remember(entry) { GymId(entry.toRoute<InventoryRoute>().gymId) }
            InventoryRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                gymId = gymId,
                onOpenEditor = onOpenEquipmentEditor,
                onBack = onBack,
                onContinue = onInventoryContinue,
            )
        }
        composable<EquipmentEditorRoute> { entry ->
            val owner = remember(entry) { graphOwner() }
            EquipmentEditorRoute(
                viewModel = koinViewModel(viewModelStoreOwner = owner),
                onOpenEditor = onOpenEquipmentEditor,
                onCloseEditor = onBack,
            )
        }
    }
}
