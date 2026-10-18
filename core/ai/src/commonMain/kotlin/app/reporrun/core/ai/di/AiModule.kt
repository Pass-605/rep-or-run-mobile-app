package app.reporrun.core.ai.di

import app.reporrun.core.ai.SelectablePlanGenerator
import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.ai.fake.ContractSampleGymSource
import app.reporrun.core.ai.fake.FakeAiConfig
import app.reporrun.core.ai.fake.FakePlanGenerator
import app.reporrun.core.ai.fake.FakePlanReviewer
import app.reporrun.core.ai.proxy.ProxyPlanGenerator
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.domain.port.PlanReviewer
import app.reporrun.core.domain.port.SampleGymSource
import app.reporrun.core.network.createHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

private val FAKE_LATENCY = 3.seconds

/**
 * Los ports de IA, registrados solo por su interfaz de `:core:domain`.
 *
 * - `PlanGenerator` es un [SelectablePlanGenerator] entre el fake y [ProxyPlanGenerator]. Cuál
 *   usa lo decide la `AiBackendSelection` que pone la raíz de composición: solo ella sabe si la
 *   build es de depuración.
 * - `PlanReviewer` sigue siendo el fake: el revisor por HTTP es de la línea E.
 * - El `HttpClient` se crea con [ContractJson] y la `NetworkConfig` que pone la raíz de
 *   composición (versión de la app e `installId`).
 *
 * [FakeAiConfig] es una definición aparte para que la espera y el modo error se cambien aquí,
 * o se sustituyan en un test, sin tocar los fakes. El `Clock`, los dispatchers y el
 * `IdGenerator` que piden por constructor los ponen `appModule` y `domainModule`.
 */
val aiModule: Module = module {
    // Lo bastante para que la pantalla de espera se vea; para enseñar un error, `failure`.
    single { FakeAiConfig(latency = FAKE_LATENCY) }

    single<HttpClient> { createHttpClient(get(), json = ContractJson) }
    single { FakePlanGenerator(get(), get(), get(), get()) }
    single { ProxyPlanGenerator(get(), get(), get()) }
    single<PlanGenerator> {
        SelectablePlanGenerator(selection = get(), fake = get<FakePlanGenerator>(), proxy = get<ProxyPlanGenerator>())
    }
    single<PlanReviewer> { FakePlanReviewer(get(), get(), get()) }
    single<SampleGymSource> { ContractSampleGymSource }
}
