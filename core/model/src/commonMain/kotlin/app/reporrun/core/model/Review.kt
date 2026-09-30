package app.reporrun.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/**
 * Cada cuánto se revisa un plan y cuándo toca la próxima vez.
 *
 * @property customDays intervalo en días; obligatorio en [ReviewFrequency.CUSTOM] y `null`
 *   en el resto, donde lo da [ReviewFrequency.days].
 * @property weekday día preferido para la revisión, si la persona lo fija.
 * @property time hora del recordatorio.
 * @property metrics medidas que se piden en cada check-in.
 * @property nextDueAt se recalcula en local al completar o saltar una revisión.
 */
data class ReviewSchedule(
    val id: ReviewScheduleId,
    val planId: PlanId,
    val frequency: ReviewFrequency,
    val customDays: Int?,
    val anchorDate: LocalDate,
    val weekday: DayOfWeek?,
    val time: LocalTime,
    val metrics: Set<ReviewMetric>,
    val remindersEnabled: Boolean,
    val nextDueAt: Instant,
) {
    init {
        if (frequency == ReviewFrequency.CUSTOM) {
            require(customDays != null && customDays in CUSTOM_DAYS_RANGE) {
                "CUSTOM necesita customDays en $CUSTOM_DAYS_RANGE"
            }
        } else {
            require(customDays == null) { "customDays solo aplica a CUSTOM" }
        }
    }

    /** Intervalo efectivo en días. */
    val intervalDays: Int
        get() = frequency.days ?: checkNotNull(customDays)

    companion object {
        /** Límite de revisión personalizada; 120 es el máximo de `daysElapsed` del contrato. */
        val CUSTOM_DAYS_RANGE: IntRange = 1..120
    }
}

/**
 * Una revisión del plan. Produce propuestas, nunca cambios (plan §4.12).
 *
 * @property completedAt presente solo si el estado es final ([ReviewStatus.isFinal]).
 * @property dataQuality `null` hasta que se evalúan los datos del check-in; obligatoria
 *   desde que hay propuestas ([ReviewStatus.PROPOSED]).
 */
data class Review(
    val id: ReviewId,
    val planId: PlanId,
    val scheduleId: ReviewScheduleId,
    val dueAt: Instant,
    val completedAt: Instant?,
    val status: ReviewStatus,
    val dataQuality: DataQuality?,
) {
    init {
        require((completedAt != null) == status.isFinal) { "completedAt solo en revisiones cerradas o saltadas" }
        require(status != ReviewStatus.PROPOSED || dataQuality != null) {
            "una revisión con propuestas necesita dataQuality"
        }
    }
}

/**
 * Datos que la persona registra para una revisión. Los límites son los del contrato
 * (`CheckIn` en `review-request.schema.json`).
 *
 * @property painNotes molestias en palabras de la persona; no se interpretan clínicamente
 *   (plan §4.9).
 * @property painDate fecha de la molestia: obligatoria si hay [painNotes], porque una
 *   molestia nueva se registra con fecha antes de proponer nada (plan §4.14).
 * @property energy y [fatigue] de 1 a 5.
 */
data class CheckIn(
    val id: CheckInId,
    val reviewId: ReviewId,
    val weightKg: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val sessionsPlanned: Int,
    val sessionsDone: Int,
    val avgSleepH: Double?,
    val avgSteps: Int?,
    val energy: Int?,
    val fatigue: Int?,
    val painNotes: String,
    val painDate: LocalDate?,
    val notes: String,
) {
    init {
        require(weightKg == null || weightKg in UserProfile.WEIGHT_KG_RANGE) { "weightKg fuera de rango" }
        require(waistCm == null || waistCm in UserProfile.GIRTH_CM_RANGE) { "waistCm fuera de rango" }
        require(hipCm == null || hipCm in UserProfile.GIRTH_CM_RANGE) { "hipCm fuera de rango" }
        require(sessionsPlanned in SESSIONS_RANGE) { "sessionsPlanned fuera de $SESSIONS_RANGE" }
        require(sessionsDone in SESSIONS_RANGE) { "sessionsDone fuera de $SESSIONS_RANGE" }
        require(avgSleepH == null || avgSleepH in SLEEP_H_RANGE) { "avgSleepH fuera de $SLEEP_H_RANGE" }
        require(avgSteps == null || avgSteps in STEPS_RANGE) { "avgSteps fuera de $STEPS_RANGE" }
        require(energy == null || energy in SCALE_RANGE) { "energy fuera de $SCALE_RANGE" }
        require(fatigue == null || fatigue in SCALE_RANGE) { "fatigue fuera de $SCALE_RANGE" }
        require(painNotes.length <= MAX_PAIN_NOTES_LENGTH) { "painNotes demasiado largo" }
        require(painNotes.isBlank() || painDate != null) { "una molestia se registra con fecha" }
        require(notes.length <= MAX_NOTES_LENGTH) { "notes demasiado largo" }
    }

    /** Límites del contrato. */
    companion object {
        val SESSIONS_RANGE: IntRange = 0..60
        val SLEEP_H_RANGE: ClosedFloatingPointRange<Double> = 0.0..16.0
        val STEPS_RANGE: IntRange = 0..60_000
        val SCALE_RANGE: IntRange = 1..5
        const val MAX_PAIN_NOTES_LENGTH: Int = 400
        const val MAX_NOTES_LENGTH: Int = 600
    }
}

