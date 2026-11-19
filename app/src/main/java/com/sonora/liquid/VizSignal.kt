package com.sonora.liquid

/** Waveform samples per visualizer frame. */
const val WAVEFORM_POINTS = 128

/** Spectrum magnitudes per visualizer frame. */
const val SPECTRUM_BINS = 64

/**
 * One full-rate visualizer frame: the raw waveform (-1..1) and the
 * normalized spectrum (0..1) plus the capture clock. FloatArray
 * equality is referential, so every capture is a distinct emission;
 * tests compare contents.
 */
data class VizFrame(
    val waveform: FloatArray,
    val spectrum: FloatArray,
    /** [android.os.SystemClock.elapsedRealtime] at capture. */
    val atMs: Long,
) {
    companion object {
        fun flat(atMs: Long = 0L) = VizFrame(
            FloatArray(WAVEFORM_POINTS),
            FloatArray(SPECTRUM_BINS),
            atMs,
        )
    }
}

/** Coarse bands plus the full frame renderers draw from. */
data class VizSignal(val bands: Bands, val frame: VizFrame)
