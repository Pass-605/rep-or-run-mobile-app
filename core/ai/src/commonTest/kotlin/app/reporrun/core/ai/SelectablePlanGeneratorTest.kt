package app.reporrun.core.ai

import app.reporrun.core.ai.fake.generationRequest
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.model.AiBackend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectablePlanGeneratorTest {
    private val fakeResult = PlanGenerationResult.Failure(AiError.InvalidInput(listOf("fake")))
    private val proxyResult = PlanGenerationResult.Failure(AiError.Upstream)
    private val fake = PlanGenerator { fakeResult }
    private val proxy = PlanGenerator { proxyResult }

    @Test
    fun usesTheFakeWhenTheExamplesAreSelected() = runTest {
        val generator = SelectablePlanGenerator({ AiBackend.FAKE }, fake, proxy)

        assertEquals(fakeResult, generator.generate(generationRequest()))
    }

    @Test
    fun usesTheProxyWhenItIsSelected() = runTest {
        val generator = SelectablePlanGenerator({ AiBackend.PROXY }, fake, proxy)

        assertEquals(proxyResult, generator.generate(generationRequest()))
    }

    @Test
    fun asksForTheSelectionOnEveryCall() = runTest {
        var backend = AiBackend.FAKE
        val generator = SelectablePlanGenerator({ backend }, fake, proxy)

        val first = generator.generate(generationRequest())
        backend = AiBackend.PROXY
        val second = generator.generate(generationRequest())

        assertEquals(listOf(fakeResult, proxyResult), listOf(first, second))
    }
}
