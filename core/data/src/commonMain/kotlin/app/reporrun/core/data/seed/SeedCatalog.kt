package app.reporrun.core.data.seed

import app.reporrun.core.data.mapper.Mapped
import app.reporrun.core.data.mapper.ModelErrors
import app.reporrun.core.model.CatalogKey
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.MuscleGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lectura y comprobación de `seed/equipment_catalog.json` (formato y reglas en `seed/README.md`).
 *
 * Los enums se leen como texto y se comprueban aquí, no al deserializar, para poder listar
 * todas las entradas mal escritas de una vez en lugar de parar en la primera.
 */
internal object SeedCatalog {
    /** Versión de la forma del archivo que entiende esta app. */
    const val SUPPORTED_VERSION: Int = 1

    /** Mínimo de entradas del catálogo (B-01). */
    const val MIN_ITEMS: Int = 40

    private val json = Json { ignoreUnknownKeys = false }

    /** El catálogo si cumple todas las reglas del README, o la lista completa de problemas. */
    fun parse(source: String): Mapped<List<EquipmentCatalogItem>> {
        val file = try {
            json.decodeFromString<SeedFile>(source)
        } catch (e: IllegalArgumentException) {
            // SerializationException hereda de aquí: JSON mal formado, campos que faltan o sobran.
            return Mapped.Invalid(listOf("JSON inválido: ${e.message}"))
        }

        val errors = ModelErrors()
        if (file.catalogVersion != SUPPORTED_VERSION) {
            errors.report("catalogVersion", "${file.catalogVersion} no es la versión conocida ($SUPPORTED_VERSION)")
        }
        val items = file.items.mapIndexedNotNull { index, item -> item.toModel("items[$index]", errors) }
        checkUniqueKeys(file.items, errors)
        checkUniqueSearchTerms(file.items, errors)
        if (file.items.size < MIN_ITEMS) {
            errors.report("items", "${file.items.size} entradas; hacen falta al menos $MIN_ITEMS")
        }
        val missing = SeedFamily.entries.filter { family -> items.none(family::matches) }
        if (missing.isNotEmpty()) {
            errors.report("items", "faltan familias: ${missing.joinToString { it.label }}")
        }
        return if (errors.size > 0) Mapped.Invalid(errors.errors) else Mapped.Valid(items)
    }

    private fun SeedItem.toModel(path: String, errors: ModelErrors): EquipmentCatalogItem? {
        val before = errors.size
        val catalogKey = errors.build("$path.key") { CatalogKey(key) }
        if (aliases.isEmpty()) errors.report("$path.aliases", "hace falta al menos un alias")
        val category = enumOrReport<EquipmentCategory>(category, "$path.category", errors)
        val loadType = enumOrReport<LoadType>(loadType, "$path.loadType", errors)
        val muscles = primaryMuscles.map { enumOrReport<MuscleGroup>(it, "$path.primaryMuscles", errors) }
        if (primaryMuscles.isEmpty()) errors.report("$path.primaryMuscles", "vacío")
        if (primaryMuscles.distinct().size != primaryMuscles.size) errors.report("$path.primaryMuscles", "repetidos")
        if (errors.size > before) return null

        // Sin errores anotados, las piezas están todas: los requireNotNull no pueden fallar.
        return errors.build(path) {
            EquipmentCatalogItem(
                key = requireNotNull(catalogKey),
                name = name,
                aliases = aliases,
                category = requireNotNull(category),
                loadType = requireNotNull(loadType),
                primaryMuscles = muscles.filterNotNull().toSet(),
            )
        }
    }

    private inline fun <reified E : Enum<E>> enumOrReport(value: String, path: String, errors: ModelErrors): E? {
        val match = enumValues<E>().firstOrNull { it.name == value }
        if (match == null) errors.report(path, "\"$value\" no es un valor de ${E::class.simpleName}")
        return match
    }

    private fun checkUniqueKeys(items: List<SeedItem>, errors: ModelErrors) {
        items.groupBy { it.key }.filterValues { it.size > 1 }.keys.forEach { key ->
            errors.report("items", "key repetida: $key")
        }
    }

    /**
     * Ningún `name` ni alias se repite entre entradas distintas, ignorando mayúsculas y
     * acentos: el buscador quedaría ambiguo. Dentro de una misma entrada da igual.
     */
    private fun checkUniqueSearchTerms(items: List<SeedItem>, errors: ModelErrors) {
        items
            .flatMap { item -> (listOf(item.name) + item.aliases).map { searchKey(it) to item.key }.distinct() }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
            .forEach { (term, keys) ->
                errors.report("items", "\"$term\" está en varias entradas: ${keys.joinToString()}")
            }
    }
}

/**
 * Forma de comparar términos de búsqueda: sin mayúsculas, sin acentos ni diéresis y con los
 * espacios normalizados. La `ñ` se conserva: es otra letra, no una `n` acentuada.
 */
internal fun searchKey(term: String): String =
    term.trim().lowercase().map { ACCENT_FOLDS[it] ?: it }.joinToString("").replace(WHITESPACE, " ")

private val ACCENT_FOLDS: Map<Char, Char> = buildMap {
    "áàâäã".forEach { put(it, 'a') }
    "éèêë".forEach { put(it, 'e') }
    "íìîï".forEach { put(it, 'i') }
    "óòôöõ".forEach { put(it, 'o') }
    "úùûü".forEach { put(it, 'u') }
    put('ç', 'c')
}

private val WHITESPACE = Regex("\\s+")

/**
 * Las diez familias que el catálogo tiene que cubrir (`seed/README.md`). La mayoría es un
 * `LoadType`; poleas y bancos son una categoría, porque su carga se lee como la de otras.
 */
internal enum class SeedFamily(val label: String, private val predicate: (EquipmentCatalogItem) -> Boolean) {
    SELECTOR("selector", { it.loadType == LoadType.SELECTOR && it.category == EquipmentCategory.MACHINE }),
    PLATES("discos", { it.loadType == LoadType.PLATES_TOTAL }),
    CABLES("poleas", { it.category == EquipmentCategory.CABLE }),
    SMITH("Smith", { it.loadType == LoadType.SMITH }),
    SLED("carro", { it.loadType == LoadType.SLED }),
    BENCH("banco", { it.category == EquipmentCategory.BENCH }),
    DUMBBELL("mancuerna", { it.loadType == LoadType.DUMBBELL }),
    BARBELL("barra", { it.loadType == LoadType.BARBELL }),
    CARDIO("cardio", { it.loadType == LoadType.CARDIO }),
    BODYWEIGHT("peso corporal", { it.loadType == LoadType.BODYWEIGHT }),
    ;

    fun matches(item: EquipmentCatalogItem): Boolean = predicate(item)
}

@Serializable
private data class SeedFile(val catalogVersion: Int, val description: String, val items: List<SeedItem>)

@Serializable
private data class SeedItem(
    val key: String,
    val name: String,
    val aliases: List<String>,
    val category: String,
    val loadType: String,
    val primaryMuscles: List<String>,
)
