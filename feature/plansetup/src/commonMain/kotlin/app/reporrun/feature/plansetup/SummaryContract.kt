// Estado, evento y efecto de una pantalla van juntos en `XxxContract.kt`: son su contrato con
// el ViewModel y se leen de una vez.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.feature.plansetup

import app.reporrun.core.domain.usecase.GenerationSummary
import app.reporrun.core.model.GymId

/**
 * El resumen previo a generar.
 *
 * [summary] es el del caso de uso, sin traducir: qué hay guardado, qué falta y si se puede
 * generar lo dice él. Es `null` hasta que el almacenamiento contesta, y entonces no se pinta
 * nada, para que la pantalla no diga «falta todo» durante un instante.
 */
data class SummaryUiState(val summary: GenerationSummary? = null) {
    val isLoading: Boolean
        get() = summary == null

    val canGenerate: Boolean
        get() = summary?.canGenerate == true
}

sealed interface SummaryEvent {
    data object GenerateClicked : SummaryEvent
}

sealed interface SummaryEffect {
    /** No falta nada: se puede pasar a generar. */
    data object GenerationRequested : SummaryEffect
}

/**
 * A dónde lleva cada fila del resumen. Qué formulario se abre, y a dónde se vuelve al guardarlo,
 * lo decide la raíz.
 *
 * @property onEditGym recibe el gimnasio activo, o `null` si no hay ninguno que editar.
 */
internal class SummaryActions(
    val onBack: () -> Unit,
    val onEditProfile: () -> Unit,
    val onEditGym: (GymId?) -> Unit,
    val onEditPreferences: () -> Unit,
)
