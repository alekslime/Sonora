package com.sonora.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumAnalyzerTest {

    @Test
    fun tinyInputReturnsAmbient() {
        val frame = SpectrumAnalyzer.fftToBands(ByteArray(4), 1.5f, 2f)
        assertEquals(Bands.Ambient, frame.bands)
        assertEquals(1.5f, frame.bassEma, 0.001f)
        assertEquals(2f, frame.peak, 0.001f)
    }

    @Test
    fun silenceGivesZeroBands() {
        val frame = SpectrumAnalyzer.fftToBands(ByteArray(64), 0f, 0f)
        assertEquals(0f, frame.bands.bass, 0.001f)
        assertEquals(0f, frame.bands.mid, 0.001f)
        assertEquals(0f, frame.bands.treble, 0.001f)
        assertEquals(0f, frame.bands.pulse, 0.001f)
        assertEquals(0f, frame.peak, 0.001f)
    }

    @Test
    fun bassHeavyInputDominatesBass() {
        // 32 bins; only bin 1 carries energy. Instant attack normalizes
        // the first loud frame to full scale.
        val fft = ByteArray(64)
        fft[2] = 100
        val frame = SpectrumAnalyzer.fftToBands(fft, 0f, 0f)
        assertEquals(1f, frame.bands.bass, 0.001f)
        assertEquals(0f, frame.bands.mid, 0.001f)
        assertEquals(0f, frame.bands.treble, 0.001f)
        assertEquals(3.125f, frame.peak, 0.001f)
    }

    @Test
    fun quietPassageReadsAgainstTheFloor() {
        val fft = ByteArray(64)
        fft[2] = 10
        val frame = SpectrumAnalyzer.fftToBands(fft, 0f, 0f)
        // Raw mean 0.3125 against the 0.5 floor: still visibly alive.
        assertEquals(0.791f, frame.bands.bass, 0.01f)
    }

    @Test
    fun establishedPeakReadsRelativeLoudness() {
        val fft = ByteArray(64)
        fft[2] = 10
        val frame = SpectrumAnalyzer.fftToBands(fft, 0f, 3.125f)
        // Same quiet frame against a loud running peak reads low.
        assertEquals(0.32f, frame.bands.bass, 0.01f)
    }

    @Test
    fun peakDecaysThroughSilence() {
        val loud = ByteArray(64)
        loud[2] = 100
        val first = SpectrumAnalyzer.fftToBands(loud, 0f, 0f)
        val second = SpectrumAnalyzer.fftToBands(ByteArray(64), first.bassEma, first.peak)
        assertEquals(3.125f * 0.98f, second.peak, 0.001f)
        assertEquals(0f, second.bands.bass, 0.001f)
    }

    @Test
    fun onsetFiresPulseThenDecays() {
        val fft = ByteArray(64)
        fft[2] = 100
        val first = SpectrumAnalyzer.fftToBands(fft, 0f, 0f)
        assertEquals(1f, first.bands.pulse, 0.001f)
        assertTrue(first.bassEma > 0f)
        // Same level against the adapted average: no onset.
        val second = SpectrumAnalyzer.fftToBands(fft, first.bassEma * 5f, first.peak)
        assertEquals(0f, second.bands.pulse, 0.001f)
    }

    @Test
    fun bandsStayInRange() {
        val fft = ByteArray(64) { 127 }
        val frame = SpectrumAnalyzer.fftToBands(fft, 0f, 0f)
        assertTrue(frame.bands.bass in 0f..1f)
        assertTrue(frame.bands.mid in 0f..1f)
        assertTrue(frame.bands.treble in 0f..1f)
        assertTrue(frame.bands.pulse in 0f..1f)
    }

    @Test
    fun trebleEnergyStaysOutOfBass() {
        // Energy in the top bins only.
        val fft = ByteArray(64)
        fft[60] = 100
        fft[62] = 100
        val frame = SpectrumAnalyzer.fftToBands(fft, 0f, 0f)
        assertEquals(0f, frame.bands.bass, 0.001f)
        assertEquals(0f, frame.bands.mid, 0.001f)
        assertTrue(frame.bands.treble > 0f)
    }

    @Test
    fun spectrumMapsEnergyToItsBin() {
        // 32 FFT bins; bin 2 carries all the energy. Quadratic spacing
        // puts it in output bin 2, peaking alone at full scale.
        val fft = ByteArray(64)
        fft[4] = 100
        val out = SpectrumAnalyzer.fftToSpectrum(fft, 8)
        assertEquals(8, out.size)
        assertEquals(1f, out.maxOrNull()!!, 0.001f)
        assertEquals(1f, out[2], 0.001f)
        assertEquals(0f, out[0], 0.001f)
        assertEquals(0f, out[1], 0.001f)
        assertEquals(0f, out[7], 0.001f)
    }

    @Test
    fun spectrumSilenceStaysSilent() {
        val out = SpectrumAnalyzer.fftToSpectrum(ByteArray(64), 8)
        assertTrue(out.all { it == 0f })
    }

    @Test
    fun spectrumDegradesOnTinyInput() {
        assertEquals(4, SpectrumAnalyzer.fftToSpectrum(ByteArray(4), 4).size)
        assertTrue(SpectrumAnalyzer.fftToSpectrum(ByteArray(4), 4).all { it == 0f })
        assertEquals(0, SpectrumAnalyzer.fftToSpectrum(ByteArray(64), 0).size)
    }

    @Test
    fun spectrumStaysBounded() {
        val out = SpectrumAnalyzer.fftToSpectrum(ByteArray(1024) { 127 }, 64)
        assertEquals(64, out.size)
        assertTrue(out.all { it in 0f..1f })
    }

    @Test
    fun waveformCentersAndScales() {
        // Silence, full negative, full positive.
        val bytes = byteArrayOf(128.toByte(), 0.toByte(), 255.toByte())
        val out = SpectrumAnalyzer.waveformToFloat(bytes, 3)
        assertEquals(0f, out[0], 0.001f)
        assertEquals(-1f, out[1], 0.001f)
        assertEquals(0.992f, out[2], 0.01f)
    }

    @Test
    fun waveformResamplesBothDirections() {
        val up = SpectrumAnalyzer.waveformToFloat(byteArrayOf(0.toByte(), 255.toByte()), 5)
        assertEquals(5, up.size)
        assertEquals(-1f, up.first(), 0.001f)
        assertEquals(0.992f, up.last(), 0.01f)
        assertTrue(up[2] in -0.1f..0.1f)
        val down = SpectrumAnalyzer.waveformToFloat(ByteArray(100) { 128.toByte() }, 4)
        assertEquals(4, down.size)
        assertTrue(down.all { it == 0f })
    }

    @Test
    fun waveformDegradesOnEmpty() {
        // Empty input yields flat zeros of the requested size, matching
        // the spectrum contract, so renderers always draw a flat line.
        assertEquals(8, SpectrumAnalyzer.waveformToFloat(ByteArray(0), 8).size)
        assertTrue(SpectrumAnalyzer.waveformToFloat(ByteArray(0), 8).all { it == 0f })
        assertEquals(0, SpectrumAnalyzer.waveformToFloat(ByteArray(8), 0).size)
        assertEquals(1, SpectrumAnalyzer.waveformToFloat(byteArrayOf(0.toByte()), 1).size)
    }
}
