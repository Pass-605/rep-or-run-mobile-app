package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme

/**
 * Buscador en pastilla: la lupa y el texto en `bodyMedium` sobre `surface`. Va sobre el fondo de
 * la pantalla y filtra según se escribe: no tiene botón de buscar.
 *
 * La lupa es decorativa; lo que el campo es lo dice [placeholder] («Buscar máquina»). La acción
 * que lo acompaña, si la hay, es un [RepOrRunIconButton] con `emphasis` a su lado.
 */
@Composable
fun RepOrRunSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyMedium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = style.copy(color = colors.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        singleLine = true,
        cursorBrush = SolidColor(colors.onSurface),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, CircleShape)
                    .padding(
                        horizontal = RepOrRunDimens.CardHorizontalPadding,
                        vertical = RepOrRunDimens.SearchFieldVerticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.SearchFieldSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = RepOrRunIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(RepOrRunDimens.SearchFieldIconSize),
                    tint = RepOrRunTheme.extendedColors.iconPrimary,
                )
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = style, color = colors.onSurfaceVariant, maxLines = 1)
                    }
                    innerTextField()
                }
            }
        },
    )
}
