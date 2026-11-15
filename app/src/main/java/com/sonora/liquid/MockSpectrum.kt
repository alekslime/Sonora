package com.sonora.liquid

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
}
