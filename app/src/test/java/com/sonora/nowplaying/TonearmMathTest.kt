package com.sonora.nowplaying

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class TonearmMathTest {

    @Test
    fun stylusAngleSweepsInward() {
        assertEquals(10f, stylusAngleDeg(0f), 0.001f)
        assertEquals(-18f, stylusAngleDeg(1f), 0.001f)
        val mid = stylusAngleDeg(0.5f)
        assertTrue(mid < 10f && mid > -18f)
    }

    @Test
    fun stylusRadiusShrinksToLabel() {
        assertEquals(98f, stylusRadiusPx(0f, 100f, 32f), 0.001f)
        assertEquals(38.4f, stylusRadiusPx(1f, 100f, 32f), 0.001f)
    }

    @Test
    fun stylusStaysOnRecordSurface() {
        val center = Offset(200f, 300f)
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
            val point = stylusPoint(fraction, center, 100f, 32f)
            val distance = hypot(point.x - center.x, point.y - center.y)
            assertTrue(distance in 38.0f..98.5f)
        }
    }

    @Test
    fun mockEnergyIsBoundedAndCalmWhenPaused() {
        assertEquals(0.15f, mockEnergy(12345L, false), 0.001f)
        listOf(0L, 500L, 10_000L, 200_000L).forEach { position ->
            val energy = mockEnergy(position, true)
            assertTrue(energy in 0.1f..0.9f)
        }
    }
}
