package app.reporrun.core.ai.contract

import kotlinx.serialization.Serializable

/*
 * Objetos de `common.schema.json`. Un campo con `= null` es opcional en el schema; uno
 * anulable sin valor por defecto es obligatorio y puede valer `null`. Los límites del schema
 * (rangos, longitudes, `minItems`) no se comprueban aquí: los DTOs solo dan forma.
 */

/** `Profile`: el perfil que viaja al generar. Sin nombre real: solo un alias opcional. */
@Serializable
data class ProfileDto(
    val alias: String? = null,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val experience: ExperienceDto,
    val goal: GoalDto,
    val priorityMuscles: List<MuscleGroupDto>,
    val limitations: String,
    val preferences: String,
)

/** `Equipment`: una máquina disponible del gimnasio activo. */
@Serializable
data class EquipmentDto(
    val id: String,
    val name: String,
    val category: EquipmentCategoryDto,
    val loadType: LoadTypeDto,
    val minKg: Double? = null,
    val maxKg: Double? = null,
    val incrementKg: Double? = null,
    val notes: String? = null,
)

/** `Gym`: el gimnasio activo con su inventario. */
@Serializable
data class GymDto(val name: String, val equipment: List<EquipmentDto>)

/** `BlockPreferences`. [startDate] es una fecha ISO (`AAAA-MM-DD`) tal cual viaja. */
@Serializable
data class BlockPreferencesDto(
    val availableDays: List<DayOfWeekDto>,
    val sessionMinutes: Int,
    val blockDays: Int,
    val startDate: String? = null,
    val cardioModes: List<String>,
    val avoidExercises: String,
    val unit: UnitDto,
)

/** `Usage`: los tokens que gastó la llamada. Lo añade la Lambda, no el modelo. */
@Serializable
data class UsageDto(val promptTokens: Int, val completionTokens: Int, val totalTokens: Int)
