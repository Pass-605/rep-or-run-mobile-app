package app.reporrun.core.ai.di

import app.reporrun.core.ai.fake.FakeAiConfig
import app.reporrun.core.ai.fake.FakePlanGenerator
import app.reporrun.core.ai.fake.FakePlanReviewer
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.port.PlanReviewer
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

private val FAKE_LATENCY = 3.seconds

/**
 * Los ports de IA, registrados solo por su interfaz de `:core:domain`. En el MVP detrás están
 * los fakes y no hay entre qué elegir: el adaptador del proxy y el conmutador llegan con A-13.
 *
 * [FakeAiConfig] es una definición aparte para que la espera y el modo error se cambien aquí,
 * o se sustituyan en un test, sin tocar los fakes. El `Clock`, los dispatchers y el
 * `IdGenerator` que piden por constructor los ponen `appModule` y `domainModule`.
 */
val aiModule: Module = module {
    // Lo bastante para que la pantalla de espera se vea; para enseñar un error, `failure`.
    single { FakeAiConfig(latency = FAKE_LATENCY) }

    single<PlanGenerator> { FakePlanGenerator(get(), get(), get(), get()) }
    single<PlanReviewer> { FakePlanReviewer(get(), get(), get()) }
}
