package app.reporrun.core.data.repository

import app.reporrun.core.data.GYM_ID
import app.reporrun.core.data.equipment
import app.reporrun.core.data.gym
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
    fun anUpdatedSeedAddsTheMissingKeysAndRenamesTheKnownOnes() = runTest {
        EquipmentCatalogRepositoryImpl(db.equipmentDao(), seedWith("chest_press" to "Chest Press")).loadSeed()

        val inserted = EquipmentCatalogRepositoryImpl(
            db.equipmentDao(),
            seedWith("chest_press" to "Press de pecho", "leg_press" to "Prensa de piernas"),
        ).loadSeed()

        val catalog = EquipmentCatalogRepositoryImpl(db.equipmentDao()).observeCatalog().first()
        assertEquals(1, inserted)
        // La clave que ya estaba sigue ahí, con el nombre de la semilla nueva (README, paso 5).
        val names = catalog.filterNot { it.key.value.startsWith("filler_") }.associate { it.key.value to it.name }
        assertEquals(mapOf("chest_press" to "Press de pecho", "leg_press" to "Prensa de piernas"), names)
    }

    @Test
    fun anUpdatedSeedRenamesTheEquipmentMarkedFromTheCatalog() = runTest {
        val gyms = GymRepositoryImpl(db.gymDao(), db.equipmentDao())
        gyms.saveGym(gym())
        val marked = equipment("eq_marked").copy(catalogKey = CatalogKey("chest_press"), name = "Chest Press")
        val custom = equipment("eq_custom").copy(catalogKey = null, name = "Mi máquina", isCustom = true)
        gyms.saveEquipment(listOf(marked, custom))

        EquipmentCatalogRepositoryImpl(db.equipmentDao(), seedWith("chest_press" to "Press de pecho")).loadSeed()

        assertEquals(
            setOf(marked.copy(name = "Press de pecho"), custom),
            gyms.observeEquipment(GYM_ID).first().toSet(),
        )
    }

    @Test
    fun theEnglishNameOfTheSeedIsSearchableAsAnAlias() = runTest {
        val repository = EquipmentCatalogRepositoryImpl(db.equipmentDao())
        repository.loadSeed()

        val legPress = repository.observeCatalog().first().single { it.key == CatalogKey("leg_press") }

        assertEquals("Prensa de piernas", legPress.name)
        assertTrue("Leg Press" in legPress.aliases)
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
            """{"key": "$key", "name": "$name", "nameEn": "$key en inglés", "aliases": [], "category": "MACHINE",
               "loadType": "SELECTOR", "primaryMuscles": ["CHEST"]}"""
        }
        val fillers = (1..40).joinToString(",") { index ->
            val (category, loadType) = FAMILIES[index % FAMILIES.size]
            """{"key": "filler_$index", "name": "Relleno $index", "nameEn": "Filler $index", "aliases": [],
               "category": "$category",
               "loadType": "$loadType", "primaryMuscles": ["CORE"]}"""
        }
        return """{"catalogVersion": 2, "description": "sintética", "items": [$items, $fillers]}"""
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
