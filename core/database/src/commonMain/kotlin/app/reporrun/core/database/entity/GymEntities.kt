package app.reporrun.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.MuscleGroup
import kotlin.time.Instant

/** Fila de `Gym`. Solo uno tiene [isActive]; lo mantiene [app.reporrun.core.database.dao.GymDao.setActive]. */
@Entity(tableName = "gym", indices = [Index("isActive")])
data class GymEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isActive: Boolean,
    val notes: String,
    val createdAt: Instant,
)

/**
 * Fila de `Equipment`: un aparato del inventario de un gimnasio.
 *
 * Borrar el gimnasio borra su inventario (`CASCADE`). [catalogKey] no es clave foránea: el
 * catálogo es una copia de la semilla que se puede recargar, y el aparato ya guarda todo lo
 * que necesita (nombre, categoría, `loadType`).
 */
@Entity(
    tableName = "equipment",
    foreignKeys = [
        ForeignKey(
            entity = GymEntity::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gymId")],
)
data class EquipmentEntity(
    @PrimaryKey val id: String,
    val gymId: String,
    val catalogKey: String?,
    val name: String,
    val category: EquipmentCategory,
    val loadType: LoadType,
    val minKg: Double?,
    val maxKg: Double?,
    val incrementKg: Double?,
    val notes: String?,
    val isCustom: Boolean,
)

/** Entrada del catálogo semilla. Solo lectura para la persona; la carga B-04. */
@Entity(tableName = "equipment_catalog")
data class EquipmentCatalogEntity(
    @PrimaryKey val key: String,
    val name: String,
    val aliases: List<String>,
    val category: EquipmentCategory,
    val loadType: LoadType,
    val primaryMuscles: Set<MuscleGroup>,
)
