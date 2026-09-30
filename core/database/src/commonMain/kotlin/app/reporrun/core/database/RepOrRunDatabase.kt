package app.reporrun.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.reporrun.core.database.dao.CheckInDao
import app.reporrun.core.database.dao.EquipmentDao
import app.reporrun.core.database.dao.GymDao
import app.reporrun.core.database.dao.PlanDao
import app.reporrun.core.database.dao.PlanHistoryDao
import app.reporrun.core.database.dao.ProfileDao
import app.reporrun.core.database.dao.ReviewDao
import app.reporrun.core.database.entity.BlockPreferencesEntity
import app.reporrun.core.database.entity.ChangeProposalEntity
import app.reporrun.core.database.entity.CheckInEntity
import app.reporrun.core.database.entity.EquipmentCatalogEntity
import app.reporrun.core.database.entity.EquipmentEntity
import app.reporrun.core.database.entity.ExerciseFeedbackEntity
import app.reporrun.core.database.entity.ExportRecordEntity
import app.reporrun.core.database.entity.GymEntity
import app.reporrun.core.database.entity.PlanChangeEntity
import app.reporrun.core.database.entity.PlanDayEntity
import app.reporrun.core.database.entity.PlanEntity
import app.reporrun.core.database.entity.PlanExerciseAlternativeEntity
import app.reporrun.core.database.entity.PlanExerciseEntity
import app.reporrun.core.database.entity.ReviewEntity
import app.reporrun.core.database.entity.ReviewScheduleEntity
import app.reporrun.core.database.entity.UserProfileEntity
import kotlin.coroutines.CoroutineContext

/**
 * Base de datos local de Rep or Run (ADR 0002). El esquema de cada versión se exporta a
 * `core/database/schemas/`; a partir de la primera release interna, cada cambio lleva su
 * `Migration` y su test, nunca una migración destructiva.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        BlockPreferencesEntity::class,
        GymEntity::class,
        EquipmentEntity::class,
        EquipmentCatalogEntity::class,
        PlanEntity::class,
        PlanDayEntity::class,
        PlanExerciseEntity::class,
        PlanExerciseAlternativeEntity::class,
        PlanChangeEntity::class,
        ExportRecordEntity::class,
        ReviewScheduleEntity::class,
        ReviewEntity::class,
        CheckInEntity::class,
        ExerciseFeedbackEntity::class,
        ChangeProposalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DateConverters::class, CollectionConverters::class)
@ConstructedBy(RepOrRunDatabaseConstructor::class)
abstract class RepOrRunDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun gymDao(): GymDao

    abstract fun equipmentDao(): EquipmentDao

    abstract fun planDao(): PlanDao

    abstract fun planHistoryDao(): PlanHistoryDao

    abstract fun reviewDao(): ReviewDao

    abstract fun checkInDao(): CheckInDao

    companion object {
        const val FILE_NAME: String = "reporrun.db"
    }
}

/** Lo implementa el compilador de Room en cada plataforma. */
@Suppress("KotlinNoActualForExpect")
expect object RepOrRunDatabaseConstructor : RoomDatabaseConstructor<RepOrRunDatabase> {
    override fun initialize(): RepOrRunDatabase
}

/**
 * Termina el builder de la plataforma con lo que es común: SQLite bundled (el mismo motor en
 * Android e iOS) y el contexto donde corren las consultas, que se inyecta.
 */
fun RoomDatabase.Builder<RepOrRunDatabase>.buildRepOrRunDatabase(queryContext: CoroutineContext): RepOrRunDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .build()
