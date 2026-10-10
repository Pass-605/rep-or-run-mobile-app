package app.reporrun.core.ai.mapper

import app.reporrun.core.ai.contract.ConfidenceDto
import app.reporrun.core.ai.contract.DataQualityDto
import app.reporrun.core.ai.contract.DayOfWeekDto
import app.reporrun.core.ai.contract.EquipmentCategoryDto
import app.reporrun.core.ai.contract.ExperienceDto
import app.reporrun.core.ai.contract.GeneratePlanResponse
import app.reporrun.core.ai.contract.GoalDto
import app.reporrun.core.ai.contract.LoadTypeDto
import app.reporrun.core.ai.contract.MetricDto
import app.reporrun.core.ai.contract.MuscleGroupDto
import app.reporrun.core.ai.contract.PlanDayDto
import app.reporrun.core.ai.contract.PlanExerciseDto
import app.reporrun.core.ai.contract.ProposalDto
import app.reporrun.core.ai.contract.ProposalTypeDto
import app.reporrun.core.ai.contract.ReviewPlanResponse
import app.reporrun.core.ai.contract.UnitDto
import app.reporrun.core.ai.fake.FakeExamples
import app.reporrun.core.ai.fake.SequentialIds
import app.reporrun.core.ai.fake.generationRequest
import app.reporrun.core.domain.port.GeneratedPlan
import app.reporrun.core.domain.port.GeneratedReview
import app.reporrun.core.model.Confidence
import app.reporrun.core.model.DataQuality
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.ExerciseDose
import app.reporrun.core.model.Experience
import app.reporrun.core.model.Goal
import app.reporrun.core.model.LoadType
import app.reporrun.core.model.LoadUnit
import app.reporrun.core.model.Metric
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ProposalType
import app.reporrun.core.model.RirRange
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MappersTest {
    // ------------------------------------------------------------ plan

    @Test
    fun mapsTheWholeExamplePlan() {
        val example = FakeExamples.generateResponse

        val plan = assertIs<Mapped.Valid<GeneratedPlan>>(example.toGeneratedPlan(SequentialIds(), SNAPSHOT)).value

        assertEquals(example.plan.days.map { it.exercises.size }, plan.days.map { it.exercises.size })
        assertEquals(example.plan.days.map { it.dayOfWeek.name }, plan.days.map { it.dayOfWeek.name })
        assertEquals(example.promptVersion, plan.promptVersion)
        assertEquals(SNAPSHOT, plan.inputSnapshotJson)
    }

    @Test
    fun mapsRepsWithRirAndSecondsWithout() {
        val monday = mapped(FakeExamples.generateResponse).days.first().exercises

        assertEquals(ExerciseDose.Reps(range = monday.first().dose.range, rir = RirRange(2, 2)), monday.first().dose)
        assertIs<ExerciseDose.Seconds>(monday.last().dose)
    }

    @Test
    fun mapsADayWithoutCardioMinutesAsNoCardio() {
        val withoutCardio = FakeExamples.generateResponse.editDay { it.copy(cardioMin = null, cardioMax = null) }

        assertEquals(null, mapped(withoutCardio).days.first().cardio)
    }

    @Test
    fun reportsEveryProblemAndNotOnlyTheFirst() {
        val broken = FakeExamples.generateResponse.editExercises { index, exercise ->
            when (index) {
                0 -> exercise.copy(low = 12, high = 6, rirHigh = 9)
                1 -> exercise.copy(equipmentId = "chest press", sets = 0)
                else -> exercise
            }
        }

        val errors = assertIs<Mapped.Invalid>(broken.toGeneratedPlan(SequentialIds(), SNAPSHOT)).errors

        assertEquals(4, errors.size)
        assertContains(errors[0], "plan.days[0].exercises[0].dose: rango inválido: 12-6")
        assertContains(errors[1], "plan.days[0].exercises[0].dose: RIR inválido: 2-9")
        assertContains(errors[2], "plan.days[0].exercises[1].equipmentId")
        assertContains(errors[3], "plan.days[0].exercises[1].sets")
    }

    @Test
    fun rejectsRirOnAnExerciseThatIsNotCountedInReps() {
        val broken = FakeExamples.generateResponse.editExercises { _, exercise ->
            if (exercise.metric == MetricDto.SECONDS) exercise.copy(rirLow = 2, rirHigh = 2) else exercise
        }

        val errors = assertIs<Mapped.Invalid>(broken.toGeneratedPlan(SequentialIds(), SNAPSHOT)).errors

        assertContains(errors.single(), "el RIR solo aplica a repeticiones")
    }

    @Test
    fun rejectsABlockLengthThatV1DoesNotAdmit() {
        val broken = FakeExamples.generateResponse.let { it.copy(plan = it.plan.copy(blockDays = 28)) }

        val errors = assertIs<Mapped.Invalid>(broken.toGeneratedPlan(SequentialIds(), SNAPSHOT)).errors

        assertContains(errors.single(), "plan.blockDays")
    }

    @Test
    fun rejectsADateThatIsNotAnInstant() {
        val broken = FakeExamples.generateResponse.copy(generatedAt = "ayer")

        val errors = assertIs<Mapped.Invalid>(broken.toGeneratedPlan(SequentialIds(), SNAPSHOT)).errors

        assertContains(errors.single(), "generatedAt")
    }

    // ------------------------------------------------------------ revisión

    @Test
    fun mapsBothExampleReviews() {
        val sufficient = assertIs<Mapped.Valid<GeneratedReview>>(FakeExamples.reviewSufficient.toGeneratedReview())
        val insufficient = assertIs<Mapped.Valid<GeneratedReview>>(FakeExamples.reviewInsufficient.toGeneratedReview())

        assertEquals(DataQuality.SUFFICIENT, sufficient.value.result.dataQuality)
        assertEquals(FakeExamples.reviewSufficient.review.proposals.size, sufficient.value.result.proposals.size)
        assertEquals(DataQuality.INSUFFICIENT, insufficient.value.result.dataQuality)
        assertTrue(insufficient.value.result.proposals.isEmpty())
    }

    @Test
    fun reportsEveryBrokenProposal() {
        val broken = FakeExamples.reviewSufficient.editProposals { index, proposal ->
            when (index) {
                0 -> proposal.copy(targetExerciseCode = null)
                1 -> proposal.copy(targetExerciseCode = "pendulum")
                else -> proposal
            }
        }

        val errors = assertIs<Mapped.Invalid>(broken.toGeneratedReview()).errors

        assertEquals(2, errors.size)
        assertContains(errors[0], "review.proposals[0]")
        assertContains(errors[1], "review.proposals[1]")
    }

    @Test
    fun rejectsProposalsWhenTheReviewSaysDataIsInsufficient() {
        val example = FakeExamples.reviewSufficient
        val broken = example.copy(review = example.review.copy(dataQuality = DataQualityDto.INSUFFICIENT))

        val errors = assertIs<Mapped.Invalid>(broken.toGeneratedReview()).errors

        assertContains(errors.single(), "con datos insuficientes no se proponen cambios")
    }

    // ------------------------------------------------------------ petición

    @Test
    fun writesTheRequestWithTheVersionsAndTheInventory() {
        val request = generationRequest()

        val dto = request.toDto(promptVersion = "plan-gen-v1", locale = "es")

        assertEquals("1", dto.schemaVersion)
        assertEquals(request.equipment.map { it.id.value }, dto.gym.equipment.map { it.id })
        assertEquals(
            listOf(DayOfWeekDto.MONDAY, DayOfWeekDto.WEDNESDAY, DayOfWeekDto.SATURDAY),
            dto.preferences.availableDays,
        )
        // Los conjuntos viajan en el orden del enum, no en el de inserción.
        assertEquals(listOf(MuscleGroupDto.CHEST, MuscleGroupDto.QUADS), dto.profile.priorityMuscles)
    }

    // Los mappers cruzan los enums con `valueOf(name)`: si uno del dominio y el del contrato
    // dejan de llamarse igual, esto falla aquí y no en ejecución.
    @Test
    fun everyDomainEnumHasTheNamesOfItsContractEnum() {
        assertEquals(DayOfWeekDto.entries.map { it.name }, DayOfWeek.entries.map { it.name })
        assertEquals(ExperienceDto.entries.map { it.name }, Experience.entries.map { it.name })
        assertEquals(GoalDto.entries.map { it.name }, Goal.entries.map { it.name })
        assertEquals(MuscleGroupDto.entries.map { it.name }, MuscleGroup.entries.map { it.name })
        assertEquals(EquipmentCategoryDto.entries.map { it.name }, EquipmentCategory.entries.map { it.name })
        assertEquals(LoadTypeDto.entries.map { it.name }, LoadType.entries.map { it.name })
        assertEquals(MetricDto.entries.map { it.name }, Metric.entries.map { it.name })
        assertEquals(UnitDto.entries.map { it.name }, LoadUnit.entries.map { it.name })
        assertEquals(DataQualityDto.entries.map { it.name }, DataQuality.entries.map { it.name })
        assertEquals(ProposalTypeDto.entries.map { it.name }, ProposalType.entries.map { it.name })
        assertEquals(ConfidenceDto.entries.map { it.name }, Confidence.entries.map { it.name })
    }

    // ------------------------------------------------------------ ayuda

    private fun mapped(response: GeneratePlanResponse): GeneratedPlan =
        assertIs<Mapped.Valid<GeneratedPlan>>(response.toGeneratedPlan(SequentialIds(), SNAPSHOT)).value

    /** Cambia el primer día del plan. */
    private fun GeneratePlanResponse.editDay(edit: (PlanDayDto) -> PlanDayDto) =
        copy(plan = plan.copy(days = listOf(edit(plan.days.first())) + plan.days.drop(1)))

    /** Cambia los ejercicios del primer día, por posición. */
    private fun GeneratePlanResponse.editExercises(edit: (Int, PlanExerciseDto) -> PlanExerciseDto) =
        editDay { day -> day.copy(exercises = day.exercises.mapIndexed(edit)) }

    private fun ReviewPlanResponse.editProposals(edit: (Int, ProposalDto) -> ProposalDto) =
        copy(review = review.copy(proposals = review.proposals.mapIndexed(edit)))

    private companion object {
        const val SNAPSHOT = """{"schemaVersion":"1"}"""
    }
}
