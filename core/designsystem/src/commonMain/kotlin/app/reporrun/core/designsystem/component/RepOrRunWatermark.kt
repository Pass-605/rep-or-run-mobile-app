package app.reporrun.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.reporrun.core.designsystem.RepOrRunAlpha
import app.reporrun.core.designsystem.RepOrRunDimens

// La marca, no un texto de la interfaz: no se traduce y por eso no va en strings.xml.
private const val WATERMARK_TEXT = "REP OR RUN REP OR RUN"

// Dónde empieza cada fila, como en el componente `Pattern/Watermark` de Figma ya colocado en
// la pantalla 01. Cada fila arranca más a la izquierda que la anterior para que no se alineen.
private val WatermarkRows = listOf(
    DpOffset(x = (-120).dp, y = (-60).dp),
    DpOffset(x = (-380).dp, y = 150.dp),
    DpOffset(x = (-640).dp, y = 360.dp),
    DpOffset(x = (-900).dp, y = 570.dp),
    DpOffset(x = (-260).dp, y = 780.dp),
    DpOffset(x = (-520).dp, y = 990.dp),
)

private val WatermarkTracking = (-4).dp

/**
 * Marca de agua tipográfica «REP OR RUN»: filas alternas, rellenas y contorneadas, casi
 * transparentes. Va **sobre la superficie oscura de marca** (bienvenida, portada del PDF),
 * detrás del contenido, y se recorta con su propio tamaño: pásale `matchParentSize()`.
 *
 * Toma el color de `onBackground`, así que hay que pintarla dentro del tema oscuro. Es
 * decoración: no tiene semántica y su tamaño no crece con la fuente del sistema.
 *
 * En Figma las filas rellenas usan Outfit Black (900). Aquí van en 700, el peso más alto que
 * se empaqueta.
 */
@Composable
fun RepOrRunWatermark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    val density = LocalDensity.current
    val fontSize = with(density) { RepOrRunDimens.WatermarkTextSize.toSp() }
    val filled = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize,
        letterSpacing = with(density) { WatermarkTracking.toSp() },
    )
    val outlined = filled.copy(
        drawStyle = Stroke(width = with(density) { RepOrRunDimens.WatermarkStrokeWidth.toPx() }),
    )

    Box(modifier.clipToBounds().clearAndSetSemantics { }) {
        WatermarkRows.forEachIndexed { index, start ->
            val isOutlined = index % 2 == 1
            Text(
                text = WATERMARK_TEXT,
                modifier = Modifier
                    .offset(x = start.x, y = start.y)
                    // La fila es mucho más ancha que la pantalla: se mide sin límite y la
                    // recorta el contenedor.
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true),
                color = color.copy(
                    alpha = if (isOutlined) RepOrRunAlpha.WATERMARK_OUTLINE else RepOrRunAlpha.WATERMARK_FILL,
                ),
                style = if (isOutlined) outlined else filled,
                softWrap = false,
                maxLines = 1,
            )
        }
    }
}
