package com.sonora.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSpectrumTest {

    private fun assertBounded(bands: Bands) {
        listOf(bands.bass, bands.mid, bands.treble, bands.pulse).forEach {
            assertTrue(it in 0f..1f)
        }
    }

    @Test
    fun pausedYieldsAmbient() {
        assertEquals(Bands.Ambient, MockSpectrum.sample(123_456L, false))
    }

    @Test
    fun levelsStayBounded() {
        var time = 0L
        while (time <= 300_000L) {
            assertBounded(MockSpectrum.sample(time, true))
            time += 977L
        }
    }

    @Test
    fun samplingIsDeterministic() {
        assertEquals(MockSpectrum.sample(42_000L, true), MockSpectrum.sample(42_000L, true))
    }

    @Test
    fun energyVariesOverTime() {
        val early = MockSpectrum.sample(1_000L, true)
        val later = MockSpectrum.sample(30_000L, true)
        assertTrue(
            early.bass != later.bass || early.mid != later.mid || early.treble != later.treble,
        )
    }

    @Test
    fun signalFramesAreSizedAndBounded() {
        val signal = MockSpectrum.signal(42_000L, true)
        assertEquals(WAVEFORM_POINTS, signal.frame.waveform.size)
        assertEquals(SPECTRUM_BINS, signal.frame.spectrum.size)
        assertTrue(signal.frame.waveform.all { it in -1f..1f })
        assertTrue(signal.frame.spectrum.all { it in 0f..1f })
        assertEquals(signal.bands, MockSpectrum.sample(42_000L, true))
    }

    @Test
    fun signalIsDeterministicAndAlive() {
        val a = MockSpectrum.signal(42_000L, true)
        val b = MockSpectrum.signal(42_000L, true)
        assertTrue(a.frame.waveform.contentEquals(b.frame.waveform))
        assertTrue(a.frame.spectrum.contentEquals(b.frame.spectrum))
        assertTrue(a.frame.waveform.any { it != 0f })
        assertTrue(a.frame.spectrum.any { it > 0.1f })
    }

    @Test
    fun pausedSignalIsFlat() {
        val signal = MockSpectrum.signal(42_000L, false)
        assertEquals(Bands.Ambient, signal.bands)
        assertTrue(signal.frame.waveform.all { it == 0f })
        assertTrue(signal.frame.spectrum.all { it == 0f })
    }

    @Test
    fun pulsesOccur() {
        var time = 0L
        var peaked = false
        while (time <= 120_000L) {
            if (MockSpectrum.sample(time, true).pulse > 0.5f) peaked = true
            time += 250L
        }
        assertTrue(peaked)
        assertNotEquals(0f, MockSpectrum.sample(0L, true).bass)
    }
}
