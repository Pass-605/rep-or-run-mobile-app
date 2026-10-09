package app.reporrun.feature.gym

import app.reporrun.core.domain.usecase.CatalogSearch
import app.reporrun.core.domain.usecase.GymInventory
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCatalogItem
import app.reporrun.core.model.EquipmentCategory

/*
 * De lo guardado a las filas de la lista. Son funciones puras: el ViewModel las llama cuando
 * cambia lo guardado, la búsqueda o el filtro.
 */

/** Una máquina con el texto en el que se busca, calculado una vez y no en cada tecla. */
internal class InventoryEntry(val item: InventoryRow.Item, val searchIndex: String)

/**
 * Todas las máquinas que se pueden ver en un gimnasio, por categoría y, dentro, por nombre: el
 * catálogo entero, con lo que el gimnasio ya tiene marcado, y sus máquinas propias.
 *
 * **El cruce es por clave, nunca por nombre**: una entrada está marcada si el gimnasio tiene un
 * aparato con su `catalogKey`. Un aparato cuya clave ya no está en el catálogo se conserva como
 * fila propia, para que se pueda seguir viendo y quitando.
 */
internal fun GymInventory.toEntries(): List<InventoryEntry> {
    val owned = equipment.filter { it.catalogKey != null }.associateBy { it.catalogKey }
    val catalogKeys = catalog.map { it.key }.toSet()
    val fromCatalog = catalog.map { it.toEntry(owned[it.key]) }
    val own = equipment.filter { it.catalogKey !in catalogKeys }.map { it.toEntry() }
    return (fromCatalog + own).sortedWith(compareBy({ it.item.category.ordinal }, { it.searchIndex }))
}

private fun EquipmentCatalogItem.toEntry(owned: Equipment?) = InventoryEntry(
    item = InventoryRow.Item(
        key = "c:${key.value}",
        catalogKey = key,
        equipmentId = owned?.id,
        name = name,
        category = category,
        loadType = loadType,
        minKg = owned?.minKg,
        maxKg = owned?.maxKg,
    ),
    searchIndex = CatalogSearch.index(name, aliases),
)

private fun Equipment.toEntry() = InventoryEntry(
    item = InventoryRow.Item(
        key = "e:${id.value}",
        catalogKey = catalogKey,
        equipmentId = id,
        name = name,
        category = category,
        loadType = loadType,
        minKg = minKg,
        maxKg = maxKg,
        isCustom = isCustom,
    ),
    searchIndex = CatalogSearch.index(name),
)

/**
 * Las filas que deja ver el filtro. Sin [category], cada grupo lleva su cabecera; con una
 * categoría elegida no hace falta: ya lo dice el chip.
 */
internal fun List<InventoryEntry>.toRows(query: String, category: EquipmentCategory?): List<InventoryRow> {
    val visible = filter { entry ->
        (category == null || entry.item.category == category) && CatalogSearch.matches(entry.searchIndex, query)
    }
    return if (category != null) {
        visible.map { it.item }
    } else {
        buildList {
            visible.forEach { entry ->
                val previous = (lastOrNull() as? InventoryRow.Item)?.category
                if (previous != entry.item.category) add(InventoryRow.Header(entry.item.category))
                add(entry.item)
            }
        }
    }
}

/** Las categorías que tienen alguna máquina, en el orden en que salen los grupos. */
internal fun List<InventoryEntry>.categories(): List<EquipmentCategory> = map { it.item.category }.distinct()
