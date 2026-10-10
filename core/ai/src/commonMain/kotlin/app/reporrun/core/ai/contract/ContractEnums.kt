package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable

/*
 * Los enums de `common.schema.json`, con los mismos valores. Son los del cable, no los de
 * `:core:model`: renombrar uno del dominio no puede cambiar lo que viaja a la Lambda.
 */

/** `DayOfWeek`. */
@Serializable
enum class DayOfWeekDto { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

/** `Experience`. */
@Serializable
enum class ExperienceDto { BEGINNER, INTERMEDIATE, ADVANCED }

/** `Goal`. */
@Serializable
enum class GoalDto { RECOMPOSITION, HYPERTROPHY, STRENGTH, GENERAL_FITNESS }

/** `MuscleGroup`. */
@Serializable
enum class MuscleGroupDto {
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

/** `EquipmentCategory`. */
@Serializable
enum class EquipmentCategoryDto { MACHINE, CABLE, FREE_WEIGHT, SMITH, BENCH, CARDIO, BODYWEIGHT, OTHER }

/** `LoadType`: la convención de carga del aparato (plan §4.5). */
@Serializable
enum class LoadTypeDto { SELECTOR, PLATES_TOTAL, SMITH, SLED, DUMBBELL, BARBELL, BODYWEIGHT, CARDIO, NONE }

/** `Metric`. */
@Serializable
enum class MetricDto { REPS, METERS, SECONDS }

/** `Unit`. */
@Serializable
enum class UnitDto { KG, }

/** `DataQuality`. */
@Serializable
enum class DataQualityDto { SUFFICIENT, LIMITED, INSUFFICIENT }

/** `ProposalType`. */
@Serializable
enum class ProposalTypeDto {
    KEEP,
    INCREASE_LOAD,
    ADJUST_REPS,
    ADJUST_SETS,
    SWAP_EXERCISE,
    MOVE_DAY,
    ADJUST_CARDIO,
    OTHER,
}

/** `Confidence`. */
@Serializable
enum class ConfidenceDto { LOW, MEDIUM, HIGH }

/** `FeedbackRating`. */
@Serializable
enum class FeedbackRatingDto { TOO_EASY, OK, TOO_HARD, NOT_DONE }

/** `code` de `error.schema.json`. */
@Serializable
enum class ApiErrorCodeDto { INVALID_INPUT, PLAN_INVALID, RATE_LIMITED, UPSTREAM_ERROR, TIMEOUT }
