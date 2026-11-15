package com.sonora.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumAnalyzerTest {

    @Test
    fun tinyInputReturnsAmbient() {
        val (bands, ema) = SpectrumAnalyzer.fftToBands(ByteArray(4), 1.5f)
        assertEquals(Bands.Ambient, bands)
        assertEquals(1.5f, ema, 0.001f)
    }

    @Test
    fun silenceGivesZeroBands() {
        val (bands, ema) = SpectrumAnalyzer.fftToBands(ByteArray(64), 0f)
        assertEquals(0f, bands.bass, 0.001f)
        assertEquals(0f, bands.mid, 0.001f)
        assertEquals(0f, bands.treble, 0.001f)
        assertEquals(0f, bands.pulse, 0.001f)
        assertEquals(0f, ema, 0.001f)
    }

    @Test
    fun bassHeavyInputDominatesBass() {
        // 32 bins; only bin 1 carries energy.
        val fft = ByteArray(64)
        fft[2] = 100
        val (bands, _) = SpectrumAnalyzer.fftToBands(fft, 0f)
        assertEquals(0.625f, bands.bass, 0.02f)
        assertEquals(0f, bands.mid, 0.001f)
        assertEquals(0f, bands.treble, 0.001f)
        assertTrue(bands.bass in 0f..1f)
    }

    @Test
    fun onsetFiresPulseThenDecays() {
        val fft = ByteArray(64)
        fft[2] = 100
        val (first, ema) = SpectrumAnalyzer.fftToBands(fft, 0f)
        assertEquals(1f, first.pulse, 0.001f)
        assertTrue(ema > 0f)
        // Same level against the adapted average: no onset.
        val (second, _) = SpectrumAnalyzer.fftToBands(fft, ema * 5f)
        assertEquals(0f, second.pulse, 0.001f)
    }

    @Test
    fun bandsStayInRange() {
        val fft = ByteArray(64) { 127 }
        val (bands, _) = SpectrumAnalyzer.fftToBands(fft, 0f)
        assertTrue(bands.bass in 0f..1f)
        assertTrue(bands.mid in 0f..1f)
        assertTrue(bands.treble in 0f..1f)
        assertTrue(bands.pulse in 0f..1f)
    }

    @Test
    fun trebleEnergyStaysOutOfBass() {
        // Energy in the top bins only.
        val fft = ByteArray(64)
        fft[60] = 100
        fft[62] = 100
        val (bands, _) = SpectrumAnalyzer.fftToBands(fft, 0f)
        assertEquals(0f, bands.bass, 0.001f)
        assertEquals(0f, bands.mid, 0.001f)
        assertTrue(bands.treble > 0f)
    }
}
