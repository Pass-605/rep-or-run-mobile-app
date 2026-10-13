package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

/**
 * El armazón de una pantalla de formulario: la barra de arriba, el contenido con scroll y la
 * acción fija al pie, que sigue al alcance con el teclado abierto o la fuente grande. Es un
 * [RepOrRunScreenFrame] con la barra y el botón ya puestos.
 *
 * No decide navegación ni conoce los textos de nadie: la vuelta y la acción del pie llegan como
 * lambdas, y lo que anuncia el botón de volver, como texto de quien lo usa.
 *
 * @param backContentDescription lo que anuncia el botón de volver.
 * @param actionIcon icono de la acción del pie, si lo lleva.
 * @param onBack `null` cuando la pantalla es el primer destino y no hay a dónde volver.
 * @param content lo que va bajo la barra, separado como en [RepOrRunScrollContent].
 */
@Composable
fun RepOrRunScreenScaffold(
    title: String,
    backContentDescription: String,
    actionText: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: Painter? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    RepOrRunScreenFrame(
        modifier = modifier,
        footer = {
            RepOrRunButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                icon = actionIcon,
                enabled = actionEnabled,
            )
        },
    ) {
        RepOrRunScrollContent {
            RepOrRunTopBar(
                title = title,
                backContentDescription = backContentDescription,
                onBack = onBack,
            )
            content()
        }
    }
}
