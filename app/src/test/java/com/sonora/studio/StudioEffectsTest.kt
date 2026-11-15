package com.sonora.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class StudioEffectsTest {

    private val deviceCenters = listOf(60, 230, 910, 3600, 14000)

    @Test
    fun flatMapsToZeroMillibels() {
        val levels = StudioEffects.mapBandsToMillibels(
            List(10) { 0f }, deviceCenters, (-1500).toShort(), 1500.toShort(),
        )
        assertEquals(5, levels.size)
        assertEquals(listOf(0, 0, 0, 0, 0), levels.map { it.toInt() })
    }

    @Test
    fun exactCenterMatchesBand() {
        val bands = listOf(0f, 0f, 0f, 0f, 0f, 5f, 0f, 0f, 0f, 0f)
        val levels = StudioEffects.mapBandsToMillibels(
            bands, listOf(1000), (-1500).toShort(), 1500.toShort(),
        )
        assertEquals(500, levels.single().toInt())
    }

    @Test
    fun interpolatesLogMidpoint() {
        // 500 Hz at +12 dB, 1000 Hz at 0 dB: the log midpoint (~707 Hz)
        // reads ~+6 dB.
        val bands = listOf(0f, 0f, 0f, 0f, 12f, 0f, 0f, 0f, 0f, 0f)
        val levels = StudioEffects.mapBandsToMillibels(
            bands, listOf(707), (-1500).toShort(), 1500.toShort(),
        )
        val db = levels.single().toInt() / 100.0
        assertEquals(6.0, db, 0.05)
    }

    @Test
    fun clampsToDeviceRange() {
        val hot = StudioEffects.mapBandsToMillibels(
            List(10) { 12f }, deviceCenters, (-500).toShort(), 500.toShort(),
        )
        assertEquals(List(5) { 500 }, hot.map { it.toInt() })
        val cold = StudioEffects.mapBandsToMillibels(
            List(10) { -12f }, deviceCenters, (-500).toShort(), 500.toShort(),
        )
        assertEquals(List(5) { -500 }, cold.map { it.toInt() })
    }

    @Test
    fun outOfRangeCentersClampToEnds() {
        val bands = listOf(-4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 7f)
        val levels = StudioEffects.mapBandsToMillibels(
            bands, listOf(20, 20000), (-1500).toShort(), 1500.toShort(),
        )
        assertEquals(-400, levels[0].toInt())
        assertEquals(700, levels[1].toInt())
    }

    @Test(expected = IllegalArgumentException::class)
    fun mappingRejectsWrongBandCount() {
        StudioEffects.mapBandsToMillibels(
            listOf(0f, 0f), deviceCenters, (-1500).toShort(), 1500.toShort(),
        )
    }

    @Test
    fun preampToGain() {
        assertEquals(1f, StudioEffects.preampToGain(0f), 0.001f)
        assertEquals(1.995f, StudioEffects.preampToGain(6f), 0.001f)
        assertEquals(0.501f, StudioEffects.preampToGain(-6f), 0.001f)
        assertEquals(3.981f, StudioEffects.preampToGain(12f), 0.001f)
        assertEquals(0.251f, StudioEffects.preampToGain(-12f), 0.001f)
    }

    @Test
    fun preampGainClamps() {
        assertEquals(4f, StudioEffects.preampToGain(100f), 0.001f)
        assertEquals(0f, StudioEffects.preampToGain(-100f), 0.001f)
    }
}
