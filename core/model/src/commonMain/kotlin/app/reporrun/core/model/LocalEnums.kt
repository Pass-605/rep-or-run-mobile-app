package app.reporrun.core.model

/*
 * Enumeraciones que solo existen en la app (plan §10): estados guardados en local que no
 * viajan por el contrato. Cambiarlas no toca la Lambda, pero sí las migraciones de Room.
 */

/** Ciclo de vida de una versión de plan. Solo un plan por gimnasio está [ACTIVE]. */
enum class PlanStatus { DRAFT, ACTIVE, ARCHIVED }

/** Estado de un día del plan: [PROPOSED] hasta que la persona lo confirma (plan §4.11). */
enum class DayStatus { PROPOSED, APPROVED }

/**
 * Cada cuánto se revisa el plan. [days] es el intervalo fijo; `null` en [CUSTOM], donde lo
 * da [ReviewSchedule.customDays].
 */
enum class ReviewFrequency(val days: Int?) {
    EVERY_7(ONE_WEEK),
    EVERY_14(TWO_WEEKS),
    EVERY_21(THREE_WEEKS),
    EVERY_28(FOUR_WEEKS),
    CUSTOM(null),
}

private const val ONE_WEEK = 7
private const val TWO_WEEKS = 14
private const val THREE_WEEKS = 21
private const val FOUR_WEEKS = 28

/** Medidas que la persona elige registrar en cada check-in. */
enum class ReviewMetric { WEIGHT, WAIST, HIP, SLEEP, STEPS, ENERGY, PAIN }

/** Estado de una revisión. [CLOSED] y [SKIPPED] son finales. */
enum class ReviewStatus {
    PENDING,
    IN_PROGRESS,
    PROPOSED,
    CLOSED,
    SKIPPED,
    ;

    /** Si la revisión ya terminó y tiene fecha de cierre. */
    val isFinal: Boolean
        get() = this == CLOSED || this == SKIPPED
}

/** Decisión de la persona sobre una propuesta. Nada se aplica sin [APPROVED] (plan §4.12). */
enum class Decision { PENDING, APPROVED, REJECTED }

/**
 * El tema que la persona elige en Ajustes. [SYSTEM] sigue al del dispositivo y es el valor
 * cuando no ha elegido nada. Se guarda en DataStore, no en Room.
 */
enum class ThemeMode { SYSTEM, DARK, LIGHT }
