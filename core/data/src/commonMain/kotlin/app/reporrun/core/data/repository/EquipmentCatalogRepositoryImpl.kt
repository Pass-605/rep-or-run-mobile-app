package app.reporrun.core.data.repository

import app.reporrun.core.data.mapper.toEntity
import app.reporrun.core.data.mapper.toModel
import app.reporrun.core.data.seed.SeedCatalog
import app.reporrun.core.data.seed.SeedCatalogSource
import app.reporrun.core.database.dao.EquipmentDao
import app.reporrun.core.domain.repository.EquipmentCatalogRepository
import app.reporrun.core.domain.validation.Mapped
import app.reporrun.core.model.EquipmentCatalogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Catálogo sobre la tabla `equipment_catalog`, que se llena con la semilla empaquetada.
 *
 * @param seedJson el JSON de la semilla; por defecto, el de `seed/equipment_catalog.json`
 *   generado en la compilación.
 */
class EquipmentCatalogRepositoryImpl internal constructor(
    private val dao: EquipmentDao,
    private val seedJson: String,
) : EquipmentCatalogRepository {
    constructor(dao: EquipmentDao) : this(dao, SeedCatalogSource.json)

    override fun observeCatalog(): Flow<List<EquipmentCatalogItem>> =
        dao.observeCatalog().map { rows -> rows.map { it.toModel() } }

    /**
     * Las claves nuevas de una versión posterior de la app se añaden y las que ya estaban toman
     * el texto de la semilla (`seed/README.md`); los aparatos marcados desde el catálogo siguen
     * el nombre de su entrada. Una semilla rota es un fallo de la build, no un error de la
     * persona: lanza con todos los problemas, y el test de la semilla lo impide antes de
     * publicar.
     */
    override suspend fun loadSeed(): Int {
        val items = when (val parsed = SeedCatalog.parse(seedJson)) {
            is Mapped.Valid -> parsed.value
            is Mapped.Invalid -> error("Semilla del catálogo inválida: ${parsed.errors.joinToString("; ")}")
        }
        val before = dao.countCatalog()
        dao.upsertCatalog(items.map { it.toEntity() })
        dao.syncCatalogNames()
        return dao.countCatalog() - before
    }
}
