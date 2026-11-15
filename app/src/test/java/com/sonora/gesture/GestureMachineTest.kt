package com.sonora.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMachineTest {

    private val nowPlaying = GestureContext.NOW_PLAYING
    private val studio = GestureContext.STUDIO
    private val other = GestureContext.OTHER

    private fun arm(machine: GestureMachine = GestureMachine(), y: Float = 1000f): GestureMachine {
        val (next, action) = machine.reduce(GestureEvent.PointersChanged(3, y), nowPlaying)
        assertEquals(GestureAction.NONE, action)
        return next
    }

    @Test
    fun holdFiresOnceAfterTimeout() {
        val armed = arm()
        val (fired, action) = armed.reduce(GestureEvent.HoldTimeout, nowPlaying)
        assertEquals(GestureAction.OPEN_LIQUID, action)
        val (_, again) = fired.reduce(GestureEvent.HoldTimeout, nowPlaying)
        assertEquals(GestureAction.NONE, again)
    }

    @Test
    fun movementCancelsHold() {
        val armed = arm()
        val (moved, _) = armed.reduce(GestureEvent.MovedBeyondSlop, nowPlaying)
        val (_, action) = moved.reduce(GestureEvent.HoldTimeout, nowPlaying)
        assertEquals(GestureAction.NONE, action)
    }

    @Test
    fun fourthFingerCancelsHold() {
        val armed = arm()
        val (four, _) = armed.reduce(GestureEvent.PointersChanged(4, 1000f), nowPlaying)
        val (_, action) = four.reduce(GestureEvent.HoldTimeout, nowPlaying)
        assertEquals(GestureAction.NONE, action)
    }

    @Test
    fun fingerLiftCancelsHold() {
        val armed = arm()
        val (two, _) = armed.reduce(GestureEvent.PointersChanged(2, 1000f), nowPlaying)
        val (_, action) = two.reduce(GestureEvent.HoldTimeout, nowPlaying)
        assertEquals(GestureAction.NONE, action)
    }

    @Test
    fun swipeUpFromNowPlayingOpensStudio() {
        val armed = arm(y = 1200f)
        val (tracking, _) = armed.reduce(GestureEvent.MovedBeyondSlop, nowPlaying)
        val (_, action) = tracking.reduce(GestureEvent.PointersChanged(0, 900f), nowPlaying)
        assertEquals(GestureAction.OPEN_STUDIO, action)
    }

    @Test
    fun swipeDownFromStudioCloses() {
        var machine = GestureMachine()
        var action: GestureAction
        machine.reduce(GestureEvent.PointersChanged(3, 800f), studio).let { machine = it.first }
        machine.reduce(GestureEvent.MovedBeyondSlop, studio).let { machine = it.first }
        machine.reduce(GestureEvent.PointersChanged(0, 1100f), studio).let { action = it.second }
        assertEquals(GestureAction.CLOSE_STUDIO, action)
    }

    @Test
    fun swipesIgnoredInWrongContext() {
        val armed = arm(y = 1200f)
        val (tracking, _) = armed.reduce(GestureEvent.MovedBeyondSlop, other)
        val (_, upElsewhere) = tracking.reduce(GestureEvent.PointersChanged(0, 900f), other)
        assertEquals(GestureAction.NONE, upElsewhere)
        val downNowPlaying = arm(y = 800f)
            .reduce(GestureEvent.MovedBeyondSlop, nowPlaying).first
            .reduce(GestureEvent.PointersChanged(0, 1100f), nowPlaying).second
        assertEquals(GestureAction.NONE, downNowPlaying)
    }

    @Test
    fun smallDriftIsNoSwipe() {
        val armed = arm(y = 1000f)
        val (tracking, _) = armed.reduce(GestureEvent.MovedBeyondSlop, nowPlaying)
        val (_, action) = tracking.reduce(GestureEvent.PointersChanged(0, 950f), nowPlaying)
        assertEquals(GestureAction.NONE, action)
    }

    @Test
    fun firedHoldConsumesRelease() {
        val armed = arm(y = 1200f)
        val (fired, _) = armed.reduce(GestureEvent.HoldTimeout, nowPlaying)
        val (_, action) = fired.reduce(GestureEvent.PointersChanged(0, 700f), nowPlaying)
        assertEquals(GestureAction.NONE, action)
    }

    @Test
    fun cancelResets() {
        val armed = arm()
        val (reset, action) = armed.reduce(GestureEvent.Cancel, nowPlaying)
        assertEquals(GestureAction.NONE, action)
        assertEquals(GestureMachine(), reset)
    }
}
