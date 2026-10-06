package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons

/**
 * Barra de arriba de una pantalla: botón de volver, título centrado y un hueco del mismo ancho
 * al otro lado para que lo esté. No trae fondo ni margen: va sobre el fondo de la pantalla.
 *
 * @param backContentDescription lo que anuncia el botón de volver.
 * @param onBack `null` cuando la pantalla es el primer destino y no hay a dónde volver; el
 *   hueco se conserva para que el título no se mueva.
 */
@Composable
fun RepOrRunTopBar(
    title: String,
    backContentDescription: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(RepOrRunDimens.IconButtonSize)) {
            if (onBack != null) {
                RepOrRunIconButton(
                    icon = RepOrRunIcons.ArrowBack,
                    contentDescription = backContentDescription,
                    onClick = onBack,
                )
            }
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.size(RepOrRunDimens.IconButtonSize))
    }
}
