package app.reporrun.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reporrun.core.designsystem.RepOrRunAlpha
import app.reporrun.core.designsystem.RepOrRunDimens

/**
 * Interruptor de 48 × 28 dp. Encendido usa el bloque de énfasis (`inverseSurface`) con el
 * botón en `surface`; apagado, `background` con el botón en `outline`. No usa el acento.
 *
 * Con [onCheckedChange] en `null` no es interactivo: sirve cuando la que se pulsa es la fila
 * entera que lo contiene.
 */
@Composable
fun RepOrRunSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val travel = RepOrRunDimens.SwitchWidth - RepOrRunDimens.SwitchThumbSize - RepOrRunDimens.SwitchThumbPadding * 2
    val thumbOffset by animateDpAsState(if (checked) travel else 0.dp)
    val toggle = if (onCheckedChange != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(toggle)
            .alpha(if (enabled) 1f else RepOrRunAlpha.DISABLED)
            .size(RepOrRunDimens.SwitchWidth, RepOrRunDimens.SwitchHeight)
            .background(if (checked) colors.inverseSurface else colors.background, CircleShape)
            .padding(RepOrRunDimens.SwitchThumbPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(RepOrRunDimens.SwitchThumbSize)
                .background(if (checked) colors.surface else colors.outline, CircleShape),
        )
    }
}
