package app.reporrun.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * Medidas del lenguaje visual v2, tomadas de las pantallas de Figma y de la sección «Forma»
 * del Anexo A. Las pantallas y los componentes las leen de aquí en vez de escribir el número.
 */
object RepOrRunDimens {
    /** Margen lateral de todas las pantallas. */
    val ScreenPadding = 20.dp

    /** Separación entre bloques de una pantalla. */
    val SectionSpacing = 16.dp

    val ButtonHeight = 60.dp
    val ButtonIconSize = 20.dp
    val ButtonContentSpacing = 10.dp

    val IconButtonSize = 44.dp
    val IconButtonIconSize = 18.dp
    val IconButtonEmphasisSize = 52.dp
    val IconButtonEmphasisIconSize = 22.dp

    val ChipHeight = 34.dp
    val ChipRadius = 18.dp
    val ChipHorizontalPadding = 14.dp
    val ChipSpacing = 8.dp
    val StatusChipHeight = 28.dp
    val StatusChipHorizontalPadding = 12.dp

    val CardHorizontalPadding = 18.dp
    val CardVerticalPadding = 16.dp
    val EmphasisCardPadding = 20.dp

    val FieldVerticalPadding = 14.dp
    val FieldLabelSpacing = 6.dp

    val ListRowVerticalPadding = 14.dp
    val ListRowSpacing = 12.dp
    val ListRowTextSpacing = 2.dp
    val ListRowLeadingSize = 38.dp
    val ListRowLeadingIconSize = 17.dp
    val ListRowChevronSize = 18.dp

    val DividerThickness = 1.dp

    val SwitchWidth = 48.dp
    val SwitchHeight = 28.dp
    val SwitchThumbSize = 20.dp
    val SwitchThumbPadding = 4.dp

    val StepperNodeSize = 22.dp
    val StepperNodeDotSize = 8.dp
    val StepperNodeBorder = 3.dp
    val StepperTrackThickness = 3.dp
    val StepperNodeInset = 24.dp
    val StepperLabelSpacing = 8.dp

    val BottomNavHeight = 68.dp
    val BottomNavHorizontalPadding = 10.dp
    val BottomNavItemHeight = 46.dp
    val BottomNavItemSpacing = 8.dp
    val BottomNavItemIconSize = 22.dp
    val BottomNavItemPadding = 14.dp
    val BottomNavActiveStartPadding = 16.dp
    val BottomNavActiveEndPadding = 18.dp

    val StateSpacing = 12.dp
    val ProgressSize = 44.dp
    val ProgressStroke = 4.dp
    val OutlineThickness = 1.dp

    // Bienvenida (pantalla 01): es la única con composición propia.

    /** Los 64 del frame de Figma menos la barra de estado, que aquí pone el sistema. */
    val WelcomeTopPadding = 40.dp
    val WelcomeHeroSpacing = 120.dp
    val WelcomeSectionSpacing = 18.dp

    /** Lista de puntos con check: el resumen del aviso. */
    val ChecklistIconSize = 16.dp
    val ChecklistIconSpacing = 10.dp
    val ChecklistRowSpacing = 12.dp

    val WatermarkTextSize = 200.dp
    val WatermarkStrokeWidth = 2.dp

    // Perfil (pantalla 02): la tarjeta que explica el objetivo elegido.

    /** Borde en `primary`: es la tarjeta que acaba de aparecer, y ahí el acento sí marca. */
    val GoalCardBorder = 1.5.dp
    val GoalCardSpacing = 12.dp
    val GoalCardRowSpacing = 9.dp
    val GoalCardDotSize = 8.dp
}

/** Opacidades del diseño. No son colores: se aplican sobre un rol del tema. */
object RepOrRunAlpha {
    /** Los separadores son `outline` al 20 %, como en las pantallas de Figma. */
    const val DIVIDER = 0.2f

    /** Componentes deshabilitados. */
    const val DISABLED = 0.38f

    /** Filas rellenas de la marca de agua: el 6 % de la fila por el 50 % del patrón. */
    const val WATERMARK_FILL = 0.03f

    /** Filas contorneadas de la marca de agua: el 16 % de la fila por el 50 % del patrón. */
    const val WATERMARK_OUTLINE = 0.08f
}
