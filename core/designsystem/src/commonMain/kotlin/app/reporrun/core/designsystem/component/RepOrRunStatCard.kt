package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme

/**
 * Tarjeta de cifra: un número grande y, debajo, qué cuenta. Van varias en una fila, cada una con
 * `Modifier.weight(1f)`.
 *
 * Con [highlighted] la cifra va en `extendedColors.accentText`, que sobre `surface` cumple el
 * Anexo A; una por fila, para la cifra que más conviene revisar.
 *
 * @param unit la unidad de la cifra («min»), pegada a ella y en pequeño.
 */
@Composable
fun RepOrRunStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    highlighted: Boolean = false,
) {
    // Un lector de pantalla lee la cifra, la unidad y la etiqueta de una vez.
    RepOrRunCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.StatCardUnitSpacing)) {
            Text(
                value,
                modifier = Modifier.alignByBaseline(),
                style = RepOrRunTheme.extendedTypography.numberMedium,
                color = if (highlighted) {
                    RepOrRunTheme.extendedColors.accentText
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (unit != null) {
                Text(
                    unit,
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            label,
            modifier = Modifier.padding(top = RepOrRunDimens.StatCardSpacing),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
