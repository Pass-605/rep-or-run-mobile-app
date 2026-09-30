package app.reporrun.core.domain.repository

import app.reporrun.core.model.EquipmentCatalogItem
import kotlinx.coroutines.flow.Flow

/**
 * Catálogo semilla de equipos, de solo lectura. La persona marca de aquí lo que tiene su
 * gimnasio en lugar de escribirlo.
 */
interface EquipmentCatalogRepository {
    /** El catálogo, por nombre. */
    fun observeCatalog(): Flow<List<EquipmentCatalogItem>>

    /**
     * Carga la semilla empaquetada con la app. Idempotente: se llama en cada arranque, solo
     * inserta las claves que falten y nunca toca una que ya existe.
     *
     * @return cuántas entradas nuevas se insertaron.
     */
    suspend fun loadSeed(): Int
}
