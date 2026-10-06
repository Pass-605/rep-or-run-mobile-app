package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons

/**
 * Marca de selección al final de una fila: un círculo de 28 dp. Elegida usa el bloque de énfasis
 * (`inverseSurface`) con el check en su par; sin elegir, el color del fondo de la pantalla con
 * un borde en `onSurfaceVariant`.
 *
 * El borde no está en Figma: allí el círculo sin elegir es `bg/canvas` sobre `bg/surface`, que
 * mide 1,13:1 en claro y 1,46:1 en oscuro, y una marca de estado pide 3:1. `outline` se queda
 * en 2,86:1 en claro; `onSurfaceVariant` da 5,70:1 y 5,08:1.
 *
 * Es decorativa: no se pulsa ni se anuncia. Quien la usa hace pulsable la fila entera y dice
 * el estado en su texto.
 */
@Composable
fun RepOrRunCheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val circle = if (checked) {
        Modifier.background(colors.inverseSurface, CircleShape)
    } else {
        Modifier
            .background(colors.background, CircleShape)
            .border(RepOrRunDimens.CheckMarkBorder, colors.onSurfaceVariant, CircleShape)
    }
    Box(
        modifier = modifier.size(RepOrRunDimens.CheckMarkSize).then(circle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = RepOrRunIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(RepOrRunDimens.CheckMarkIconSize),
                tint = colors.inverseOnSurface,
            )
        }
    }
}
