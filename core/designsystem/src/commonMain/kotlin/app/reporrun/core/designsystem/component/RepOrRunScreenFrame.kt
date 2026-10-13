package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.reporrun.core.designsystem.RepOrRunDimens

/**
 * El marco de una pantalla sin barra propia del armazón: los márgenes seguros, el margen lateral
 * y, si lo hay, un pie fijo que sigue al alcance con el teclado abierto o la fuente grande.
 *
 * Qué va arriba lo decide cada pantalla, y por eso no trae barra: una espera solo tiene cerrar,
 * el inventario deja fijos la barra y el buscador. Para la forma «barra, scroll y una acción al
 * pie» está [RepOrRunScreenScaffold], que se monta sobre este.
 *
 * @param footer lo que se queda fijo abajo; `null` si la pantalla no tiene pie.
 * @param body lo que ocupa el resto. Normalmente un [RepOrRunScrollContent]; una lista perezosa
 *   pone ella su `Modifier.weight(1f)`.
 */
@Composable
fun RepOrRunScreenFrame(
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Incluye el teclado: con él abierto el pie sube y el cuerpo sigue haciendo scroll.
            .safeDrawingPadding()
            .padding(horizontal = RepOrRunDimens.ScreenPadding),
    ) {
        body()
        if (footer != null) {
            Box(Modifier.fillMaxWidth().padding(vertical = RepOrRunDimens.SectionSpacing)) { footer() }
        }
    }
}

/**
 * El cuerpo con scroll de un [RepOrRunScreenFrame]: ocupa lo que deja el pie y separa sus bloques
 * con [spacing].
 */
@Composable
fun ColumnScope.RepOrRunScrollContent(
    modifier: Modifier = Modifier,
    spacing: Dp = RepOrRunDimens.SectionSpacing,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = RepOrRunDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}