/**
 * Lo que la persona cuenta de un ejercicio en el check-in. Es un registro de lo que hizo,
 * no una prescripción.
 *
 * @property usualLoad carga habitual en la unidad del `LoadType` del aparato.
 * @property loadUnitNote cómo se leyó la carga ("número del selector", "por mancuerna").
 * @property repsReached una entrada por serie efectiva, en orden.
 * @property equipmentChanged si usó otro aparato: entonces las cargas no son comparables
 *   (plan §4.13).
 * @property sessionsWithData sesiones registradas de este ejercicio en el bloque; con menos
 *   de [MIN_SESSIONS_TO_COMPARE] no se propone subir carga (plan §4.13).
 */
data class ExerciseFeedback(
    val id: ExerciseFeedbackId,
    val checkInId: CheckInId,
    val planExerciseId: PlanExerciseId,
    val usualLoad: Double?,
    val loadUnitNote: String,
    val repsReached: List<Int>?,
    val rirPerceived: Int?,
    val rating: FeedbackRating,
    val techniqueOk: Boolean?,
    val equipmentChanged: Boolean,
    val sessionsWithData: Int,
    val notes: String,
) {
    init {
        require(usualLoad == null || usualLoad in USUAL_LOAD_RANGE) { "usualLoad fuera de $USUAL_LOAD_RANGE" }
        require(loadUnitNote.length <= MAX_LOAD_UNIT_NOTE_LENGTH) { "loadUnitNote demasiado largo" }
        require(repsReached == null || repsReached.size <= MAX_SETS_REPORTED) { "más de $MAX_SETS_REPORTED series" }
        require(repsReached == null || repsReached.all { it in REPS_RANGE }) { "repeticiones fuera de $REPS_RANGE" }
        require(rirPerceived == null || rirPerceived in RirRange.BOUNDS) { "rirPerceived fuera de ${RirRange.BOUNDS}" }
        require(sessionsWithData in SESSIONS_WITH_DATA_RANGE) { "sessionsWithData fuera de $SESSIONS_WITH_DATA_RANGE" }
        require(notes.length <= MAX_NOTES_LENGTH) { "notes demasiado largo" }
    }

    /** Si hay datos suficientes para comparar la carga de este ejercicio (plan §4.13). */
    val isComparable: Boolean
        get() = !equipmentChanged && sessionsWithData >= MIN_SESSIONS_TO_COMPARE

    /** Límites del contrato (`ExerciseFeedback` en `review-request.schema.json`). */
    companion object {
        const val MIN_SESSIONS_TO_COMPARE: Int = 2
        val USUAL_LOAD_RANGE: ClosedFloatingPointRange<Double> = 0.0..1000.0
        const val MAX_LOAD_UNIT_NOTE_LENGTH: Int = 120
        const val MAX_SETS_REPORTED: Int = 10
        val REPS_RANGE: IntRange = 0..100
        val SESSIONS_WITH_DATA_RANGE: IntRange = 0..20
        const val MAX_NOTES_LENGTH: Int = 300
    }
}

/**
 * El contenido de una propuesta, tal como sale de una revisión (`Proposal` en
 * `review-result.schema.json`).
 *
 * @property targetExerciseCode obligatorio si el tipo apunta a un ejercicio
 *   ([ProposalType.targetsExercise]).
 * @property targetDayOfWeek solo en [ProposalType.MOVE_DAY] y [ProposalType.ADJUST_CARDIO].
 * @property replacementEquipmentId solo en [ProposalType.SWAP_EXERCISE]; del inventario.
 * @property description qué cambiar, concreto y accionable.
 * @property rationale por qué, con la evidencia del check-in.
 */
data class ProposedChange(
    val type: ProposalType,
    val targetExerciseCode: ExerciseCode?,
    val targetDayOfWeek: DayOfWeek?,
    val replacementEquipmentId: EquipmentId?,
    val description: String,
    val rationale: String,
    val confidence: Confidence,
) {
    init {
        require(!type.targetsExercise || targetExerciseCode != null) { "$type necesita targetExerciseCode" }
        require(targetDayOfWeek == null || type.targetsDay) { "targetDayOfWeek no aplica a $type" }
        require(type != ProposalType.MOVE_DAY || targetDayOfWeek != null) { "MOVE_DAY necesita targetDayOfWeek" }
        require(replacementEquipmentId == null || type == ProposalType.SWAP_EXERCISE) {
            "replacementEquipmentId solo en SWAP_EXERCISE"
        }
        require(description.isNotBlank()) { "description vacía" }
        require(rationale.isNotBlank()) { "rationale vacío" }
    }
}

/**
 * Propuesta guardada, pendiente de decisión. Aprobarla no cambia el plan: la aplicación de
 * las aprobadas crea una versión nueva (plan §4.12).
 *
 * @property decidedAt presente si y solo si ya hay decisión.
 */
data class ChangeProposal(
    val id: ChangeProposalId,
    val reviewId: ReviewId,
    val change: ProposedChange,
    val decision: Decision = Decision.PENDING,
    val decidedAt: Instant? = null,
) {
    init {
        require((decision == Decision.PENDING) == (decidedAt == null)) { "decidedAt solo con una decisión tomada" }
    }
}

/**
 * Resultado de una revisión (`review-result.schema.json`): lo que se muestra a la persona
 * antes de que decida.
 *
 * Con [DataQuality.INSUFFICIENT] no hay propuestas: la revisión lo dice en [summary] y pide
 * los datos que faltan en [questions] (plan §4.13).
 */
data class ReviewResult(
    val dataQuality: DataQuality,
    val summary: String,
    val observations: List<String>,
    val proposals: List<ProposedChange>,
    val questions: List<String>,
) {
    init {
        require(summary.isNotBlank()) { "summary vacío" }
        require(dataQuality != DataQuality.INSUFFICIENT || proposals.isEmpty()) {
            "con datos insuficientes no se proponen cambios"
        }
    }
}
