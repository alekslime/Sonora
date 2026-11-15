package com.sonora.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureConfigTest {

    @Test
    fun storeUpdatesAndClampsHold() {
        GestureConfigStore.setEnabled(false)
        assertEquals(false, GestureConfigStore.config.enabled)
        GestureConfigStore.setHoldDurationMs(50L)
        assertEquals(GestureConfig.MIN_HOLD_MS, GestureConfigStore.config.holdDurationMs)
        GestureConfigStore.setHoldDurationMs(99_000L)
        assertEquals(GestureConfig.MAX_HOLD_MS, GestureConfigStore.config.holdDurationMs)
        GestureConfigStore.setHaptics(false)
        assertEquals(false, GestureConfigStore.config.haptics)
        GestureConfigStore.setReducedMotion(true)
        assertEquals(true, GestureConfigStore.config.reducedMotion)
        // Restore defaults.
        GestureConfigStore.setEnabled(true)
        GestureConfigStore.setHoldDurationMs(600L)
        GestureConfigStore.setHaptics(true)
        GestureConfigStore.setReducedMotion(false)
        assertEquals(GestureConfig(), GestureConfigStore.config)
    }
}
