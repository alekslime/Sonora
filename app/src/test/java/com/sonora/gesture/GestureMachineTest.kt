package com.sonora.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureMachineTest {

    private fun machine(holdMs: Long = 600L) = GestureMachine(
        holdDurationMs = holdMs,
        touchSlopPx = 24f,
        swipeThresholdPx = 120f,
    )

    private fun downsAt(machine: GestureMachine, t: Long, context: GestureContext = GestureContext.OTHER) {
        machine.handle(GestureEvent.Down(1, 100f, 500f, t), context, false)
        machine.handle(GestureEvent.Down(2, 200f, 500f, t), context, false)
        machine.handle(GestureEvent.Down(3, 300f, 500f, t), context, false)
    }

    @Test
    fun holdOpensLiquidAfterDuration() {
        val machine = machine()
        downsAt(machine, 1000L)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(1599L), GestureContext.OTHER, false))
        assertEquals(GestureOutput.OpenLiquid, machine.handle(GestureEvent.Tick(1600L), GestureContext.OTHER, false))
    }

    @Test
    fun holdNeedsThreePointers() {
        val machine = machine()
        machine.handle(GestureEvent.Down(1, 100f, 500f, 1000L), GestureContext.OTHER, false)
        machine.handle(GestureEvent.Down(2, 200f, 500f, 1000L), GestureContext.OTHER, false)
        assertNull(machine.nextDeadline)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(5000L), GestureContext.OTHER, false))
    }

    @Test
    fun movementCancelsHold() {
        val machine = machine()
        downsAt(machine, 1000L)
        machine.handle(GestureEvent.Move(2, 200f, 560f, 1200L), GestureContext.OTHER, false)
        assertNull(machine.nextDeadline)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(5000L), GestureContext.OTHER, false))
    }

    @Test
    fun smallJitterDoesNotCancelHold() {
        val machine = machine()
        downsAt(machine, 1000L)
        machine.handle(GestureEvent.Move(2, 205f, 510f, 1200L), GestureContext.OTHER, false)
        assertEquals(GestureOutput.OpenLiquid, machine.handle(GestureEvent.Tick(1600L), GestureContext.OTHER, false))
    }

    @Test
    fun liftCancelsHold() {
        val machine = machine()
        downsAt(machine, 1000L)
        machine.handle(GestureEvent.Up(3, 1200L), GestureContext.OTHER, false)
        assertNull(machine.nextDeadline)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(5000L), GestureContext.OTHER, false))
    }

    @Test
    fun fourthFingerFailsSessionUntilRelease() {
        val machine = machine()
        downsAt(machine, 1000L)
        machine.handle(GestureEvent.Down(4, 400f, 500f, 1100L), GestureContext.OTHER, false)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(5000L), GestureContext.OTHER, false))
        // Release all, then a fresh hold works again.
        machine.handle(GestureEvent.Up(1, 1200L), GestureContext.OTHER, false)
        machine.handle(GestureEvent.Up(2, 1200L), GestureContext.OTHER, false)
        machine.handle(GestureEvent.Up(3, 1200L), GestureContext.OTHER, false)
        machine.handle(GestureEvent.Up(4, 1200L), GestureContext.OTHER, false)
        downsAt(machine, 2000L)
        assertEquals(GestureOutput.OpenLiquid, machine.handle(GestureEvent.Tick(2600L), GestureContext.OTHER, false))
    }

    @Test
    fun swipeUpOpensStudioOnlyFromNowPlaying() {
        val machine = machine()
        downsAt(machine, 1000L, GestureContext.NOW_PLAYING)
        var output: GestureOutput = GestureOutput.None
        listOf(1L to 100f, 2L to 200f, 3L to 300f).forEach { (id, x) ->
            output = machine.handle(GestureEvent.Move(id, x, 350f, 1100L), GestureContext.NOW_PLAYING, false)
        }
        assertEquals(GestureOutput.OpenStudio, output)
    }

    @Test
    fun swipeUpIgnoredElsewhere() {
        val machine = machine()
        downsAt(machine, 1000L, GestureContext.OTHER)
        var output: GestureOutput = GestureOutput.None
        listOf(1L to 100f, 2L to 200f, 3L to 300f).forEach { (id, x) ->
            output = machine.handle(GestureEvent.Move(id, x, 350f, 1100L), GestureContext.OTHER, false)
        }
        assertEquals(GestureOutput.None, output)
    }

    @Test
    fun swipeDownClosesStudioOnlyFromStudio() {
        val machine = machine()
        downsAt(machine, 1000L, GestureContext.STUDIO)
        var output: GestureOutput = GestureOutput.None
        listOf(1L to 100f, 2L to 200f, 3L to 300f).forEach { (id, x) ->
            output = machine.handle(GestureEvent.Move(id, x, 650f, 1100L), GestureContext.STUDIO, false)
        }
        assertEquals(GestureOutput.CloseStudio, output)
    }

    @Test
    fun incoherentSwipeDoesNotFire() {
        val machine = machine()
        downsAt(machine, 1000L, GestureContext.NOW_PLAYING)
        machine.handle(GestureEvent.Move(1, 100f, 350f, 1100L), GestureContext.NOW_PLAYING, false)
        machine.handle(GestureEvent.Move(2, 200f, 520f, 1100L), GestureContext.NOW_PLAYING, false)
        val output = machine.handle(GestureEvent.Move(3, 300f, 350f, 1100L), GestureContext.NOW_PLAYING, false)
        assertEquals(GestureOutput.None, output)
    }

    @Test
    fun singleTriggerPerTouchSession() {
        val machine = machine()
        downsAt(machine, 1000L, GestureContext.NOW_PLAYING)
        assertEquals(GestureOutput.OpenLiquid, machine.handle(GestureEvent.Tick(1600L), GestureContext.NOW_PLAYING, false))
        // A later swipe in the same touch must not fire again.
        var output: GestureOutput = GestureOutput.None
        listOf(1L to 100f, 2L to 200f, 3L to 300f).forEach { (id, x) ->
            output = machine.handle(GestureEvent.Move(id, x, 350f, 1700L), GestureContext.NOW_PLAYING, false)
        }
        assertEquals(GestureOutput.None, output)
    }

    @Test
    fun cancelResetsMachine() {
        val machine = machine()
        downsAt(machine, 1000L)
        machine.handle(GestureEvent.Cancel(1100L), GestureContext.OTHER, false)
        assertEquals(0, machine.pointerCount)
        assertNull(machine.nextDeadline)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(5000L), GestureContext.OTHER, false))
    }

    @Test
    fun noOutputWhileLiquidOpen() {
        val machine = machine()
        downsAt(machine, 1000L)
        assertEquals(GestureOutput.None, machine.handle(GestureEvent.Tick(1600L), GestureContext.OTHER, true))
    }
}
