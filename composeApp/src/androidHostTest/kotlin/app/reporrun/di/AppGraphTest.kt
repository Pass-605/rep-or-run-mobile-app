package app.reporrun.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.RoomDatabase
import app.reporrun.core.database.RepOrRunDatabase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * Comprueba que el grafo está completo sin arrancar nada: recorre los constructores de todas
 * las definiciones y falla si alguno pide un tipo que nadie define. Koin resuelve en
 * ejecución, así que sin este test una definición olvidada se descubre al abrir la pantalla.
 *
 * `verify()` es API de JVM, por eso el test vive en `androidHostTest` y no en `commonTest`.
 */
class AppGraphTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionFindsItsDependencies() {
        // El módulo de plataforma real necesita un Context; aquí basta con declarar los dos
        // tipos que aporta, sin construirlos.
        val platformTypes = module {
            single<RoomDatabase.Builder<RepOrRunDatabase>> { error("no se construye en verify()") }
            single<DataStore<*>> { error("no se construye en verify()") }
        }

        module { includes(appModules(platformTypes)) }.verify(
            // Parámetros que una definición construye a mano en su lambda, sin pedirlos al grafo.
            extraTypes = listOf(Context::class, CoroutineDispatcher::class, Function0::class),
        )
    }
}
