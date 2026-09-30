package app.reporrun.core.model

/**
 * Enumeraciones del dominio. Los nombres coinciden exactamente con los del contrato
 * (`docs/contract/v1/common.schema.json`): si cambian aquí, cambian allí y en la Lambda.
 */

enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }

enum class Goal { RECOMPOSITION, HYPERTROPHY, STRENGTH, GENERAL_FITNESS }

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    ABDUCTORS,
    ADDUCTORS,
    CALVES,
    CORE,
    FULL_BODY,
}

enum class EquipmentCategory { MACHINE, CABLE, FREE_WEIGHT, SMITH, BENCH, CARDIO, BODYWEIGHT, OTHER }

/**
 * Cómo se lee y se compara la carga de un aparato (plan §4.5). No es un detalle de UI:
 * decide qué significa "30 kg" en el plan, en el check-in y en el PDF.
 */
enum class LoadType {
    /** Número de la pila. */
    SELECTOR,

    /** Discos totales. */
    PLATES_TOTAL,

    /** Discos, y la barra aparte si se conoce su peso. */
    SMITH,

    /** Carro de prensa, hack o péndulo: el vacío ya pesa. */
    SLED,

    /** Por mancuerna. */
    DUMBBELL,

    /** Barra más discos. */
    BARBELL,

    /** Peso corporal; 0 kg es sin lastre. */
    BODYWEIGHT,

    /** Sin carga: minutos, RPE y, si aplica, velocidad e inclinación. */
    CARDIO,

    /** Sin carga externa. */
    NONE,
}

/** Unidad de la métrica de un ejercicio: repeticiones, metros o segundos (plan §4.8). */
enum class Metric { REPS, METERS, SECONDS }

/**
 * Unidad de carga del bloque. En el contrato se llama `Unit`; aquí no, para no tapar a
 * [kotlin.Unit]. v1 solo admite kilos.
 */
enum class LoadUnit { KG, }

/**
 * Cuántos datos hay para revisar (plan §4.13). Con [INSUFFICIENT] la revisión lo dice y no
 * propone cambios: ver [ReviewResult].
 */
enum class DataQuality { SUFFICIENT, LIMITED, INSUFFICIENT }

/** Tipo de cambio que propone una revisión. [KEEP] es proponer no tocar nada. */
enum class ProposalType {
    KEEP,
    INCREASE_LOAD,
    ADJUST_REPS,
    ADJUST_SETS,
    SWAP_EXERCISE,
    MOVE_DAY,
    ADJUST_CARDIO,
    OTHER,
    ;

    /** Si la propuesta tiene que apuntar a un ejercicio concreto del plan. */
    val targetsExercise: Boolean
        get() = this in setOf(INCREASE_LOAD, ADJUST_REPS, ADJUST_SETS, SWAP_EXERCISE)

    /** Si la propuesta puede apuntar a un día (contrato: solo en estas dos). */
    val targetsDay: Boolean
        get() = this == MOVE_DAY || this == ADJUST_CARDIO
}

enum class Confidence { LOW, MEDIUM, HIGH }

/**
 * Valoración de un ejercicio en el check-in. El backlog la llama `ExerciseRating`; se
 * mantiene el nombre del contrato (`FeedbackRating`), que es la fuente de verdad.
 */
enum class FeedbackRating { TOO_EASY, OK, TOO_HARD, NOT_DONE }
