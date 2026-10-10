package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme

/* Las piezas de una sección de formulario, sobre el fondo de la pantalla. */

/** El título de una sección. [trailing] es el apunte corto que va a su derecha, si lo hay. */
@Composable
fun RepOrRunSectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Texto de apoyo sobre el fondo de la pantalla. */
@Composable
fun RepOrRunSupportingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Un grupo de [RepOrRunChip] que saltan de línea cuando no caben. [selected] dice cuáles están
 * elegidos; que se pueda elegir uno o varios lo decide quien lo usa.
 */
@Composable
fun <T> RepOrRunChipGroup(
    options: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
        verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
    ) {
        options.forEach { option ->
            RepOrRunChip(label = label(option), selected = selected(option), onClick = { onClick(option) })
        }
    }
}

/**
 * El error de una sección, o nada si [message] es `null`. Va **dentro de una tarjeta**:
 * `accentText` sobre el fondo de la pantalla se queda en 4,07:1 en claro (Anexo A), y sobre
 * `surface` llega a 4,61:1.
 *
 * Recibe el texto ya resuelto: de qué error se trata lo sabe el feature, no el design system.
 */
@Composable
fun RepOrRunSectionError(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    RepOrRunCard(modifier = modifier.fillMaxWidth()) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = RepOrRunTheme.extendedColors.accentText)
    }
}
