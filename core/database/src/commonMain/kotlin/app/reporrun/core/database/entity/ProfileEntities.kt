package app.reporrun.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.MuscleGroup
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Fila de `UserProfile`. Hay una por instalación. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val alias: String?,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val experience: Experience,
    val goal: Goal,
    val priorityMuscles: Set<MuscleGroup>,
    val limitations: String,
    val preferences: String,
    val updatedAt: Instant,
)

/** Fila de `BlockPreferences`: las preferencias con las que se pide el siguiente bloque. */
@Entity(tableName = "block_preferences")
data class BlockPreferencesEntity(
    @PrimaryKey val id: String,
    val availableDays: Set<DayOfWeek>,
    val sessionMinutes: Int,
    val blockDays: Int,
    val startDate: LocalDate?,
    val cardioModes: List<String>,
    val avoidExercises: String,
    val unit: LoadUnit,
)
