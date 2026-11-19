package com.sonora.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VizEngineTest {

    @Test
    fun smoothingAttacksFasterThanItReleases() {
        val attack = smoothBand(0f, 1f, 1f / 60f)
        val release = smoothBand(1f, 0f, 1f / 60f)
        assertTrue(attack > 1f - release)
        assertTrue(attack in 0f..1f)
    }

    @Test
    fun smoothingSnapsWhenCaughtUp() {
        assertEquals(1f, smoothBand(0.99f, 1f, 1f), 0.05f)
        assertEquals(0.5f, smoothBand(0.5f, 0.5f, 1f / 60f), 0.0001f)
        assertEquals(0.3f, smoothBand(0.3f, 0.9f, 0f), 0.0001f)
    }

    @Test
    fun smoothingFollowsEveryChannel() {
        val from = Bands.Ambient
        val to = Bands(1f, 1f, 1f, 1f)
        val next = smoothBands(from, to, 1f / 60f)
        assertTrue(next.bass > from.bass)
        assertTrue(next.mid > from.mid)
        assertTrue(next.treble > from.treble)
        assertTrue(next.pulse > from.pulse)
    }

    @Test
    fun showStepsThroughEveryModeAndWraps() {
        val show = VizShow(VizMode.MANDELBROT, autoCycle = false)
        val seen = mutableListOf(show.current)
        repeat(VizMode.entries.size) {
            show.next()
            // Settle the fade so each step starts clean.
            show.frame(VIZ_FADE_MS)
            seen += show.current
        }
        assertEquals(
            listOf(
                VizMode.MANDELBROT, VizMode.RIBBONS, VizMode.POLAR,
                VizMode.STAR, VizMode.MANDELBROT,
            ),
            seen,
        )
        show.prev()
        assertEquals(VizMode.STAR, show.current)
    }

    @Test
    fun stepOpensCrossfadeThenSettles() {
        val show = VizShow(VizMode.MANDELBROT, autoCycle = false)
        show.next()
        assertEquals(VizMode.MANDELBROT, show.outgoing)
        assertEquals(0f, show.fade, 0.0001f)
        show.frame(VIZ_FADE_MS / 2)
        assertEquals(0.5f, show.fade, 0.01f)
        show.frame(VIZ_FADE_MS / 2)
        assertEquals(1f, show.fade, 0.0001f)
        assertNull(show.outgoing)
    }

    @Test
    fun autoCycleAdvancesOnlyWhenEnabled() {
        val on = VizShow(VizMode.MANDELBROT, autoCycle = true)
        on.frame(VIZ_CYCLE_MS - 1)
        assertEquals(VizMode.MANDELBROT, on.current)
        on.frame(1)
        assertEquals(VizMode.RIBBONS, on.current)

        val off = VizShow(VizMode.MANDELBROT, autoCycle = false)
        off.frame(VIZ_CYCLE_MS * 5)
        assertEquals(VizMode.MANDELBROT, off.current)
        assertNull(off.outgoing)
    }

    @Test
    fun lineAltLiftsBodyTowardWhite() {
        LiquidPreset.entries.forEach { preset ->
            val alt = vizLineAlt(preset)
            val body = preset.body
            assertTrue(alt.red >= body.red)
            assertTrue(alt.green >= body.green)
            assertTrue(alt.blue >= body.blue)
            assertTrue(alt.red in 0f..1f)
            assertTrue(alt.green in 0f..1f)
            assertTrue(alt.blue in 0f..1f)
        }
    }

    @Test
    fun manualStepRestartsCycleClock() {
        val show = VizShow(VizMode.MANDELBROT, autoCycle = true)
        show.frame(VIZ_CYCLE_MS - 100)
        show.next()
        show.frame(VIZ_FADE_MS)
        assertEquals(VizMode.RIBBONS, show.current)
        // Nearly a full cycle later it still holds: the clock restarted.
        show.frame(VIZ_CYCLE_MS - 100)
        assertEquals(VizMode.RIBBONS, show.current)
        show.frame(100)
        assertEquals(VizMode.POLAR, show.current)
    }
}
