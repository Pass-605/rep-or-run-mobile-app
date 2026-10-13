package app.reporrun.feature.plan

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.reporrun.core.model.PlanId
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
internal data object GeneratingRoute

/**
 * La vista del plan. Con [planId], ese plan; sin él, el activo del gimnasio activo, que es como
 * se abre cuando es el destino inicial. El id viaja como texto: una ruta solo lleva tipos simples.
 */
@Serializable
data class PlanRoute(val planId: String? = null)

/** El nombre con el que [PlanRoute.planId] llega al `SavedStateHandle` del ViewModel. */
internal const val PLAN_ID_ARG = "planId"

/**
 * Abre la espera, **y con ella empieza la generación**: la pide el ViewModel al crearse.
 * `launchSingleTop`: dos toques seguidos en «Generar plan» no piden dos planes.
 */
fun NavController.navigateToGenerating() {
    navigate(GeneratingRoute) { launchSingleTop = true }
}

/**
 * Sigue de la espera al plan generado, **sacándola de la pila**: si se quedara debajo, «atrás»
 * desde el plan la reabriría, y reabrirla es generar otra vez.
 */
fun NavController.navigateToGeneratedPlan(planId: PlanId) {
    navigate(PlanRoute(planId.value)) {
        popUpTo<GeneratingRoute> { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * El grafo del plan: la espera de la generación y la vista del plan.
 *
 * @param canLeave si la vista del plan tiene a dónde volver: no cuando es el destino inicial, y
 *   sí tras generar, con el resumen debajo. Se pregunta al componerla.
 * @param onBack volver al destino anterior.
 * @param onBackToSummary salir de la espera hacia el resumen: por cancelar, por un error que
 *   pide corregir datos o porque faltaba algo.
 * @param onPlanGenerated hay plan guardado, y este es su id.
 * @param onOpenSummary ir al resumen previo a generar, desde la vista del plan.
 */
fun NavGraphBuilder.planGraph(
    canLeave: () -> Boolean,
    onBack: () -> Unit,
    onBackToSummary: () -> Unit,
    onPlanGenerated: (PlanId) -> Unit,
    onOpenSummary: () -> Unit,
) {
    composable<GeneratingRoute> {
        GeneratingRoute(
            viewModel = koinViewModel(),
            onPlanGenerated = onPlanGenerated,
            onBackToSummary = onBackToSummary,
        )
    }
    composable<PlanRoute> { entry ->
        val canLeavePlan = remember(entry) { canLeave() }
        PlanRoute(
            viewModel = koinViewModel(),
            onBack = onBack.takeIf { canLeavePlan },
            onOpenSummary = onOpenSummary,
        )
    }
}
