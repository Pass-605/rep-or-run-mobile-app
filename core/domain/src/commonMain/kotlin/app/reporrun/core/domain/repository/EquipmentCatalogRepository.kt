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
     * Carga la semilla empaquetada con la app. Idempotente: se llama en cada arranque, inserta
     * las claves que falten y pone al día el nombre y los alias de las que ya estaban, también
     * en los aparatos que la persona marcó desde el catálogo. Una clave no cambia nunca.
     *
     * @return cuántas entradas nuevas se insertaron.
     */
    suspend fun loadSeed(): Int
}
