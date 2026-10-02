package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunDimens

/**
 * Chip de filtro o de selección, de 34 dp. Elegido usa el bloque de énfasis
 * (`inverseSurface`); sin elegir, `surface` con el texto secundario. Va sobre el fondo de la
 * pantalla, no dentro de una tarjeta.
 */
@Composable
fun RepOrRunChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(RepOrRunDimens.ChipRadius),
        color = if (selected) colors.inverseSurface else colors.surface,
        contentColor = if (selected) colors.inverseOnSurface else colors.onSurfaceVariant,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = RepOrRunDimens.ChipHeight)
                .padding(horizontal = RepOrRunDimens.ChipHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/**
 * Chip de estado, no pulsable. [highlighted] lo pinta con el acento y es para el estado en
 * curso: aquí el naranja sí marca. El resto de estados van en neutro, con el color del fondo
 * de la pantalla, así que el chip va dentro de una tarjeta.
 */
@Composable
fun RepOrRunStatusChip(label: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (highlighted) colors.primary else colors.background,
        contentColor = if (highlighted) colors.onPrimary else colors.onSurfaceVariant,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = RepOrRunDimens.StatusChipHeight)
                .padding(horizontal = RepOrRunDimens.StatusChipHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
