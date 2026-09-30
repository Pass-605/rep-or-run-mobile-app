package app.reporrun.core.database

import app.reporrun.core.database.entity.BlockPreferencesEntity
import app.reporrun.core.database.entity.UserProfileEntity
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileDaoTest {
    private lateinit var db: RepOrRunDatabase
    private val profileDao get() = db.profileDao()

    private val profile = UserProfileEntity(
        id = "profile",
        alias = null,
        age = 30,
        heightCm = 170.0,
        weightKg = 70.5,
        waistCm = null,
        hipCm = null,
        experience = Experience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY,
        priorityMuscles = setOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
        limitations = "",
        preferences = "Máquinas antes que peso libre",
        updatedAt = T0,
    )

    private val preferences = BlockPreferencesEntity(
        id = "prefs",
        availableDays = setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        sessionMinutes = 60,
        blockDays = 14,
        startDate = LocalDate(2026, 10, 5),
        cardioModes = listOf("Caminadora", "Bici, estática"),
        avoidExercises = "",
        unit = LoadUnit.KG,
    )

    @BeforeTest
    fun setUp() {
        db = inMemoryDatabase()
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun hasNoProfileUntilSaved() = runBlocking {
        assertNull(profileDao.observeProfile().first())
    }

    @Test
    fun readsBackTheProfileAfterUpdating() = runBlocking {
        profileDao.upsertProfile(profile)
        profileDao.upsertProfile(profile.copy(age = 31))

        assertEquals(profile.copy(age = 31), profileDao.observeProfile().first())
    }

    @Test
    fun readsBackPreferencesWithTheirCollections() = runBlocking {
        profileDao.upsertPreferences(preferences)

        assertEquals(preferences, profileDao.observePreferences().first())
    }
}
