package app.reporrun.core.data.repository

import app.reporrun.core.data.GYM_ID
import app.reporrun.core.data.T0
import app.reporrun.core.data.mapper.toRows
import app.reporrun.core.data.plan
import app.reporrun.core.model.DayStatus
import app.reporrun.core.model.ExportRecord
import app.reporrun.core.model.ExportRecordId
import app.reporrun.core.model.PlanChange
import app.reporrun.core.model.PlanChangeId
import app.reporrun.core.model.PlanId
import app.reporrun.core.model.PlanStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class PlanRepositoryImplTest {
    private val db = inMemoryDatabase()
    private val repository = PlanRepositoryImpl(db.planDao(), db.planHistoryDao())

    @AfterTest
    fun close() = db.close()

    @Test
    fun savesAndReadsBackTheWholePlan() = runTest {
        db.seedGymWithPlanEquipment()
        val plan = plan()

        repository.savePlan(plan)

        assertEquals(plan, repository.observePlan(plan.id).first())
    }

    @Test
    fun observesNullForAnUnknownPlan() = runTest {
        assertNull(repository.observePlan(PlanId("ROR-2026-01-01-v1")).first())
    }

    @Test
    fun activatingAPlanArchivesThePreviousOne() = runTest {
        db.seedGymWithPlanEquipment()
        val first = plan("ROR-2026-09-30-v1", status = PlanStatus.ACTIVE)
        val second = plan("ROR-2026-10-14-v2")
        repository.savePlan(first)
        repository.savePlan(second)

        repository.activatePlan(second.id)

        assertEquals(second.copy(status = PlanStatus.ACTIVE), repository.observeActivePlan(GYM_ID).first())
        assertEquals(PlanStatus.ARCHIVED, repository.observePlan(first.id).first()?.status)
    }

    @Test
    fun changesOnlyTheStatusOfADay() = runTest {
        db.seedGymWithPlanEquipment()
        val plan = plan()
        repository.savePlan(plan)
        val thursday = plan.days[1]

        repository.setDayStatus(thursday.id, DayStatus.APPROVED)

        val read = repository.observePlan(plan.id).first()
        assertEquals(DayStatus.APPROVED, read?.days?.get(1)?.status)
        assertEquals(plan.days[1].copy(status = DayStatus.APPROVED), read?.days?.get(1))
    }

    @Test
    fun listsHistoryAsSummariesNewestFirst() = runTest {
        db.seedGymWithPlanEquipment()
        repository.savePlan(plan("ROR-2026-09-30-v1"))
        repository.savePlan(plan("ROR-2026-10-14-v2").copy(createdAt = T0 + 14.days))

        val history = repository.observeHistory(GYM_ID).first()

        assertEquals(listOf(2, 1), history.map { it.version })
        assertEquals(listOf("Fuerza básica"), history.first().focus)
    }

    @Test
    fun listsThePlanIdsOfOneDay() = runTest {
        db.seedGymWithPlanEquipment()
        repository.savePlan(plan("ROR-2026-09-30-v1"))
        repository.savePlan(plan("ROR-2026-09-30-v2", status = PlanStatus.ARCHIVED))
        repository.savePlan(plan("ROR-2026-10-01-v1"))

        val ids = repository.planIdsOn(LocalDate(2026, 9, 30))

        assertEquals(setOf(PlanId("ROR-2026-09-30-v1"), PlanId("ROR-2026-09-30-v2")), ids.toSet())
        assertEquals(emptyList(), repository.planIdsOn(LocalDate(2026, 9, 29)))
    }

    @Test
    fun recordsVersionChangesAndExports() = runTest {
        db.seedGymWithPlanEquipment()
        val v1 = plan("ROR-2026-09-30-v1")
        val v2 = plan("ROR-2026-10-14-v2")
        repository.savePlan(v1)
        repository.savePlan(v2)
        val change = PlanChange(
            id = PlanChangeId("ch_1"),
            planId = v2.id,
            previousPlanId = v1.id,
            reviewId = null,
            date = LocalDate(2026, 10, 14),
            summary = "Sube series de espalda",
            reason = "Aprobado en la revisión",
            approvedAt = T0,
        )
        val export = ExportRecord(ExportRecordId("exp_1"), v2.id, T0, "plan.pdf", pages = 8)

        repository.saveChange(change)
        repository.saveExport(export)

        assertEquals(listOf(change), repository.observeChanges(GYM_ID).first())
        assertEquals(listOf(export), repository.observeExports(v2.id).first())
    }

    @Test
    fun failsLoudlyWithEveryErrorWhenAStoredPlanIsUnreadable() = runTest {
        db.seedGymWithPlanEquipment()
        val plan = plan()
        repository.savePlan(plan)
        // Filas dañadas a propósito, por debajo del repositorio: rango al revés y RIR en metros.
        db.planDao().deletePlan(plan.id.value)
        val rows = plan.toRows()
        val broken = rows.exercises.map { exercise ->
            when (exercise.code) {
                "L01" -> exercise.copy(dose = exercise.dose.copy(low = 12, high = 8))
                "J01" -> exercise.copy(dose = exercise.dose.copy(rirLow = 1, rirHigh = 2))
                else -> exercise
            }
        }
        db.planDao().insertPlan(rows.plan, rows.days, broken, rows.alternatives)

        val failure = assertFailsWith<IllegalStateException> { repository.observePlan(plan.id).first() }

        assertEquals(
            "Plan ROR-2026-09-30-v1 ilegible: " +
                "days[0].exercises[0].dose: rango inválido: 12-8 (1 <= low <= high); " +
                "days[1].exercises[0].dose: el RIR no aplica a METERS",
            failure.message,
        )
    }
}
