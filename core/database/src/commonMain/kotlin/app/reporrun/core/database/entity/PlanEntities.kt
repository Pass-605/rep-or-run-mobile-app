package app.reporrun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.Metric
import app.reporrun.core.model.PlanStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/*
 * El `Plan` del modelo anida sus días y estos sus ejercicios; aquí se aplanan en
 * plan → plan_day → plan_exercise → plan_exercise_alternative, cada nivel con clave foránea
 * al anterior y `CASCADE`: un plan se guarda y se borra entero.
 *
 * Un plan no se edita en el sitio (plan §4.12): lo único que cambia de una fila ya guardada es
 * `plan.status` y `plan_day.status`.
 */

/**
 * Fila de `Plan`. La versión no se guarda: es el `N` de `ROR-AAAA-MM-DD-vN` del [id].
 *
 * Borrar el gimnasio borra sus planes.
 */
@Entity(
    tableName = "plan",
    foreignKeys = [
        ForeignKey(
            entity = GymEntity::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gymId", "status")],
)
data class PlanEntity(
    @PrimaryKey val id: String,
    val gymId: String,
    val createdAt: Instant,
    val status: PlanStatus,
    val blockDays: Int,
    val startDate: LocalDate?,
    val reviewDate: LocalDate?,
    val focus: List<String>,
    val notes: List<String>,
    val inputSnapshotJson: String,
    val model: String,
    val promptVersion: String,
    val schemaVersion: String,
)

/** Fila de `PlanDay`. [cardio] es `null` si el día no lleva cardio. */
@Entity(
    tableName = "plan_day",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class PlanDayEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val dayOfWeek: DayOfWeek,
    val dayIndex: Int,
    val session: String,
    val priority: String,
    @Embedded(prefix = "cardio_") val cardio: CardioColumns?,
    val warmup: String,
    val mobility: String,
    val cooldown: String,
    val status: DayStatus,
)

/**
 * `CardioPrescription` en columnas propias de `plan_day` (`cardio_minMinutes`, ...). Sin cardio,
 * todas son `NULL` y Room devuelve `cardio = null`. Con cardio, [minMinutes] y [maxMinutes]
 * nunca son `NULL`: el mapper no debe escribir un cardio sin minutos.
 */
data class CardioColumns(
    val minMinutes: Int,
    val maxMinutes: Int,
    val mode: String?,
    val rpe: String?,
    val speed: String?,
    val incline: String?,
)

/**
 * Fila de `PlanExercise`. [dose] se aplana en `metric`, `low`, `high`, `rirLow` y `rirHigh`,
 * los mismos campos que el plan §10 y el contrato.
 *
 * [equipmentId] apunta al inventario sin `onDelete`: un aparato que usa algún plan, aunque
 * esté archivado, no se puede borrar (la versión anterior se conserva, plan §4.12). Se comprueba
 * al final de la sentencia, así que borrar el gimnasio entero sí funciona.
 */
@Entity(
    tableName = "plan_exercise",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
        ),
    ],
    indices = [Index("planDayId"), Index("equipmentId")],
)
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    val planDayId: String,
    val code: String,
    // `order` es palabra reservada de SQL: en columna se llama `sortOrder`.
    @ColumnInfo(name = "sortOrder") val order: Int,
    val name: String,
    val equipmentId: String,
    val sets: Int,
    @Embedded val dose: DoseColumns,
    val rest: String,
    val trial: String,
    val target: String,
    val unilateral: Boolean,
)

/**
 * `ExerciseDose` aplanado. [metric] es el discriminador del sellado:
 *
 * - `REPS` → `ExerciseDose.Reps`; con `rirLow` y `rirHigh` los dos presentes hay RIR, con los
 *   dos `NULL` no. Uno solo presente es un dato corrupto.
 * - `METERS` / `SECONDS` → `Meters` / `Seconds`; `rirLow` y `rirHigh` siempre `NULL`.
 *
 * Esa correspondencia la aplica el mapper (B-04), que reconstruye el sellado.
 */
data class DoseColumns(val metric: Metric, val low: Int, val high: Int, val rirLow: Int?, val rirHigh: Int?)

/**
 * Una alternativa de un ejercicio, en su [position] dentro de la lista (desde 0). Sin series
 * propias: no suma volumen (plan §4.2). [equipmentId] se protege igual que en [PlanExerciseEntity].
 */
@Entity(
    tableName = "plan_exercise_alternative",
    primaryKeys = ["planExerciseId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["planExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
        ),
    ],
    indices = [Index("equipmentId")],
)
data class PlanExerciseAlternativeEntity(
    val planExerciseId: String,
    val position: Int,
    val name: String,
    val equipmentId: String,
    val note: String?,
)

/**
 * Fila de `PlanChange`: enlaza una versión con la anterior.
 *
 * - [planId]: `CASCADE`, el registro es de ese plan.
 * - [previousPlanId]: sin `onDelete`, así no se puede borrar una versión que otra tiene como
 *   anterior; el historial se conserva (plan §4.12).
 * - [reviewId]: `SET NULL`, perder la revisión no invalida el cambio aprobado.
 */
@Entity(
    tableName = "plan_change",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["previousPlanId"],
        ),
        ForeignKey(
            entity = ReviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("planId"), Index("previousPlanId"), Index("reviewId")],
)
data class PlanChangeEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val previousPlanId: String?,
    val reviewId: String?,
    val date: LocalDate,
    val summary: String,
    val reason: String,
    val approvedAt: Instant,
)

/** Metadatos de un PDF exportado. Se van con su plan. */
@Entity(
    tableName = "export_record",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class ExportRecordEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val createdAt: Instant,
    val fileName: String,
    val pages: Int,
)
