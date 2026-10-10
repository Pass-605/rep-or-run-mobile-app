package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import app.reporrun.core.designsystem.RepOrRunDimens

/**
 * El armazón de una pantalla de formulario: la barra de arriba, el contenido con scroll y la
 * acción fija al pie, que sigue al alcance con el teclado abierto o la fuente grande.
 *
 * No decide navegación ni conoce los textos de nadie: la vuelta y la acción del pie llegan como
 * lambdas, y lo que anuncia el botón de volver, como texto de quien lo usa.
 *
 * @param backContentDescription lo que anuncia el botón de volver.
 * @param actionIcon icono de la acción del pie, si lo lleva.
 * @param onBack `null` cuando la pantalla es el primer destino y no hay a dónde volver.
 * @param content lo que va bajo la barra, separado con [RepOrRunDimens.SectionSpacing].
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
    Column(
        modifier = modifier
            .fillMaxSize()
            // Incluye el teclado: con él abierto la acción sube y el contenido sigue haciendo scroll.
            .safeDrawingPadding()
            .padding(horizontal = RepOrRunDimens.ScreenPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = RepOrRunDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.SectionSpacing),
        ) {
            RepOrRunTopBar(
                title = title,
                backContentDescription = backContentDescription,
                onBack = onBack,
            )
            content()
        }
        RepOrRunButton(
            text = actionText,
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RepOrRunDimens.SectionSpacing),
            icon = actionIcon,
            enabled = actionEnabled,
        )
    }
}
