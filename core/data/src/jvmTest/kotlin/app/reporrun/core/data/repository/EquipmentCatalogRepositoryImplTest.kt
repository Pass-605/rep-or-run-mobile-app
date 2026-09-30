package app.reporrun.core.data.repository

import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EquipmentCatalogRepositoryImplTest {
    private val db = inMemoryDatabase()

    @AfterTest
    fun close() = db.close()

    @Test
    fun loadsThePackagedSeedOnFirstLaunch() = runTest {
        val repository = EquipmentCatalogRepositoryImpl(db.equipmentDao())

        val inserted = repository.loadSeed()

        val catalog = repository.observeCatalog().first()
        assertEquals(inserted, catalog.size)
        assertTrue(catalog.size >= 40, "solo ${catalog.size} entradas")
        assertEquals(catalog.sortedBy { it.name }, catalog)
    }

    @Test
    fun loadingTheSeedTwiceInsertsNothingTheSecondTime() = runTest {
        val repository = EquipmentCatalogRepositoryImpl(db.equipmentDao())
        repository.loadSeed()
        val before = repository.observeCatalog().first()

        assertEquals(0, repository.loadSeed())
        assertEquals(before, repository.observeCatalog().first())
    }

    @Test
    fun keepsMuscleOrderAndAliasesOfTheSeed() = runTest {
        val repository = EquipmentCatalogRepositoryImpl(db.equipmentDao())
        repository.loadSeed()

        val pendulum = repository.observeCatalog().first().single { it.key == CatalogKey("pendulum_squat") }

        assertEquals(EquipmentCategory.MACHINE, pendulum.category)
        assertEquals(LoadType.SLED, pendulum.loadType)
        assertEquals(MuscleGroup.QUADS, pendulum.primaryMuscles.first())
        assertTrue(pendulum.aliases.isNotEmpty())
    }

    @Test
    fun anUpdatedSeedOnlyAddsTheMissingKeys() = runTest {
        EquipmentCatalogRepositoryImpl(db.equipmentDao(), seedWith("chest_press" to "Chest Press")).loadSeed()

        val inserted = EquipmentCatalogRepositoryImpl(
            db.equipmentDao(),
            seedWith("chest_press" to "Chest Press renombrada", "leg_press" to "Leg Press"),
        ).loadSeed()

        val catalog = EquipmentCatalogRepositoryImpl(db.equipmentDao()).observeCatalog().first()
        assertEquals(1, inserted)
        // La clave que ya estaba no se toca (README de la semilla, paso 5).
        val names = catalog.filterNot { it.key.value.startsWith("filler_") }.map { it.name }
        assertEquals(listOf("Chest Press", "Leg Press"), names)
    }

    @Test
    fun refusesABrokenSeedWithEveryProblem() = runTest {
        val repository =
            EquipmentCatalogRepositoryImpl(
                db.equipmentDao(),
                """{"catalogVersion": 9, "description": "", "items": []}""",
            )

        val failure = assertFailsWith<IllegalStateException> { repository.loadSeed() }

        assertTrue(failure.message.orEmpty().contains("catalogVersion"))
        assertTrue(failure.message.orEmpty().contains("hacen falta al menos 40"))
        assertEquals(0, db.equipmentDao().countCatalog())
    }

    /**
     * La semilla empaquetada con [entries] de más (clave → nombre), así la carga pasa todas las
     * comprobaciones y solo cambia lo que se prueba.
     */
    private fun seedWith(vararg entries: Pair<String, String>): String {
        val items = entries.joinToString(",") { (key, name) ->
            """{"key": "$key", "name": "$name", "aliases": ["alias único de $key"], "category": "MACHINE",
               "loadType": "SELECTOR", "primaryMuscles": ["CHEST"]}"""
        }
        val fillers = (1..40).joinToString(",") { index ->
            val (category, loadType) = FAMILIES[index % FAMILIES.size]
            """{"key": "filler_$index", "name": "Filler $index", "aliases": ["relleno $index"], "category": "$category",
               "loadType": "$loadType", "primaryMuscles": ["CORE"]}"""
        }
        return """{"catalogVersion": 1, "description": "sintética", "items": [$items, $fillers]}"""
    }

    private companion object {
        val FAMILIES = listOf(
            "MACHINE" to "SELECTOR",
            "MACHINE" to "PLATES_TOTAL",
            "CABLE" to "SELECTOR",
            "SMITH" to "SMITH",
            "MACHINE" to "SLED",
            "BENCH" to "NONE",
            "FREE_WEIGHT" to "DUMBBELL",
            "FREE_WEIGHT" to "BARBELL",
            "CARDIO" to "CARDIO",
            "BODYWEIGHT" to "BODYWEIGHT",
        )
    }
}
