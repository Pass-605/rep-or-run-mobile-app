package app.reporrun.feature.plan

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.reporrun.core.model.PlanId
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
internal data object GeneratingRoute

/** El plan recién generado. Lleva el id como texto: es lo que sabe serializar una ruta. */
@Serializable
internal data class PlanDraftRoute(val planId: String)

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
    navigate(PlanDraftRoute(planId.value)) {
        popUpTo<GeneratingRoute> { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * El grafo del plan: la espera de la generación y a dónde llega el plan generado.
 *
 * @param onBack volver al destino anterior.
 * @param onBackToSummary salir de la espera hacia el resumen: por cancelar, por un error que
 *   pide corregir datos o porque faltaba algo.
 * @param onPlanGenerated hay plan guardado, y este es su id.
 */
fun NavGraphBuilder.planGraph(onBack: () -> Unit, onBackToSummary: () -> Unit, onPlanGenerated: (PlanId) -> Unit) {
    composable<GeneratingRoute> {
        GeneratingRoute(
            viewModel = koinViewModel(),
            onPlanGenerated = onPlanGenerated,
            onBackToSummary = onBackToSummary,
        )
    }
    composable<PlanDraftRoute> { entry ->
        PlanDraftScreen(planId = PlanId(entry.toRoute<PlanDraftRoute>().planId), onBack = onBack)
    }
}
