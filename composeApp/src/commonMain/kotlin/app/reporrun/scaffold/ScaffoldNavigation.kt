package app.reporrun.scaffold

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import app.reporrun.core.model.GymId
import kotlinx.serialization.Serializable

/*
 * La navegación de un feature, entera en un archivo: sus rutas, cómo se llega a ellas y su
 * grafo. Es lo único del feature que ve `composeApp`.
 */

/** La entrada al grafo. Es la única ruta pública: desde fuera se entra al feature, no a una pantalla. */
@Serializable
data object ScaffoldGraphRoute

// Las rutas de dentro son `internal`: nadie de fuera puede escribir `navigate(ScaffoldDetailRoute(...))`.
@Serializable
internal data object ScaffoldHomeRoute

/**
 * Los argumentos de una ruta son primitivos: el id viaja como texto y se envuelve al llegar.
 * Nunca viaja un objeto del dominio: se pasa el id y el destino lo carga.
 */
@Serializable
internal data class ScaffoldDetailRoute(val gymId: String)

/** Cómo se llega al detalle. Recibe el tipo del dominio para que la ruta no salga del archivo. */
fun NavController.navigateToScaffoldDetail(gymId: GymId) {
    navigate(ScaffoldDetailRoute(gymId.value))
}

/**
 * El grafo del feature. Recibe **lambdas** y no el `NavController`: los features no se conocen
 * entre sí (`CLAUDE.md` §3), así que a dónde lleva cada salida lo decide `composeApp`.
 */
fun NavGraphBuilder.scaffoldGraph(onGymCreated: (GymId) -> Unit, onBack: () -> Unit) {
    navigation<ScaffoldGraphRoute>(startDestination = ScaffoldHomeRoute) {
        composable<ScaffoldHomeRoute> {
            ScaffoldRoute(onGymCreated = onGymCreated)
        }
        composable<ScaffoldDetailRoute> { entry ->
            val route = entry.toRoute<ScaffoldDetailRoute>()
            ScaffoldDetailScreen(gymId = GymId(route.gymId), onBack = onBack)
        }
    }
}
