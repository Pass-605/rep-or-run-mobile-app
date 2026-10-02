package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme

/**
 * Fila de lista: icono opcional en un círculo, título, subtítulo y un elemento al final, que
 * por defecto es el chevron. No trae fondo ni margen lateral: va dentro de una
 * [RepOrRunCard], separada de la siguiente con [RepOrRunDivider].
 *
 * [titleColor] cambia solo para acciones destructivas, con `extendedColors.accentText`.
 * Para una fila con interruptor, pasa un [RepOrRunSwitch] en [trailing].
 */
@Composable
fun RepOrRunListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: Painter? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = { RepOrRunListRowChevron() },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = RepOrRunDimens.ListRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(RepOrRunDimens.ListRowLeadingSize)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(RepOrRunDimens.ListRowLeadingIconSize),
                    tint = RepOrRunTheme.extendedColors.iconPrimary,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowTextSpacing),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

/** El chevron de [RepOrRunListRow]. Es decorativo: la fila ya dice adónde lleva. */
@Composable
fun RepOrRunListRowChevron(modifier: Modifier = Modifier) {
    Icon(
        painter = RepOrRunIcons.ChevronForward,
        contentDescription = null,
        modifier = modifier.size(RepOrRunDimens.ListRowChevronSize),
        tint = MaterialTheme.colorScheme.outline,
    )
}
