package app.reporrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunAlpha
import app.reporrun.core.designsystem.RepOrRunDimens

/** Relleno por defecto de [RepOrRunCard]. */
val RepOrRunCardPadding = PaddingValues(
    horizontal = RepOrRunDimens.CardHorizontalPadding,
    vertical = RepOrRunDimens.CardVerticalPadding,
)

/**
 * Tarjeta plana: `surface`, radio 24, sin borde, sombra ni elevación. La separación con el
 * fondo la da el contraste entre `background` y `surface`.
 *
 * Con [onClick] toda la tarjeta es pulsable.
 */
@Composable
fun RepOrRunCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = RepOrRunCardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body = @Composable { Column(Modifier.padding(contentPadding), content = content) }
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            content = body,
        )
    }
}

/**
 * Bloque de énfasis: `inverseSurface` (negro en claro, blanco en oscuro), radio 28. Uno por
 * pantalla, para el héroe. El texto de dentro va en `inverseOnSurface`.
 */
@Composable
fun RepOrRunEmphasisCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(RepOrRunDimens.EmphasisCardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Separador entre las filas de una tarjeta: una línea de `outline` al 20 %. */
@Composable
fun RepOrRunDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(RepOrRunDimens.DividerThickness)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = RepOrRunAlpha.DIVIDER)),
    )
}
