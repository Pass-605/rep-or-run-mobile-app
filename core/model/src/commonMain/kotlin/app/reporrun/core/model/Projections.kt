package app.reporrun.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/*
 * Proyecciones de lectura: juntan o recortan entidades del plan §10 para una pantalla, sin
 * añadir reglas nuevas.
 */

/**
 * Una versión de plan sin sus días, para el historial. Para el plan entero, [Plan].
 *
 * @property focus enfoque del bloque, como en [Plan.focus].
 */
data class PlanSummary(
    val id: PlanId,
    val gymId: GymId,
    val createdAt: Instant,
    val status: PlanStatus,
    val startDate: LocalDate?,
    val reviewDate: LocalDate?,
    val focus: List<String>,
) {
    /** Número de versión, el `N` de `ROR-AAAA-MM-DD-vN`. */
    val version: Int
        get() = id.version
}

/** Un check-in con lo que la persona contó de cada ejercicio. */
data class CheckInRecord(val checkIn: CheckIn, val feedback: List<ExerciseFeedback>) {
    init {
        require(feedback.all { it.checkInId == checkIn.id }) { "feedback de otro check-in" }
    }
}
