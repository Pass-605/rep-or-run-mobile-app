package app.reporrun.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals

class RepOrRunStepperTest {
    @Test
    fun stepsBeforeTheCurrentOneAreDone() {
        assertEquals(StepState.Done, stepState(index = 0, currentStep = 1))
    }

    @Test
    fun theCurrentStepIsCurrent() {
        assertEquals(StepState.Current, stepState(index = 1, currentStep = 1))
    }

    @Test
    fun stepsAfterTheCurrentOneArePending() {
        assertEquals(StepState.Pending, stepState(index = 2, currentStep = 1))
    }

    @Test
    fun everyStepIsDoneWhenCurrentIsPastTheLast() {
        assertEquals(List(3) { StepState.Done }, List(3) { stepState(index = it, currentStep = 3) })
    }

    @Test
    fun firstAndLastNodesSitInsetFromTheEdges() {
        assertEquals(24f, stepCenter(index = 0, count = 3, width = 336f, inset = 24f))
        assertEquals(312f, stepCenter(index = 2, count = 3, width = 336f, inset = 24f))
    }

    @Test
    fun middleNodeSitsInTheCenter() {
        assertEquals(168f, stepCenter(index = 1, count = 3, width = 336f, inset = 24f))
    }
}
