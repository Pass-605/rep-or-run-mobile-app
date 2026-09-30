package app.reporrun.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Una versión de plan (plan §10). Inmutable: aprobar cambios crea otro [Plan] con un
 * [PlanId] nuevo y archiva este; nunca se edita en el sitio (plan §4.12).
 *
 * El modelo garantiza lo que se puede comprobar mirando cada pieza por separado (rangos,
 * RIR, series, cardio). Lo que exige cruzar datos —aparatos del inventario, días
 * disponibles, códigos únicos, volumen frente a los minutos— es cosa de `PlanValidator`.
 *
 * @property focus enfoque del bloque en palabras de la persona.
 * @property notes avisos del bloque (cargas de ensayo, alternativas, etc.).
 * @property inputSnapshotJson exactamente lo que se envió a generar, para reproducirlo.
 * @property model modelo que lo generó, tal como lo devolvió el proxy.
 */
data class Plan(
    val id: PlanId,
    val gymId: GymId,
    val createdAt: Instant,
    val status: PlanStatus,
    val blockDays: Int,
    val startDate: LocalDate?,
    val reviewDate: LocalDate?,
    val focus: List<String>,
    val notes: List<String>,
    val days: List<PlanDay>,
    val inputSnapshotJson: String,
    val model: String,
    val promptVersion: String,
    val schemaVersion: String,
) {
    init {
        require(blockDays == BlockPreferences.BLOCK_DAYS_V1) {
            "v1 solo admite bloques de ${BlockPreferences.BLOCK_DAYS_V1} días"
        }
        require(reviewDate == null || startDate == null || reviewDate >= startDate) { "reviewDate antes de startDate" }
    }

    /** Número de versión, el `N` de `ROR-AAAA-MM-DD-vN`. */
    val version: Int
        get() = id.version

    /** Series efectivas del plan en una semana. Ver [PlanExercise.effectiveSets]. */
    val weeklyEffectiveSets: Int
        get() = days.sumOf { it.effectiveSets }
}

/**
 * Un día con sesión dentro del plan.
 *
 * [warmup], [mobility] y [cooldown] son texto: el calentamiento y las aproximaciones no son
 * ejercicios y no cuentan como series efectivas (plan §4.7).
 *
 * @property dayIndex posición del día en la semana del bloque, desde 0.
 * @property status [DayStatus.PROPOSED] hasta que la persona lo confirma (plan §4.11).
 * @property cardio `null` si ese día no lleva cardio.
 */
data class PlanDay(
    val id: PlanDayId,
    val dayOfWeek: DayOfWeek,
    val dayIndex: Int,
    val session: String,
    val priority: String,
    val cardio: CardioPrescription?,
    val warmup: String,
    val mobility: String,
    val cooldown: String,
    val exercises: List<PlanExercise>,
    val status: DayStatus = DayStatus.PROPOSED,
) {
    init {
        require(dayIndex >= 0) { "dayIndex negativo" }
    }

    /** Series efectivas del día: suma de [PlanExercise.effectiveSets]. */
    val effectiveSets: Int
        get() = exercises.sumOf { it.effectiveSets }
}

/**
 * Cardio de un día. Los textos van tal cual los escribe el plan ("5-6", "4,8-5,5 km/h").
 *
 * @property speed e [incline] solo si el modo lo admite.
 */
data class CardioPrescription(
    val minMinutes: Int,
    val maxMinutes: Int,
    val mode: String?,
    val rpe: String?,
    val speed: String?,
    val incline: String?,
) {
    init {
        require(minMinutes >= 1) { "minMinutes debe ser >= 1" }
        require(minMinutes <= maxMinutes) { "minMinutes > maxMinutes" }
    }
}

/**
 * Un ejercicio del plan.
 *
 * - [sets] son **series efectivas**; en un ejercicio [unilateral] se cuentan **por lado** y
 *   no se duplican al sumar volumen (plan §4.3).
 * - [alternatives] son "elige una": no tienen series propias y no suman (plan §4.2).
 * - [trial] es la carga de ensayo, en texto (plan §4.4). No hay campo numérico de kilos.
 *
 * @property code código dentro del plan (`L01`); coherente con el día lo comprueba `PlanValidator`.
 * @property order posición dentro del día, desde 1.
 * @property equipmentId aparato del inventario del gimnasio del plan (plan §4.1).
 * @property rest descanso como texto ("2,5-3 min").
 * @property target músculo o función principal, en texto.
 */
data class PlanExercise(
    val id: PlanExerciseId,
    val code: ExerciseCode,
    val order: Int,
    val name: String,
    val equipmentId: EquipmentId,
    val sets: Int,
    val dose: ExerciseDose,
    val rest: String,
    val trial: TrialLoad,
    val target: String,
    val unilateral: Boolean,
    val alternatives: List<Alternative>,
) {
    init {
        require(order >= 1) { "order debe ser >= 1" }
        require(name.isNotBlank()) { "name vacío" }
        require(sets >= 1) { "sets debe ser >= 1" }
    }

    /** Métrica del rango, derivada de [dose]. */
    val metric: Metric
        get() = dose.metric

    /**
     * Series que cuentan para el volumen: [sets] tal cual. Un unilateral no se duplica (las
     * series ya son por lado) y las [alternatives] no aportan nada.
     */
    val effectiveSets: Int
        get() = sets
}

