package app.reporrun.core.domain.validation

import app.reporrun.core.model.DayOfWeek

/**
 * Letra del día en los códigos de ejercicio (`L01`, `X04`), como la fija el contrato: L, M, X,
 * J, V, S y D de lunes a domingo. El miércoles es `X` para no chocar con el martes.
 */
val DayOfWeek.codeLetter: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY -> "J"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
