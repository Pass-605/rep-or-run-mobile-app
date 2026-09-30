package app.reporrun.core.data.seed

import app.reporrun.core.data.mapper.Mapped
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SeedCatalogTest {
    // ---------------------------------------------------------------- la semilla empaquetada

    private val packaged: List<EquipmentCatalogItem> by lazy {
        when (val parsed = SeedCatalog.parse(SeedCatalogSource.json)) {
            is Mapped.Valid -> parsed.value
            is Mapped.Invalid -> error("La semilla no pasa: ${parsed.errors.joinToString("\n")}")
        }
    }

    @Test
    fun packagedSeedPassesEveryCheckOfTheReadme() {
        assertIs<Mapped.Valid<List<EquipmentCatalogItem>>>(SeedCatalog.parse(SeedCatalogSource.json))
    }

    @Test
    fun packagedSeedHasAtLeastFortyEntries() {
        assertTrue(packaged.size >= SeedCatalog.MIN_ITEMS, "solo ${packaged.size} entradas")
    }

    @Test
    fun packagedSeedCoversTheTenFamilies() {
        val missing = SeedFamily.entries.filter { family -> packaged.none(family::matches) }

        assertEquals(emptyList(), missing)
        assertEquals(10, SeedFamily.entries.size)
    }

    @Test
    fun packagedSeedKeysAreUniqueAndSnakeCase() {
        val keys = packaged.map { it.key.value }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { SNAKE_CASE.matches(it) })
    }

    @Test
    fun packagedSeedSearchTermsDoNotCollideAcrossEntries() {
        val owners = packaged
            .flatMap { item -> (listOf(item.name) + item.aliases).map { searchKey(it) to item.key }.distinct() }
            .groupBy({ it.first }, { it.second })

        assertEquals(emptyMap(), owners.filterValues { it.size > 1 })
    }

    @Test
    fun packagedSeedHasAliasesAndMusclesOnEveryEntry() {
        assertTrue(packaged.all { it.aliases.isNotEmpty() && it.primaryMuscles.isNotEmpty() })
    }

    // ---------------------------------------------------------------- cada regla, rota

    @Test
    fun rejectsInvalidJson() {
        val result = assertIs<Mapped.Invalid>(SeedCatalog.parse("""{"catalogVersion": 1, "items": ["""))

        assertTrue(result.errors.single().startsWith("JSON inválido"))
    }

    @Test
    fun rejectsUnknownFields() {
        val json = seedJson(listOf(item("chest_press").replace("\"name\"", "\"extra\": 1, \"name\"")))

        assertTrue(assertIs<Mapped.Invalid>(SeedCatalog.parse(json)).errors.single().startsWith("JSON inválido"))
    }

    @Test
    fun rejectsUnknownCatalogVersion() {
        val result = parseValidPlus(version = 2)

        assertEquals(listOf("catalogVersion: 2 no es la versión conocida (1)"), result.errors)
    }

    @Test
    fun rejectsDuplicatedKeys() {
        val result = parseValidPlus(item("row_machine", name = "Otra", aliases = listOf("Otra más")))

        assertEquals(listOf("items: key repetida: row_machine"), result.errors)
    }

    @Test
    fun rejectsKeysThatAreNotSnakeCase() {
        val result = parseValidPlus(item("Leg-Press", name = "Nueva", aliases = listOf("Nueva prensa")))

        assertEquals(listOf("items[40].key: CatalogKey fuera de formato: Leg-Press"), result.errors)
    }

    @Test
    fun rejectsAliasesThatCollideIgnoringCaseAndAccents() {
        // "Máquina 1" ya es alias de item_1: cambia la mayúscula y el acento, nada más.
        val result = parseValidPlus(item("new_machine", name = "New", aliases = listOf("MAQUINA 1")))

        assertEquals(listOf("items: \"maquina 1\" está en varias entradas: item_1, new_machine"), result.errors)
    }

    @Test
    fun rejectsNameCollidingWithAnotherEntrysAlias() {
        val result = parseValidPlus(item("new_machine", name = "máquina  1", aliases = listOf("Nueva")))

        assertEquals(listOf("items: \"maquina 1\" está en varias entradas: item_1, new_machine"), result.errors)
    }

    @Test
    fun rejectsValuesOutsideTheDomainEnums() {
        val broken = item("new_machine", name = "New", aliases = listOf("Nueva"))
            .replace("\"MACHINE\"", "\"ROBOT\"")
            .replace("\"SELECTOR\"", "\"MAGIC\"")
            .replace("\"CHEST\"", "\"WINGS\"")

        val result = parseValidPlus(broken)

        assertEquals(
            listOf(
                "items[40].category: \"ROBOT\" no es un valor de EquipmentCategory",
                "items[40].loadType: \"MAGIC\" no es un valor de LoadType",
                "items[40].primaryMuscles: \"WINGS\" no es un valor de MuscleGroup",
            ),
            result.errors,
        )
    }

    @Test
    fun rejectsEntriesWithoutAliasesOrMuscles() {
        val result = parseValidPlus(item("new_machine", name = "New", aliases = emptyList(), muscles = emptyList()))

        assertEquals(
            listOf("items[40].aliases: hace falta al menos un alias", "items[40].primaryMuscles: vacío"),
            result.errors,
        )
    }

    @Test
    fun rejectsFewerThanFortyEntriesAndMissingFamilies() {
        val result = assertIs<Mapped.Invalid>(SeedCatalog.parse(seedJson(listOf(item("chest_press")))))

        assertEquals(
            listOf(
                "items: 1 entradas; hacen falta al menos 40",
                "items: faltan familias: discos, poleas, Smith, carro, banco, mancuerna, barra, cardio, peso corporal",
            ),
            result.errors,
        )
    }

    @Test
    fun reportsEveryProblemAtOnce() {
        val result = parseValidPlus(
            item("Bad Key", name = "X1", aliases = listOf("Y1")),
            item("row_machine", name = "X2", aliases = listOf("Y2"), muscles = emptyList()),
            version = 3,
        )

        assertEquals(4, result.errors.size, result.errors.joinToString("\n"))
    }

    // ---------------------------------------------------------------- normalización

    @Test
    fun searchKeyIgnoresCaseAccentsAndExtraSpaces() {
        assertEquals("sentadilla pendulo", searchKey("  Sentadilla   PÉNDULO "))
        assertEquals("pinguino", searchKey("Pingüino"))
    }

    @Test
    fun searchKeyKeepsTheEnye() {
        assertEquals("año", searchKey("Año"))
    }

    // ---------------------------------------------------------------- ayudas

    /**
     * Una semilla sintética que cumple todo (40 entradas y las diez familias) más [extra]: el
     * resultado solo lleva los problemas que añade [extra].
     */
    private fun parseValidPlus(vararg extra: String, version: Int = 1): Mapped.Invalid =
        assertIs<Mapped.Invalid>(SeedCatalog.parse(seedJson(validItems() + extra, version)))

    private fun validItems(): List<String> {
        val families = listOf(
            Triple("row_machine", EquipmentCategory.MACHINE, LoadType.SELECTOR),
            Triple("plate_row", EquipmentCategory.MACHINE, LoadType.PLATES_TOTAL),
            Triple("cable_tower", EquipmentCategory.CABLE, LoadType.SELECTOR),
            Triple("smith_machine", EquipmentCategory.SMITH, LoadType.SMITH),
            Triple("leg_press", EquipmentCategory.MACHINE, LoadType.SLED),
            Triple("flat_bench", EquipmentCategory.BENCH, LoadType.NONE),
            Triple("dumbbells", EquipmentCategory.FREE_WEIGHT, LoadType.DUMBBELL),
            Triple("barbell", EquipmentCategory.FREE_WEIGHT, LoadType.BARBELL),
            Triple("treadmill", EquipmentCategory.CARDIO, LoadType.CARDIO),
            Triple("pullup_bar", EquipmentCategory.BODYWEIGHT, LoadType.BODYWEIGHT),
        )
        val familyItems = families.map { (key, category, loadType) ->
            item(key, name = key, aliases = listOf("alias de $key"), category = category, loadType = loadType)
        }
        val fillers = (1..SeedCatalog.MIN_ITEMS - families.size).map { index ->
            item("item_$index", name = "Item $index", aliases = listOf("Máquina $index"))
        }
        return familyItems + fillers
    }

    private fun item(
        key: String,
        name: String = "Chest Press",
        aliases: List<String> = listOf("Press de pecho"),
        category: EquipmentCategory = EquipmentCategory.MACHINE,
        loadType: LoadType = LoadType.SELECTOR,
        muscles: List<MuscleGroup> = listOf(MuscleGroup.CHEST),
    ): String = """
        {"key": "$key", "name": "$name", "aliases": [${aliases.joinToString { "\"$it\"" }}],
         "category": "${category.name}", "loadType": "${loadType.name}",
         "primaryMuscles": [${muscles.joinToString { "\"${it.name}\"" }}]}
    """.trimIndent()

    private fun seedJson(items: List<String>, version: Int = 1): String =
        """{"catalogVersion": $version, "description": "sintética", "items": [${items.joinToString(",")}]}"""

    private companion object {
        val SNAKE_CASE = Regex("^[a-z0-9]+(_[a-z0-9]+)*$")
    }
}
