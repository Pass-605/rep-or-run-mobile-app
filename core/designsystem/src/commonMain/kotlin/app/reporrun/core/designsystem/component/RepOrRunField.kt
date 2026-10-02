package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunTheme

/**
 * Campo con la etiqueta encima, dentro de una tarjeta de radio 24. El valor va en
 * `titleMedium` y la unidad ([unit]: «kg», «cm»), alineada a su línea base, en texto
 * secundario. Con [singleLine] en `false` es un área de texto libre en `bodySmall`.
 *
 * [errorText] va dentro de la tarjeta, en `extendedColors.accentText`: sobre el fondo de la
 * pantalla ese color no llega a 4,5:1 en claro.
 */
@Composable
fun RepOrRunField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    placeholder: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val valueStyle = if (singleLine) typography.titleMedium else typography.bodySmall
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        textStyle = valueStyle.copy(color = colors.onSurface),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.onSurface),
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, MaterialTheme.shapes.medium)
                    .padding(
                        horizontal = RepOrRunDimens.CardHorizontalPadding,
                        vertical = RepOrRunDimens.FieldVerticalPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.FieldLabelSpacing),
            ) {
                Text(label, style = typography.labelSmall, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.FieldLabelSpacing)) {
                    // Con unidad el valor ocupa solo su texto: la unidad va pegada a él, no al borde.
                    val valueWidth = if (unit == null) Modifier else Modifier.width(IntrinsicSize.Min)
                    Box(Modifier.weight(1f, fill = unit == null).then(valueWidth).alignByBaseline()) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = valueStyle, color = colors.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                    if (unit != null) {
                        Text(
                            unit,
                            modifier = Modifier.alignByBaseline(),
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                if (errorText != null) {
                    Text(
                        errorText,
                        style = typography.bodySmall,
                        color = RepOrRunTheme.extendedColors.accentText,
                    )
                }
            }
        },
    )
}
