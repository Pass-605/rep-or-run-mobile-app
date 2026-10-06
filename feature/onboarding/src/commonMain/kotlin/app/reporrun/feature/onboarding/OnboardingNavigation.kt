package app.reporrun.feature.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

/*
 * La navegación de un feature, entera en un archivo: sus rutas, cómo se llega a ellas y su
 * grafo. Es lo único del feature que ve `composeApp`.
 */

/** La entrada al grafo. Es la única ruta pública: desde fuera se entra al feature, no a una pantalla. */
@Serializable
data object OnboardingGraphRoute

// Las rutas de dentro son `internal`: nadie de fuera puede escribir `navigate(OnboardingNoticeRoute)`.
@Serializable
internal data object OnboardingWelcomeRoute

@Serializable
internal data object OnboardingNoticeRoute

/**
 * Abre el aviso completo. Es pública porque el aviso tiene que poder leerse después de haber
 * aceptado, desde fuera del feature (Ajustes).
 */
fun NavController.navigateToNotice() {
    navigate(OnboardingNoticeRoute)
}

/**
 * El grafo del feature. Recibe **lambdas** y no el `NavController`: los features no se conocen
 * entre sí (`CLAUDE.md` §3), así que a dónde lleva cada salida lo decide `composeApp`.
 *
 * @param onConsentAccepted la persona aceptó y el consentimiento ya está guardado.
 * @param onReadNotice la persona pide el aviso completo desde la bienvenida.
 * @param onBack salir del aviso completo.
 */
fun NavGraphBuilder.onboardingGraph(onConsentAccepted: () -> Unit, onReadNotice: () -> Unit, onBack: () -> Unit) {
    navigation<OnboardingGraphRoute>(startDestination = OnboardingWelcomeRoute) {
        composable<OnboardingWelcomeRoute> {
            WelcomeRoute(onConsentAccepted = onConsentAccepted, onReadNotice = onReadNotice)
        }
        composable<OnboardingNoticeRoute> {
            NoticeScreen(onBack = onBack)
        }
    }
}