/**
 * Cuánto se hace en cada serie: el rango en la unidad de la métrica y, solo con
 * repeticiones, el RIR. En metros o segundos (carries, planchas) el RIR no aplica y el tipo
 * no deja expresarlo (plan §4.8).
 */
sealed interface ExerciseDose {
    val range: MetricRange
    val metric: Metric

    /**
     * Repeticiones.
     *
     * @property rir RIR previsto; `null` cuando no se prescribe.
     */
    data class Reps(override val range: MetricRange, val rir: RirRange?) : ExerciseDose {
        override val metric: Metric get() = Metric.REPS
    }

    /** Distancia en metros por serie (y por lado si es unilateral). */
    data class Meters(override val range: MetricRange) : ExerciseDose {
        override val metric: Metric get() = Metric.METERS
    }

    /** Duración en segundos por serie. */
    data class Seconds(override val range: MetricRange) : ExerciseDose {
        override val metric: Metric get() = Metric.SECONDS
    }
}

/** Rango `low..high` de la métrica de un ejercicio. Nunca `low > high`. */
data class MetricRange(val low: Int, val high: Int) {
    init {
        require(isValid(low, high)) { "rango inválido: $low-$high" }
    }

    companion object {
        /** La misma regla que el constructor, para que un validador la informe sin lanzar. */
        fun isValid(low: Int, high: Int): Boolean = low >= 1 && low <= high
    }
}

/** RIR previsto: los dos extremos entre 0 y 5 y `low <= high`. */
data class RirRange(val low: Int, val high: Int) {
    init {
        require(isValid(low, high)) { "RIR inválido: $low-$high" }
    }

    companion object {
        /** Valores de RIR admitidos. */
        val BOUNDS: IntRange = 0..5

        /** La misma regla que el constructor, para que un validador la informe sin lanzar. */
        fun isValid(low: Int, high: Int): Boolean = low in BOUNDS && high in BOUNDS && low <= high
    }
}

/**
 * Carga de ensayo (plan §4.4): texto con la unidad que marca el `LoadType` del aparato
 * ("20-35 kg en el selector", "8-12 kg por mancuerna", "Peso corporal, 0 kg"). Se ajusta con
 * la técnica, las repeticiones y el RIR.
 *
 * Es texto a propósito: el modelo no tiene ningún campo de kilos prescritos, así que nada
 * puede calcularlos a partir de la edad, la talla o el peso.
 */
data class TrialLoad(val text: String) {
    init {
        require(text.isNotBlank()) { "carga de ensayo vacía" }
    }
}

/**
 * Alternativa a un ejercicio: se elige una en su lugar. No tiene series propias, así que no
 * puede sumar volumen (plan §4.2).
 *
 * @property equipmentId también del inventario del gimnasio (plan §4.1).
 * @property note cuándo conviene elegirla.
 */
data class Alternative(val name: String, val equipmentId: EquipmentId, val note: String?) {
    init {
        require(name.isNotBlank()) { "name vacío" }
    }
}

/**
 * Registro de un cambio de versión aprobado (plan §4.12): enlaza la versión nueva con la
 * anterior, que se conserva.
 *
 * @property previousPlanId `null` solo en la primera versión.
 * @property reviewId revisión de la que salieron los cambios, si la hubo.
 * @property approvedAt cuándo lo aprobó la persona; un cambio sin aprobar no existe.
 */
data class PlanChange(
    val id: PlanChangeId,
    val planId: PlanId,
    val previousPlanId: PlanId?,
    val reviewId: ReviewId?,
    val date: LocalDate,
    val summary: String,
    val reason: String,
    val approvedAt: Instant,
) {
    init {
        require(previousPlanId != planId) { "un plan no puede ser su propia versión anterior" }
        require((previousPlanId == null) == (planId.version == 1)) {
            "solo la v1 no tiene versión anterior"
        }
        require(previousPlanId == null || previousPlanId.version < planId.version) {
            "la versión anterior debe tener un número menor"
        }
        require(reason.isNotBlank()) { "un cambio de versión necesita motivo" }
    }
}

/** Metadatos de un PDF exportado; el archivo lo gestiona el sistema. */
data class ExportRecord(
    val id: ExportRecordId,
    val planId: PlanId,
    val createdAt: Instant,
    val fileName: String,
    val pages: Int,
) {
    init {
        require(fileName.isNotBlank()) { "fileName vacío" }
        require(pages >= 1) { "pages debe ser >= 1" }
    }
}
