package app.reporrun.core.ai.fake

import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.ai.contract.GeneratePlanRequest
import app.reporrun.core.ai.contract.GeneratePlanResponse
import app.reporrun.core.ai.contract.ReviewPlanResponse
import app.reporrun.core.domain.port.AiError
import kotlin.time.Duration

/**
 * Cómo se comportan los fakes. Llega por constructor desde el grafo (`aiModule`): ni la espera
 * ni el fallo son constantes dentro de [FakePlanGenerator] o [FakePlanReviewer].
 *
 * @property latency cuánto tarda cada llamada, para que la pantalla de espera tenga algo que
 *   enseñar. Es un `delay`: cancelable, y en un test corre en tiempo virtual.
 * @property failure si no es `null`, toda llamada espera [latency] y termina en este error en
 *   vez de en un resultado.
 */
data class FakeAiConfig(val latency: Duration = Duration.ZERO, val failure: AiError? = null) {
    init {
        require(!latency.isNegative()) { "latency negativa" }
    }
}

/** El `model` con el que los fakes firman lo que devuelven: un plan guardado dice de dónde salió. */
internal const val FAKE_MODEL: String = "fake-ejemplos-contrato-v1"

/** El `locale` que los fakes ponen en la petición. La app solo existe en español. */
internal const val FAKE_LOCALE: String = "es"

/**
 * Los ejemplos de `contract/v1/examples/` que usan los fakes, ya leídos. Son sintéticos (README
 * del contrato, «Datos sintéticos»). El texto sale de [ContractExampleSources], que se genera
 * desde la copia del contrato del repo.
 */
internal object FakeExamples {
    val generateRequest: GeneratePlanRequest by lazy {
        ContractJson.decodeFromString(GeneratePlanRequest.serializer(), ContractExampleSources.generateRequestSmallGym)
    }

    val generateResponse: GeneratePlanResponse by lazy {
        ContractJson.decodeFromString(
            GeneratePlanResponse.serializer(),
            ContractExampleSources.generateResponseSmallGym,
        )
    }

    val reviewSufficient: ReviewPlanResponse by lazy {
        ContractJson.decodeFromString(ReviewPlanResponse.serializer(), ContractExampleSources.reviewResponseSufficient)
    }

    val reviewInsufficient: ReviewPlanResponse by lazy {
        ContractJson.decodeFromString(
            ReviewPlanResponse.serializer(),
            ContractExampleSources.reviewResponseInsufficient,
        )
    }
}
