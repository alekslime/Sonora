package com.sonora.liquid

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Normalized frequency-band levels plus a transient pulse. All 0..1. */
data class Bands(
    val bass: Float,
    val mid: Float,
    val treble: Float,
    val pulse: Float,
) {
    companion object {
        val Ambient = Bands(0.08f, 0.06f, 0.05f, 0f)
    }
}

/**
 * M3 mock spectrum. Deterministic composite waves shaped like music
 * energy (bass swells, mid ripples, treble shimmer, kick-like pulses)
 * so the visualizer behaves like the real analyzer will in M5, when this
 * object is replaced by FFT input without touching the renderers.
 */
object MockSpectrum {
    fun sample(positionMs: Long, playing: Boolean): Bands {
        if (!playing) return Bands.Ambient
        val t = positionMs / 1000.0
        val bass = 0.45 + 0.30 * sin(t * 2.1) + 0.15 * sin(t * 5.7 + 1.3)
        val mid = 0.40 + 0.25 * sin(t * 3.3 + 0.5) + 0.15 * sin(t * 8.2)
        val treble = 0.35 + 0.20 * sin(t * 5.1 + 2.1) + 0.12 * sin(t * 13.7)
        val beat = sin(t * 4.4).coerceAtLeast(0.0).pow(8.0)
        return Bands(
            bass = bass.toFloat().coerceIn(0f, 1f),
            mid = mid.toFloat().coerceIn(0f, 1f),
            treble = treble.toFloat().coerceIn(0f, 1f),
            pulse = beat.toFloat().coerceIn(0f, 1f),
        )
    }

    /**
     * Full synthetic signal: deterministic layered-sine waveform and
     * drifting spectral bumps shaped by the same band levels, so demo
     * and fallback playback drive the renderers like the analyzer.
     */
    fun signal(positionMs: Long, playing: Boolean): VizSignal {
        val bands = sample(positionMs, playing)
        if (!playing) return VizSignal(bands, VizFrame.flat())
        val t = positionMs / 1000.0
        val wave = FloatArray(WAVEFORM_POINTS) { i ->
            val x = i.toFloat() / (WAVEFORM_POINTS - 1) * 2f * PI.toFloat()
            (0.55 * sin(x * 3 + t * 9.0) * bands.bass +
                0.30 * sin(x * 7 + t * 17.0 + 1.0) * bands.mid +
                0.15 * sin(x * 15 + t * 29.0) * bands.treble).toFloat().coerceIn(-1f, 1f)
        }
        val spec = FloatArray(SPECTRUM_BINS) { b ->
            val f = b.toDouble() / (SPECTRUM_BINS - 1)
            (bump(f, 0.12 + 0.05 * sin(t * 0.7), 0.06, bands.bass.toDouble()) +
                bump(f, 0.38 + 0.06 * sin(t * 1.1 + 2.0), 0.08, bands.mid.toDouble()) +
                bump(f, 0.72 + 0.05 * sin(t * 1.7 + 4.0), 0.10, bands.treble * 0.9) +
                0.06f * bands.pulse).toFloat().coerceIn(0f, 1f)
        }
        return VizSignal(bands, VizFrame(wave, spec, 0L))
    }

    private fun bump(f: Double, center: Double, width: Double, amp: Double): Double {
        val d = (f - center) / width
        return amp * exp(-d * d / 2.0)
    }
}
