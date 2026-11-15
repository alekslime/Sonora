package com.sonora.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureConfigTest {

    @Test
    fun defaultsAreSensible() {
        val config = GestureConfig()
        assertTrue(config.enabled)
        assertEquals(700L, config.holdDurationMs)
        assertTrue(config.haptics)
        assertFalse(config.reducedMotion)
    }

    @Test
    fun storeClampsHoldDuration() {
        GestureConfigStore.setHoldDuration(50L)
        assertEquals(GestureConfig.MIN_HOLD_MS, GestureConfigStore.config.holdDurationMs)
        GestureConfigStore.setHoldDuration(99_999L)
        assertEquals(GestureConfig.MAX_HOLD_MS, GestureConfigStore.config.holdDurationMs)
        GestureConfigStore.setHoldDuration(700L)
        GestureConfigStore.setEnabled(true)
        GestureConfigStore.setHaptics(true)
        GestureConfigStore.setReducedMotion(false)
        assertEquals(GestureConfig(), GestureConfigStore.config)
    }
}
