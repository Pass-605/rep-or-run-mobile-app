package app.gymdi.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta de Gymdi. Es la traducción literal del Anexo A del plan de trabajo, que es
 * la referencia normativa: si esto y el Anexo A se contradicen, manda el Anexo A.
 *
 * Los cinco derivados no son gusto, salen de medir contraste, y el motivo está en cada uno.
 * No los cambies sin volver a medir.
 */
object GymdiPalette {
    /** El único color del sistema. Va en detalles, nunca en superficies grandes. */
    val Accent = Color(0xFFF2522E)

    val Paper = Color(0xFFFFFCF9)
    val Gray400 = Color(0xFF979797)
    val Gray600 = Color(0xFF656565)
    val Gray800 = Color(0xFF3F3F3F)
    val Gray900 = Color(0xFF252525)

    /** Derivado: la paleta no traía un fondo distinto del blanco de tarjeta. */
    val Canvas = Color(0xFFF1EEEB)

    /** Derivado: Accent da 3,41 sobre claro y no vale como texto. Este da 4,61. */
    val AccentInk = Color(0xFFDA330E)

    /** Derivado: Accent da 3,3 sobre las tarjetas oscuras. Este da 4,54. */
    val AccentLight = Color(0xFFF48F78)

    /** Derivado: Gray900 sobre Accent daba 4,40. Este da 4,51. */
    val InkOnAccent = Color(0xFF232323)

    /** Derivado: Gray400 da 3,61 sobre Gray800 y no vale como texto. Este da 5,08. */
    val Gray300 = Color(0xFFB4B4B4)
}
