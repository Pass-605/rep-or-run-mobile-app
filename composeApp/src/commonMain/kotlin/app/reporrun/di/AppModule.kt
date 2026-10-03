package app.reporrun.di

import androidx.room.RoomDatabase
import app.reporrun.core.data.di.dataModule
import app.reporrun.core.database.RepOrRunDatabase
import app.reporrun.core.database.buildRepOrRunDatabase
import app.reporrun.core.database.di.databaseModule
import app.reporrun.core.datastore.di.datastoreModule
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.di.domainModule
import app.reporrun.scaffold.scaffoldModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * Lo que es del proceso y no de un módulo concreto: el reloj, la zona horaria, los dispatchers
 * y la base de datos. Es el único sitio de la app donde aparecen `Clock.System` y
 * `Dispatchers.*`; el resto los recibe por constructor.
 */
internal val appModule: Module = module {
    single<Clock> { Clock.System }

    // `factory`, no `single`: la zona del sistema puede cambiar con la app abierta (un viaje),
    // y un `single` se quedaría con la del arranque.
    factory<TimeZone> { TimeZone.currentSystemDefault() }

    single { AppDispatchers(main = Dispatchers.Main, default = Dispatchers.Default, io = Dispatchers.IO) }

    // El builder lo pone el `platformModule`; lo común (SQLite bundled y dónde corren las
    // consultas) se termina aquí.
    single<RepOrRunDatabase> {
        get<RoomDatabase.Builder<RepOrRunDatabase>>().buildRepOrRunDatabase(queryContext = get<AppDispatchers>().io)
    }
}

/**
 * El grafo completo, en orden de capas. Un feature nuevo añade aquí su `xxxModule`; el test
 * `AppGraphTest` falla si a alguna definición le falta una dependencia.
 *
 * @param platformModule lo que depende de la plataforma: el builder de Room y el DataStore.
 */
internal fun appModules(platformModule: Module): List<Module> = listOf(
    platformModule,
    appModule,
    databaseModule,
    datastoreModule,
    dataModule,
    domainModule,
    scaffoldModule,
)

/** Arranca Koin. Se llama una vez por proceso, desde el punto de entrada de cada plataforma. */
internal fun startRepOrRunKoin(platformModule: Module): KoinApplication = startKoin {
    modules(appModules(platformModule))
}

/**
 * Scope para lo que vive tanto como el proceso y hace E/S por su cuenta (el DataStore). El
 * `SupervisorJob` evita que un fallo de una escritura cancele las siguientes.
 */
internal fun Scope.processIoScope(): CoroutineScope = CoroutineScope(SupervisorJob() + get<AppDispatchers>().io)
