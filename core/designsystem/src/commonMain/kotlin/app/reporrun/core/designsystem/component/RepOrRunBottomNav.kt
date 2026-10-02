// El archivo se llama como el componente, no como el tipo de sus destinos.
@file:Suppress("MatchingDeclarationName")

package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme

/** Un destino de [RepOrRunBottomNav]. [label] se ve solo en el activo y se lee en todos. */
@Immutable
data class RepOrRunNavItem(val label: String, val icon: Painter)

/**
 * Barra inferior en pastilla: `surface`, 68 dp de alto. El destino activo va en una pastilla
 * del bloque de énfasis con su etiqueta; los demás, solo el icono.
 */
@Composable
fun RepOrRunBottomNav(
    items: List<RepOrRunNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(RepOrRunDimens.BottomNavHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = RepOrRunDimens.BottomNavHorizontalPadding)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                NavItem(item, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

@Composable
private fun NavItem(item: RepOrRunNavItem, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val startPadding = if (selected) RepOrRunDimens.BottomNavActiveStartPadding else RepOrRunDimens.BottomNavItemPadding
    val endPadding = if (selected) RepOrRunDimens.BottomNavActiveEndPadding else RepOrRunDimens.BottomNavItemPadding
    // El área táctil ocupa todo el alto de la barra; la pastilla visible mide 46 dp.
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(RepOrRunDimens.BottomNavItemHeight)
                .then(if (selected) Modifier.background(colors.inverseSurface, CircleShape) else Modifier)
                .padding(start = startPadding, end = endPadding),
            horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.BottomNavItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = item.icon,
                // El activo ya dice su nombre con la etiqueta visible.
                contentDescription = if (selected) null else item.label,
                modifier = Modifier.size(RepOrRunDimens.BottomNavItemIconSize),
                tint = if (selected) colors.inverseOnSurface else RepOrRunTheme.extendedColors.iconPrimary,
            )
            if (selected) {
                Text(item.label, style = MaterialTheme.typography.labelMedium, color = colors.inverseOnSurface)
            }
        }
    }
}
