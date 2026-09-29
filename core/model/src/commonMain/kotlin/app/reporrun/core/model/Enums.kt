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

enum class Metric { REPS, METERS, SECONDS }
