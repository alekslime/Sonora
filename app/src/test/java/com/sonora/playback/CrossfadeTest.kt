package com.sonora.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeTest {

    @Test
    fun endpointsAreCleanHandoffs() {
        val (out0, in0) = Crossfade.fadeGains(0f)
        assertEquals(1f, out0, 0.001f)
        assertEquals(0f, in0, 0.001f)
        val (out1, in1) = Crossfade.fadeGains(1f)
        assertEquals(0f, out1, 0.001f)
        assertEquals(1f, in1, 0.001f)
    }

    @Test
    fun midpointIsEqualPower() {
        val (out, incoming) = Crossfade.fadeGains(0.5f)
        assertEquals(0.707f, out, 0.001f)
        assertEquals(0.707f, incoming, 0.001f)
        // Equal power: squares sum to unity (no mid-fade dip).
        assertEquals(1f, out * out + incoming * incoming, 0.001f)
    }

    @Test
    fun progressClamps() {
        assertEquals(Crossfade.fadeGains(0f), Crossfade.fadeGains(-2f))
        assertEquals(Crossfade.fadeGains(1f), Crossfade.fadeGains(9f))
    }

    @Test
    fun rampsAreMonotonic() {
        var lastOut = 2f
        var lastIn = -1f
        for (step in 0..20) {
            val (out, incoming) = Crossfade.fadeGains(step / 20f)
            assertTrue(out <= lastOut + 0.001f)
            assertTrue(incoming >= lastIn - 0.001f)
            lastOut = out
            lastIn = incoming
        }
    }
}
