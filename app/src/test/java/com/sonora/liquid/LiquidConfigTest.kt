package com.sonora.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidConfigTest {

    @Test
    fun sixDistinctPresets() {
        assertEquals(6, LiquidPreset.entries.size)
        val signatures = LiquidPreset.entries.map { Triple(it.deep, it.body, it.accent) }
        assertEquals(6, signatures.toSet().size)
    }

    @Test
    fun storeUpdatesAndClamps() {
        LiquidConfigStore.setPreset(LiquidPreset.AURORA)
        assertEquals(LiquidPreset.AURORA, LiquidConfigStore.config.preset)
        LiquidConfigStore.setIntensity(9f)
        assertEquals(1f, LiquidConfigStore.config.intensity, 0.001f)
        LiquidConfigStore.setIntensity(-2f)
        assertEquals(0f, LiquidConfigStore.config.intensity, 0.001f)
        LiquidConfigStore.setQuality(LiquidQuality.LOW)
        assertEquals(LiquidQuality.LOW, LiquidConfigStore.config.quality)
        // Restore defaults for other tests and the running app.
        LiquidConfigStore.setPreset(LiquidPreset.OBSIDIAN)
        LiquidConfigStore.setIntensity(0.8f)
        LiquidConfigStore.setQuality(LiquidQuality.HIGH)
        assertEquals(LiquidConfig(), LiquidConfigStore.config)
    }

    @Test
    fun defaultsAreSane() {
        val config = LiquidConfig()
        assertEquals(LiquidPreset.OBSIDIAN, config.preset)
        assertTrue(config.intensity in 0f..1f)
        assertEquals(LiquidQuality.HIGH, config.quality)
    }
}
