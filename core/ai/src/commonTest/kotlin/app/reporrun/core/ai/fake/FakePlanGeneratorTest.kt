package app.reporrun.core.ai.fake

import app.reporrun.core.ai.contract.ContractJson
import app.reporrun.core.ai.contract.GeneratePlanRequest
import app.reporrun.core.domain.AppDispatchers
import app.reporrun.core.domain.port.AiError
import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.port.PlanGenerationResult
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.PlanExercise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FakePlanGeneratorTest {
    // ------------------------------------------------------------ inventario

    @Test
    fun returnsTheExamplePlanForTheExampleGym() = runTest {
        val plan = generate(generationRequest())
        val example = FakeExamples.generateResponse.plan

        assertEquals(
            example.days.map { day -> day.exercises.map { Triple(it.code, it.name, it.equipmentId) } },
            plan.days.map { day -> day.exercises.map { Triple(it.code.value, it.name, it.equipmentId.value) } },
        )
        assertEquals(example.days.map { it.warmup }, plan.days.map { it.warmup })
        assertEquals(
            example.days.map { day -> day.exercises.map { exercise -> exercise.alternatives.map { it.equipmentId } } },
            plan.days.map { day ->
                day.exercises.map { exercise -> exercise.alternatives.map { it.equipmentId.value } }
            },
        )
    }

    @Test
    fun usesTheIdsOfTheInventoryWhenItsMachinesComeFromTheCatalog() = runTest {
        val request = generationRequest(equipment = catalogInventory())

        val plan = generate(request)

        assertValidFor(request, plan)
        assertEquals(
            FakeExamples.generateResponse.plan.days.map { day -> day.exercises.map { it.name } },
            plan.days.map { day -> day.exercises.map { it.name } },
        )
    }

    @Test
    fun neverNamesEquipmentOutsideAPartialInventory() = runTest {
        val request = generationRequest(
            equipment = catalogInventory().filterNot {
                it.id.value in
                    setOf("eq_a1", "eq_a9")
            },
        )

        val plan = generate(request)

        assertValidFor(request, plan)
    }

    @Test
    fun neverNamesEquipmentOutsideAnInventoryUnlikeTheExample() = runTest {
        val request = generationRequest(
            equipment = listOf(
                equipment("eq_b1", "squat_rack", "Rack", EquipmentCategory.FREE_WEIGHT, LoadType.BARBELL),
                equipment("eq_b2", "smith_machine", "Multipower", EquipmentCategory.SMITH, LoadType.SMITH),
                equipment("eq_b3", name = "Máquina casera"),
                equipment("eq_b4", "stationary_bike", "Bici", EquipmentCategory.CARDIO, LoadType.CARDIO),
            ),
        )

        val plan = generate(request)

        assertValidFor(request, plan)
        assertTrue(plan.days.isNotEmpty())
    }

    @Test
    fun movesAnExerciseToAnotherMachineWithItsNameAndLoadConvention() = runTest {
        val pecDeck = equipment("eq_c1", "pec_deck", "Pec Deck")
        val request = generationRequest(equipment = catalogInventory().filterNot { it.id.value == "eq_a1" } + pecDeck)

        val first = generate(request).days.first().exercises.first()

        assertEquals(pecDeck.id, first.equipmentId)
        assertEquals("Pec Deck", first.name)
        assertContains(first.trial.text, "selector")
        // Los kilos del ejemplo eran para otra máquina.
        assertFalse(first.trial.text.contains("kg"))
    }

    @Test
    fun dropsATimedExerciseWhenNoMachineIsLoadedTheSameWay() = runTest {
        val request = generationRequest(equipment = catalogInventory().filterNot { it.loadType == LoadType.BODYWEIGHT })

        val plan = generate(request)

        assertTrue(plan.exercises().none { it.dose is ExerciseDose.Seconds })
        assertValidFor(request, plan)
    }

    @Test
    fun keepsOnlyRepsExercisesOnMachinesLoadedAnotherWay() = runTest {
        val barbells = (1..12).map {
            equipment("eq_e$it", category = EquipmentCategory.FREE_WEIGHT, loadType = LoadType.BARBELL)
        }
        val request = generationRequest(equipment = barbells)

        val plan = generate(request)

        assertTrue(plan.exercises().all { it.dose is ExerciseDose.Reps })
        assertTrue(plan.exercises().all { "barra" in it.trial.text })
        assertValidFor(request, plan)
    }

    @Test
    fun failsWithInvalidInputWhenTheInventoryIsOnlyCardio() = runTest {
        val bike = equipment("eq_d1", "stationary_bike", "Bici", EquipmentCategory.CARDIO, LoadType.CARDIO)

        val result = generator().generate(generationRequest(equipment = listOf(bike)))

        assertIs<AiError.InvalidInput>(assertIs<PlanGenerationResult.Failure>(result).error)
    }

    // ------------------------------------------------------------ días y cardio

    @Test
    fun keepsSessionsOnAvailableDaysWithMatchingCodes() = runTest {
        val request = generationRequest(
            preferences = preferences(availableDays = setOf(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY)),
        )

        val plan = generate(request)

        assertEquals(listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), plan.days.map { it.dayOfWeek })
        assertEquals(listOf("M01", "J01"), plan.days.map { it.exercises.first().code.value })
        assertValidFor(request, plan)
    }

    @Test
    fun prefersTheDaysOfTheExampleWhenTheyAreAvailable() = runTest {
        val request = generationRequest(preferences = preferences(availableDays = DayOfWeek.entries.toSet()))

        val plan = generate(request)

        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY), plan.days.map { it.dayOfWeek })
    }

    @Test
    fun leavesCardioOutWhenNoModeWasChosen() = runTest {
        val plan = generate(generationRequest(preferences = preferences(cardioModes = emptyList())))

        assertTrue(plan.days.all { it.cardio == null })
    }

    @Test
    fun usesTheChosenCardioModeWithoutTheTreadmillSettings() = runTest {
        val plan =
            generate(generationRequest(preferences = preferences(cardioModes = listOf("Elíptica", "Caminadora"))))

        val cardio = plan.days.mapNotNull { it.cardio }

        assertEquals(plan.days.size, cardio.size)
        assertTrue(cardio.all { it.mode == "Elíptica" && it.speed == null && it.incline == null })
    }

    // ------------------------------------------------------------ lo que firma

    @Test
    fun signsThePlanWithTheClockAndTheRequestItWouldHaveSent() = runTest {
        val request = generationRequest(equipment = catalogInventory())

        val plan = generate(request)
        val sent = ContractJson.decodeFromString(GeneratePlanRequest.serializer(), plan.inputSnapshotJson)

        assertEquals(NOW, plan.generatedAt)
        assertEquals(FAKE_MODEL, plan.model)
        assertEquals(FakeExamples.generateResponse.promptVersion, sent.promptVersion)
        assertEquals(request.equipment.map { it.id.value }, sent.gym.equipment.map { it.id })
    }

    @Test
    fun givesEveryDayAndExerciseItsOwnId() = runTest {
        val plan = generate(generationRequest())
        val ids = plan.days.map { it.id.value } + plan.exercises().map { it.id.value }

        assertEquals(ids.size, ids.toSet().size)
    }

    // ------------------------------------------------------------ espera y fallo

    @Test
    fun waitsTheConfiguredLatency() = runTest {
        generator(FakeAiConfig(latency = 4.seconds)).generate(generationRequest())

        assertEquals(4.seconds.inWholeMilliseconds, currentTime)
    }

    @Test
    fun returnsTheConfiguredErrorAfterTheLatency() = runTest {
        val config = FakeAiConfig(latency = 2.seconds, failure = AiError.RateLimited(retryAfterSeconds = 60))

        val result = generator(config).generate(generationRequest())

        assertEquals(PlanGenerationResult.Failure(AiError.RateLimited(retryAfterSeconds = 60)), result)
        assertEquals(2.seconds.inWholeMilliseconds, currentTime)
    }

    @Test
    fun cancellingDuringTheWaitLeavesNoResult() = runTest {
        val pending = async { generator(FakeAiConfig(latency = 4.seconds)).generate(generationRequest()) }
        advanceTimeBy(1.seconds)

        pending.cancel()

        assertFailsWith<CancellationException> { pending.await() }
    }

    @Test
    fun rejectsNegativeLatency() {
        assertFailsWith<IllegalArgumentException> { FakeAiConfig(latency = (-1).seconds) }
    }

    // ------------------------------------------------------------ ayuda

    private fun TestScope.generator(config: FakeAiConfig = FakeAiConfig()) = FakePlanGenerator(
        config = config,
        ids = SequentialIds(),
        clock = FixedClock(),
        dispatchers = AppDispatchers(StandardTestDispatcher(testScheduler)),
    )

    private suspend fun TestScope.generate(request: PlanGenerationRequest): GeneratedPlan =
        assertIs<PlanGenerationResult.Success>(generator().generate(request)).plan

    private fun GeneratedPlan.exercises(): List<PlanExercise> = days.flatMap { it.exercises }

    /**
     * Las reglas de `PlanValidator` (B-06) que ya se pueden escribir: aparatos del inventario
     * también en las alternativas, días disponibles y sin repetir, códigos únicos con la letra
     * de su día y en orden. Cuando exista el validador, este test pasa a llamarlo.
     */
    private fun assertValidFor(request: PlanGenerationRequest, plan: GeneratedPlan) {
        val inventory = request.equipment.map { it.id }.toSet()
        val used = plan.exercises().flatMap { exercise ->
            exercise.alternatives.map { it.equipmentId } +
                exercise.equipmentId
        }
        val codes = plan.exercises().map { it.code.value }

        assertEquals(emptyList(), used.filterNot { it in inventory }, "aparatos fuera del inventario")
        assertTrue(plan.days.all { it.dayOfWeek in request.preferences.availableDays }, "día no disponible")
        assertEquals(plan.days.size, plan.days.map { it.dayOfWeek }.toSet().size, "día repetido")
        assertEquals(codes.size, codes.toSet().size, "códigos repetidos")
        assertTrue(plan.days.none { it.exercises.isEmpty() }, "día sin ejercicios")
        plan.days.forEach { day ->
            assertEquals((1..day.exercises.size).toList(), day.exercises.map { it.order }, "orden con huecos")
            assertEquals(1, day.exercises.map { it.code.value.first() }.toSet().size, "letras distintas en un día")
            assertNull(day.cardio?.mode?.takeIf { it !in request.preferences.cardioModes }, "cardio no elegido")
        }
    }
}
