package app.reporrun.core.database.di

import app.reporrun.core.database.RepOrRunDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Los DAOs de [RepOrRunDatabase]. La base de datos no se construye aquí: necesita el builder de
 * la plataforma y el contexto de las consultas, y los dos los pone el grafo de `composeApp`.
 *
 * Los DAOs son `single` porque Room ya devuelve siempre la misma instancia de cada uno.
 */
val databaseModule: Module = module {
    single { get<RepOrRunDatabase>().profileDao() }
    single { get<RepOrRunDatabase>().gymDao() }
    single { get<RepOrRunDatabase>().equipmentDao() }
    single { get<RepOrRunDatabase>().planDao() }
    single { get<RepOrRunDatabase>().planHistoryDao() }
    single { get<RepOrRunDatabase>().reviewDao() }
    single { get<RepOrRunDatabase>().checkInDao() }
}
