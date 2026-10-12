// El archivo se llama como el componente, no como el enum de sus estilos.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.reporrun.core.designsystem.RepOrRunAlpha
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme

enum class RepOrRunButtonStyle {
    /** La acción principal: `inverseSurface`, negra en claro y blanca en oscuro. Nunca naranja. */
    Primary,

    /** Acción secundaria: sin relleno, con borde `outline`. */
    Outline,

    /**
     * Acción que no es la principal y va sola al pie, sobre el fondo de la pantalla: relleno
     * `surface`, como una tarjeta. Es el «Cancelar» de la pantalla 06.
     */
    Tonal,
}

/**
 * Botón de 60 dp y radio 28. El ancho lo decide quien lo usa (`fillMaxWidth()` para la
 * acción de pie de pantalla). [icon] va delante del texto.
 */
@Composable
fun RepOrRunButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RepOrRunButtonStyle = RepOrRunButtonStyle.Primary,
    icon: Painter? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (style) {
        RepOrRunButtonStyle.Primary -> colors.inverseSurface
        RepOrRunButtonStyle.Outline -> Color.Transparent
        RepOrRunButtonStyle.Tonal -> colors.surface
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Button }
            .alpha(if (enabled) 1f else RepOrRunAlpha.DISABLED),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = if (style == RepOrRunButtonStyle.Primary) colors.inverseOnSurface else colors.onSurface,
        border = if (style == RepOrRunButtonStyle.Outline) {
            BorderStroke(RepOrRunDimens.OutlineThickness, colors.outline)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = RepOrRunDimens.ButtonHeight)
                .padding(horizontal = RepOrRunDimens.CardHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(
                RepOrRunDimens.ButtonContentSpacing,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(RepOrRunDimens.ButtonIconSize))
            }
            Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

/**
 * Botón de icono circular. Por defecto mide 44 dp y va en `surface`; con [emphasis] mide
 * 52 dp y usa el bloque de énfasis, para la acción que acompaña a un buscador. El área
 * táctil es de 48 dp como mínimo en los dos casos.
 */
@Composable
fun RepOrRunIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val size = if (emphasis) RepOrRunDimens.IconButtonEmphasisSize else RepOrRunDimens.IconButtonSize
    val iconSize = if (emphasis) RepOrRunDimens.IconButtonEmphasisIconSize else RepOrRunDimens.IconButtonIconSize
    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Button }
            .alpha(if (enabled) 1f else RepOrRunAlpha.DISABLED),
        enabled = enabled,
        shape = CircleShape,
        color = if (emphasis) colors.inverseSurface else colors.surface,
        contentColor = if (emphasis) colors.inverseOnSurface else RepOrRunTheme.extendedColors.iconPrimary,
    ) {
        Row(
            modifier = Modifier.size(size),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
        }
    }
}
