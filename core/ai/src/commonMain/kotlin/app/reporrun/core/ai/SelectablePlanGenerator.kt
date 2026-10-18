package app.reporrun.core.ai

import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.domain.port.PlanGenerator
import app.reporrun.core.model.AiBackend

/**
 * De dónde sale el plan **ahora**. La da la raíz de composición, que es quien sabe si la
 * build es de depuración: en una que no lo es, siempre [AiBackend.PROXY].
 */
fun interface AiBackendSelection {
    suspend fun current(): AiBackend
}

/**
 * El único [PlanGenerator] del grafo: en cada llamada pregunta a [selection] y delega en
 * [fake] o en [proxy]. Se pregunta en cada llamada para que un cambio en Ajustes valga para la
 * siguiente generación sin reiniciar.
 */
class SelectablePlanGenerator(
    private val selection: AiBackendSelection,
    private val fake: PlanGenerator,
    private val proxy: PlanGenerator,
) : PlanGenerator {
    override suspend fun generate(request: PlanGenerationRequest): PlanGenerationResult = when (selection.current()) {
        AiBackend.FAKE -> fake.generate(request)
        AiBackend.PROXY -> proxy.generate(request)
    }
}
